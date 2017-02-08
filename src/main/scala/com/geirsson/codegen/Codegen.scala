package com.geirsson.codegen

import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

import caseapp.AppOf
import caseapp._
import com.typesafe.scalalogging.Logger
import io.getquill.NamingStrategy
import org.scalafmt.FormatResult
import org.scalafmt.Scalafmt
import org.scalafmt.ScalafmtStyle

case class Error(msg: String) extends Exception(msg)

@AppName("db-codegen")
@AppVersion(Versions.nightly)
@ProgName("db-codegen")
case class CodegenOptions(
    @HelpMessage("user on database server") user: String = "postgres",
    @HelpMessage("password for user on database server") password: String = "postgres",
    @HelpMessage("jdbc url") url: String = "jdbc:postgresql:postgres",
    @HelpMessage("schema on database") schema: String = "public",
    @HelpMessage("only tested with postgresql") jdbcDriver: String = "org.postgresql.Driver",
    @HelpMessage(
      "top level imports of generated file"
    ) imports: String = """""",
    @HelpMessage(
      "package name for generated classes"
    ) `package`: String = "tables",
    @HelpMessage(
      "Which types should write to which types? Format is: numeric,BigDecimal;int8,Long;..."
    ) typeMap: TypeMap = TypeMap.default,
    @HelpMessage(
      "Do not generate classes for these tables."
    ) excludedTables: List[String] = List("schema_version"),
    @HelpMessage(
      "Write generated code to this filename. Prints to stdout if not set."
    ) file: Option[String] = None
) extends App {
  Codegen.cliRun(this)
}

