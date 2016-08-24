package com.geirsson.codegen

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

import com.geirsson.codegen.codegen.CodegenOptions
import com.geirsson.codegen.codegen.SnakeCaseReverse
import io.getquill.NamingStrategy
import org.scalafmt.FormatResult
import org.scalafmt.Scalafmt
import org.scalafmt.ScalafmtStyle

case class Codegen(options: CodegenOptions, namingStrategy: NamingStrategy) {

  import Codegen._
  import options.typeMap.columnType2scalaType

  def getForeignKeys(db: Connection): Set[ForeignKey] = {
    val sb = Set.newBuilder[ForeignKey]
    val foreignKeys = db.getMetaData.getExportedKeys(null, "public", null)
    while (foreignKeys.next()) {
      sb += ForeignKey(
        from = SimpleColumn(
          tableName = foreignKeys.getString("fktable_name"),
          columnName = foreignKeys.getString("fkcolumn_name")
        ),
        to = SimpleColumn(
          tableName = foreignKeys.getString("pktable_name"),
          columnName = foreignKeys.getString("pkcolumn_name")
        )
      )
    }
    sb.result()
  }

  def getTables(db: Connection, foreignKeys: Set[ForeignKey]): Seq[Table] = {
    val sb = Seq.newBuilder[Table]
    val rs: ResultSet =
      db.getMetaData.getTables(null, "public", "%", Array("TABLE"))
    while (rs.next()) {
      if (!excludedTables.contains(rs.getString(tableName))) {
        val name = rs.getString(tableName)
        sb += Table(
          name,
          getColumns(db, name, foreignKeys)
        )
      }
    }
    sb.result()
  }

  def getColumns(db: Connection,
                 tableName: String,
                 foreignKeys: Set[ForeignKey]): Seq[Column] = {
    val primaryKeys = getPrimaryKeys(db, tableName)
    val sb = Seq.newBuilder[Column]
    val cols =
      db.getMetaData.getColumns(null, null, tableName, null)
    while (cols.next()) {
      val colName = cols.getString(columnName)
      val simpleColumn = SimpleColumn(tableName, colName)
      val ref = foreignKeys.find(_.from == simpleColumn).map(_.to)
      sb += Column(
        tableName,
        colName,
        cols.getString(typeName),
        cols.getBoolean(nullable),
        primaryKeys contains cols.getString(columnName),
        ref
      )
    }
    sb.result()
  }

  def getPrimaryKeys(db: Connection, tableName: String): Set[String] = {
    val sb = Set.newBuilder[String]
    val primaryKeys = db.getMetaData.getPrimaryKeys(null, null, tableName)
    while (primaryKeys.next()) {
      sb += primaryKeys.getString(columnName)
    }
    sb.result()
  }

  def tables2code(tables: Seq[Table],
                  namingStrategy: NamingStrategy,
                  options: CodegenOptions) = {
    val body = tables.map(_.toCode).mkString("\n\n")
    s"""|package ${options.packageName}
        |${options.imports}
        |
        |object Tables {
        |$body
        |}
     """.stripMargin
  }

  case class ForeignKey(from: SimpleColumn, to: SimpleColumn)

  case class SimpleColumn(tableName: String, columnName: String) {
    def toType =
      s"${namingStrategy.table(tableName)}.${namingStrategy.table(columnName)}"
  }

  case class Column(tableName: String,
                    columnName: String,
                    `type`: String,
                    nullable: Boolean,
                    isPrimaryKey: Boolean,
                    references: Option[SimpleColumn]) {
    val scalaType = columnType2scalaType(`type`)

    def toArg(namingStrategy: NamingStrategy, tableName: String): String = {
      s"${namingStrategy.column(columnName)}: ${this.toSimple.toType}"
    }

    def toSimple = references.getOrElse(SimpleColumn(tableName, columnName))

    def toClass: String = {
      s"case class ${namingStrategy.table(columnName)}(value: $scalaType) extends AnyVal with WrappedValue[$scalaType]"
    }
  }

  case class Table(name: String, columns: Seq[Column]) {
    def toCode: String = {
      val scalaName = namingStrategy.table(name)
      val args = columns.map(_.toArg(namingStrategy, scalaName)).mkString(", ")
      val applyArgs = columns.map { column =>
        s"${namingStrategy.column(column.columnName)}: ${column.scalaType}"
      }.mkString(", ")
      val applyArgNames = columns.map { column =>
        if (column.references.nonEmpty) {
          s"${column.toSimple.toType}(${namingStrategy.column(column.columnName)})"
        } else {
          s"${namingStrategy.table(column.columnName)}(${namingStrategy.column(column.columnName)})"
        }
      }.mkString(", ")
      val classes =
        columns.withFilter(_.references.isEmpty).map(_.toClass).mkString("\n")

      s"""|  /////////////////////////////////////////////////////
          |  // $scalaName
          |  /////////////////////////////////////////////////////
          |case class $scalaName($args)
          |object $scalaName {
          |  def create($applyArgs): $scalaName = {
          |    $scalaName($applyArgNames)
          |  }
          |$classes
          |}""".stripMargin
    }
  }

}

object Codegen {
  val tableName = "TABLE_NAME"
  val columnName = "COLUMN_NAME"
  val typeName = "TYPE_NAME"
  val nullable = "NULLABLE"
  val primaryKeyName = "pk_name"
  val excludedTables = Set("schema_version")

  def debugPrintColumnLabels(rs: ResultSet): Unit = {
    (1 to rs.getMetaData.getColumnCount).foreach { i =>
      println(i -> rs.getMetaData.getColumnLabel(i))
    }
  }

  import caseapp._

  def main(args: Array[String]): Unit = {
    CaseApp.parse[CodegenOptions](args) match {
      case Right((options, _)) => run(options)
      case Left(msg) => System.err.println(msg)
    }
  }

  def run(codegenOptions: CodegenOptions) = {
    println("Starting...")
    val startTime = System.currentTimeMillis()
    Class.forName(codegenOptions.jdbcDriver)
    val db: Connection =
      DriverManager.getConnection(codegenOptions.url,
                                  codegenOptions.username,
                                  codegenOptions.password)
    val codegen = Codegen(codegenOptions, SnakeCaseReverse)
    val foreignKeys = codegen.getForeignKeys(db)
    val tables = codegen.getTables(db, foreignKeys)
    val generatedCode =
      codegen.tables2code(tables, SnakeCaseReverse, codegenOptions)
    val codeStyle = ScalafmtStyle.defaultWithAlign.copy(maxColumn = 120)
    val code = Scalafmt.format(generatedCode, style = codeStyle) match {
      case FormatResult.Success(x) => x
      case _ => generatedCode
    }
    codegenOptions.file match {
      case Some(uri) =>
        Files.write(Paths.get(new File(uri).toURI), code.getBytes)
        println(
          s"Done! Wrote to $uri (${System.currentTimeMillis() - startTime}ms)")
      case _ =>
        println(code)
    }
    db.close()
  }
}
