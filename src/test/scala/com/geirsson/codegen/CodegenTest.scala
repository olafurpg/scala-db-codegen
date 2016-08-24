package com.geirsson.codegen

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.sql.DriverManager

import org.scalatest.FunSuite

class CodegenTest extends FunSuite {

  def structure(code: String): String = {
    import scala.meta._
    code.parse[Source].get.structure
  }

  test("testMain") {
    val options = CodegenOptions(
      `package` = "my.custom"
    )
    Class.forName(options.jdbcDriver)
    val conn =
      DriverManager
        .getConnection(options.url, options.username, options.password)
    val stmt = conn.createStatement()
    val sql =
      """
        |drop table if exists articles;
        |drop table if exists test_users_main;
        |create table test_users_main(
        |  id integer not null,
        |  name varchar(255),
        |  primary key (id)
        |);
        |
        |create table articles(
        |  id integer not null,
        |  author_id integer not null,
        |  is_published boolean
        |);
        |
        |ALTER TABLE articles
        |  ADD CONSTRAINT author_id_fk
        |  FOREIGN KEY (author_id)
        |  REFERENCES test_users_main (id);
      """.stripMargin
    stmt.executeUpdate(sql)
    conn.close()
    val expected =
      s"""|
          |package my.custom
          |import java.util.Date
          |import io.getquill.WrappedValue
          |
          |object Tables {
          |  /////////////////////////////////////////////////////
          |  // Articles
          |  /////////////////////////////////////////////////////
          |  case class Articles(id: Articles.Id, authorId: TestUsersMain.Id, isPublished: Articles.IsPublished)
          |  object Articles {
          |    def create(id: Int, authorId: Int, isPublished: Boolean): Articles = {
          |      Articles(Id(id), TestUsersMain.Id(authorId), IsPublished(isPublished))
          |    }
          |    case class Id(value: Int)              extends AnyVal with WrappedValue[Int]
          |    case class IsPublished(value: Boolean) extends AnyVal with WrappedValue[Boolean]
          |  }
          |
          |  /////////////////////////////////////////////////////
          |  // TestUsersMain
          |  /////////////////////////////////////////////////////
          |  case class TestUsersMain(id: TestUsersMain.Id, name: TestUsersMain.Name)
          |  object TestUsersMain {
          |    def create(id: Int, name: String): TestUsersMain = {
          |      TestUsersMain(Id(id), Name(name))
          |    }
          |    case class Id(value: Int)      extends AnyVal with WrappedValue[Int]
          |    case class Name(value: String) extends AnyVal with WrappedValue[String]
          |  }
          |}
      """.stripMargin
    val baos = new ByteArrayOutputStream()
    val ps = new PrintStream(baos)
    Codegen.run(options, ps)
    val obtained = new String(baos.toByteArray, StandardCharsets.UTF_8)
    println(obtained)
    assert(expected.trim === obtained.trim)
    assert(structure(expected) == structure(obtained))
  }

}