case class Codegen(options: CodegenOptions, namingStrategy: NamingStrategy) {
  import Codegen._
  val excludedTables = options.excludedTables.toSet
  val columnType2scalaType = options.typeMap.pairs.toMap

  def results(resultSet: ResultSet): Iterator[ResultSet] = {
    new Iterator[ResultSet] {
      def hasNext = resultSet.next()
      def next() = resultSet
    }
  }

  def getForeignKeys(db: Connection): Set[ForeignKey] = {
    val foreignKeys =
      db.getMetaData.getExportedKeys(null, options.schema, null)
    results(foreignKeys).map { row =>
      ForeignKey(
        from = SimpleColumn(
          tableName = row.getString(FK_TABLE_NAME),
          columnName = row.getString(FK_COLUMN_NAME)
        ),
        to = SimpleColumn(
          tableName = row.getString(PK_TABLE_NAME),
          columnName = row.getString(PK_COLUMN_NAME)
        )
      )
    }.toSet
  }

  def warn(msg: String): Unit = {
    System.err.println(s"[${Console.YELLOW}warn${Console.RESET}] $msg")
  }

  def getTables(db: Connection, foreignKeys: Set[ForeignKey]): Seq[Table] = {
    val rs: ResultSet =
      db.getMetaData.getTables(null, options.schema, "%", Array("TABLE"))
    results(rs).flatMap { row =>
      val name = row.getString(TABLE_NAME)
      if (!excludedTables.contains(name)) {
        val columns = getColumns(db, name, foreignKeys)
        val mappedColumns = columns.filter(_.isRight).map(_.right.get)
        val unmappedColumns = columns.filter(_.isLeft).map(_.left.get)
        if (unmappedColumns.nonEmpty)
          warn(s"The following columns from table $name need a mapping: $unmappedColumns")
        Some(Table(
          name,
          mappedColumns
        ))
      } else {
        None
      }
    }.toVector
  }
  def getColumns(db: Connection,
                 tableName: String,
                 foreignKeys: Set[ForeignKey]): Seq[Either[String, Column]] = {
    val primaryKeys = getPrimaryKeys(db, tableName)
    val cols =
      db.getMetaData.getColumns(null, options.schema, tableName, null)
    results(cols).map { row =>
      val colName = cols.getString(COLUMN_NAME)
      val simpleColumn = SimpleColumn(tableName, colName)
      val ref = foreignKeys.find(_.from == simpleColumn).map(_.to)

      val typ = cols.getString(TYPE_NAME)
      columnType2scalaType.get(typ).map { scalaType =>
        Right(Column(
          tableName,
          colName,
          scalaType,
          cols.getBoolean(NULLABLE),
          primaryKeys contains cols.getString(COLUMN_NAME),
          ref
        ))
      }.getOrElse(Left(typ))
    }.toVector
  }

  def getPrimaryKeys(db: Connection, tableName: String): Set[String] = {
    val sb = Set.newBuilder[String]
    val primaryKeys = db.getMetaData.getPrimaryKeys(null, null, tableName)
    while (primaryKeys.next()) {
      sb += primaryKeys.getString(COLUMN_NAME)
    }
    sb.result()
  }

  def tables2code(tables: Seq[Table],
                  namingStrategy: NamingStrategy,
                  options: CodegenOptions) = {
    val body = tables.map(_.toCode).mkString("\n\n")
    s"""|package ${options.`package`}
        |${options.imports}
        |
        |/**
        | * Generated using [[https://github.com/olafurpg/scala-db-codegen scala-db-codegen]]
        | *  - Number of tables: ${tables.size}
        | *  - Database URL: ${options.url}
        | *  - Database schema: ${options.schema}
        | *  - Database user: ${options.user}
        | */
        |//noinspection ScalaStyle
        |object Tables {
        |
        |  /**
        |    * Quill used to have this trait before v1, but it's still useful to keep.
        |    * Examples are: pattern matching on wrapped type and conversion to JSON objects.
        |    */
        |  trait WrappedValue[T] extends Any with WrappedType { self: AnyVal =>
        |    type Type = T
        |    def value: T
        |    override def toString = s"$$value"
        |  }
        |
        |  trait WrappedType extends Any {
        |    type Type
        |    def value: Type
        |  }
        |
        |$body
        |}
     """.stripMargin
  }

  case class ForeignKey(from: SimpleColumn, to: SimpleColumn)

  val logger = Logger(this.getClass)

  case class SimpleColumn(tableName: String, columnName: String) {
    def toType =
      s"${namingStrategy.table(tableName)}.${namingStrategy.table(columnName)}"
  }

  case class Column(tableName: String,
                    columnName: String,
                    scalaType: String,
                    nullable: Boolean,
                    isPrimaryKey: Boolean,
                    references: Option[SimpleColumn]) {
    def scalaOptionType = makeOption(scalaType)

    def makeOption(typ: String): String = {
      if (nullable) s"Option[$typ]"
      else typ
    }

    def toType: String = this.toSimple.toType

    def toArg(namingStrategy: NamingStrategy, tableName: String): String = {
      s"${namingStrategy.column(columnName)}: ${makeOption(this.toType)}"
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
        s"${namingStrategy.column(column.columnName)}: ${column.scalaOptionType}"
      }.mkString(", ")
      val applyArgNames = columns.map { column =>
        val typName = if (column.references.nonEmpty) {
          column.toType
        } else {
          namingStrategy.table(column.columnName)
        }
        if (column.nullable) {
          s"${namingStrategy.column(column.columnName)}.map($typName.apply)"
        } else {
          s"$typName(${namingStrategy.column(column.columnName)})"
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

object Codegen extends AppOf[CodegenOptions] {
  val TABLE_NAME = "TABLE_NAME"
  val COLUMN_NAME = "COLUMN_NAME"
  val TYPE_NAME = "TYPE_NAME"
  val NULLABLE = "NULLABLE"
  val PK_NAME = "pk_name"
  val FK_TABLE_NAME = "fktable_name"
  val FK_COLUMN_NAME = "fkcolumn_name"
  val PK_TABLE_NAME = "pktable_name"
  val PK_COLUMN_NAME = "pkcolumn_name"

  def debugPrintColumnLabels(rs: ResultSet): Unit = {
    (1 to rs.getMetaData.getColumnCount).foreach { i =>
      println(i -> rs.getMetaData.getColumnLabel(i))
    }
  }

  def cliRun(codegenOptions: CodegenOptions,
             outstream: PrintStream = System.out): Unit = {
    try {
      run(codegenOptions, outstream)
    } catch {
      case Error(msg) =>
        System.err.println(msg)
        System.exit(1)
    }
  }

  def run(codegenOptions: CodegenOptions,
          outstream: PrintStream = System.out): Unit = {
    codegenOptions.file.foreach { x =>
      outstream.println("Starting...")
    }

    val startTime = System.currentTimeMillis()
    Class.forName(codegenOptions.jdbcDriver)
    val db: Connection =
      DriverManager.getConnection(codegenOptions.url,
                                  codegenOptions.user,
                                  codegenOptions.password)
    val codegen = Codegen(codegenOptions, SnakeCaseReverse)
    val plainForeignKeys = codegen.getForeignKeys(db)

    val foreignKeys = plainForeignKeys.map { fk =>

      def resolve(col: codegen.SimpleColumn): codegen.SimpleColumn = {
        plainForeignKeys.find(f => f.from == col).map(_.to).getOrElse(col)
      }

      codegen.ForeignKey(fk.from, resolve(fk.to))
    }

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
        outstream.println(code)
    }
    db.close()
  }
}
