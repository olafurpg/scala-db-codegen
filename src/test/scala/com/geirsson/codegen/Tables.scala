package com.geirsson.codegen

/**
  * Generated using [[https://github.com/olafurpg/scala-db-codegen scala-db-codegen]]
  *  - Number of tables: 3
  *  - Database URL: jdbc:postgresql:postgres
  *  - Database schema: scala_db_codegen
  *  - Database user: postgres
  */
//noinspection ScalaStyle
object Tables {

  /**
    * Quill used to have this trait before v1, but it's still useful to keep.
    * Examples are: pattern matching on wrapped type and conversion to JSON objects.
    */
  trait WrappedValue[T] extends Any with WrappedType { self: AnyVal =>
    type Type = T
    def value: T
    override def toString = s"$value"
  }

  trait WrappedType extends Any {
    type Type
    def value: Type
  }

  /////////////////////////////////////////////////////
  // Article
  /////////////////////////////////////////////////////
  case class Article(id: Article.Id,
                     articleUniqueId: Option[Article.ArticleUniqueId],
                     authorId: Option[TestUser.Id],
                     isPublished: Option[Article.IsPublished],
                     publishedAt: Option[Article.PublishedAt])
  object Article {
    def create(id: Int,
               articleUniqueId: Option[java.util.UUID],
               authorId: Option[Int],
               isPublished: Option[Boolean],
               publishedAt: Option[java.time.LocalDateTime]): Article = {
      Article(Id(id),
              articleUniqueId.map(ArticleUniqueId.apply),
              authorId.map(TestUser.Id.apply),
              isPublished.map(IsPublished.apply),
              publishedAt.map(PublishedAt.apply))
    }
    case class Id(value: Int)                              extends AnyVal with WrappedValue[Int]
    case class ArticleUniqueId(value: java.util.UUID)      extends AnyVal with WrappedValue[java.util.UUID]
    case class IsPublished(value: Boolean)                 extends AnyVal with WrappedValue[Boolean]
    case class PublishedAt(value: java.time.LocalDateTime) extends AnyVal with WrappedValue[java.time.LocalDateTime]
  }

  /////////////////////////////////////////////////////
  // ArticleActive
  /////////////////////////////////////////////////////
  case class ArticleActive(articleId: Article.Id, active: Option[ArticleActive.Active])
  object ArticleActive {
    def create(articleId: Int, active: Option[Boolean]): ArticleActive = {
      ArticleActive(Article.Id(articleId), active.map(Active.apply))
    }
    case class Active(value: Boolean) extends AnyVal with WrappedValue[Boolean]
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