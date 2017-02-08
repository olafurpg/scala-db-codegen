package com.geirsson.codegen

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.DriverManager

import caseapp.CaseApp
import org.scalatest.FunSuite

class CodegenTest extends FunSuite {

  def structure(code: String): String = {
    import scala.meta._
    code.parse[Source].get.structure
  }

  test("--type-map") {
    val obtained =
      CaseApp.parse[CodegenOptions](
        Seq("--type-map", "numeric,BigDecimal;int8,Long"))
    val expected = Right(
      (CodegenOptions(
         typeMap = TypeMap("numeric" -> "BigDecimal", "int8" -> "Long")),
       Seq.empty[String]))
    assert(obtained === expected)
  }

  test("testMain") {
    val options = CodegenOptions(
      schema = s"scala_db_codegen",
      `package` = "com.geirsson.codegen"
    )
    Class.forName(options.jdbcDriver)
    val conn =
      DriverManager
        .getConnection(options.url, options.user, options.password)
    val stmt = conn.createStatement()

    val sql =
      s"""|drop schema if exists ${options.schema} cascade;
          |create schema ${options.schema};
          |SET search_path TO ${options.schema};
          |
          |create table test_user(
          |  id integer not null,
          |  name varchar(255),
          |  primary key (id)
          |);
          |
          |create table article(
          |  id integer unique not null,
          |  article_unique_id uuid,
          |  author_id integer,
          |  is_published boolean,
          |  published_at timestamp
          |);
          |
          |ALTER TABLE article
          |  ADD CONSTRAINT author_id_fk
          |  FOREIGN KEY (author_id)
          |  REFERENCES test_user (id);
          |
          |create table article_active(
          |  article_id integer unique not null,
          |  active boolean
          |);
          |
          |ALTER TABLE article_active
          |  ADD CONSTRAINT article_id_fk
          |  FOREIGN KEY (article_id)
          |  REFERENCES article (id);
          |
          |COMMENT ON TABLE article_active IS 'This is to demonstrate a foreign primary key.';
      """.stripMargin
    stmt.executeUpdate(sql)
    conn.close()
    // By reading the file, we assert that the file compiles.
    val tablesPath = {
      val base = Seq(
        "src",
        "test",
        "scala",
        "com",
        "geirsson",
        "codegen",
        "Tables.scala"
      )
      // This project is a subdirectory of a closed source project.
      if (Paths.get("").toAbsolutePath.getFileName.toString == "launaskil") {
        "launaskil-codegen" +: base
      } else {
        base
      }
    }.mkString(File.separator)
    val expected = new String(
      Files.readAllBytes(Paths.get(tablesPath))
    )
    val baos = new ByteArrayOutputStream()
    val ps = new PrintStream(baos)
    Codegen.run(options, ps)
    val obtained = new String(baos.toByteArray, StandardCharsets.UTF_8)
    println(obtained)
    assert(structure(expected) == structure(obtained))
    assert(expected.trim === obtained.trim)
  }

}
