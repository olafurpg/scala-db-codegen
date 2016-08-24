package com.geirsson.codegen
import java.util.Date
import io.getquill.WrappedValue

object Tables {
  /////////////////////////////////////////////////////
  // Article
  /////////////////////////////////////////////////////
  case class Article(id: Article.Id, authorId: Option[TestUser.Id], isPublished: Option[Article.IsPublished])
  object Article {
    def create(id: Int, authorId: Option[Int], isPublished: Option[Boolean]): Article = {
      Article(Id(id), authorId.map(TestUser.Id.apply), isPublished.map(IsPublished.apply))
    }
    case class Id(value: Int)              extends AnyVal with WrappedValue[Int]
    case class IsPublished(value: Boolean) extends AnyVal with WrappedValue[Boolean]
  }

  /////////////////////////////////////////////////////
  // TestUser
  /////////////////////////////////////////////////////
  case class TestUser(id: TestUser.Id, name: Option[TestUser.Name])
  object TestUser {
    def create(id: Int, name: Option[String]): TestUser = {
      TestUser(Id(id), name.map(Name.apply))
    }
    case class Id(value: Int)      extends AnyVal with WrappedValue[Int]
    case class Name(value: String) extends AnyVal with WrappedValue[String]
  }
}