# scala-db-codegen

Generate Scala code from your database to use with the incredble library [quill](https://github.com/getquill/quill).
Only tested with postgresql, but could in theory work with any jdbc compliant database.

## What does it do?

Say you have some database with this schema

```sql
create table test_users_main(
  id integer not null,
  name varchar(255),
  primary key (id)
);

create table articles(
  id integer not null,
  author_id integer not null,
  is_published boolean
);

ALTER TABLE articles
  ADD CONSTRAINT author_id_fk
  FOREIGN KEY (author_id)
  REFERENCES test_users_main (id);
```

scala-db-codegen will then generate "type all the things!" code like this

```
package tables
import java.util.Date
import io.getquill.WrappedValue

object Tables {
  /////////////////////////////////////////////////////
  // Articles
  /////////////////////////////////////////////////////
  case class Articles(id: Articles.Id, authorId: TestUsersMain.Id, isPublished: Articles.IsPublished)
  object Articles {
    def create(id: Int, authorId: Int, isPublished: Boolean): Articles = {
      Articles(Id(id), TestUsersMain.Id(authorId), IsPublished(isPublished))
    }
    case class Id(value: Int)              extends AnyVal with WrappedValue[Int]
    case class IsPublished(value: Boolean) extends AnyVal with WrappedValue[Boolean]
  }

  /////////////////////////////////////////////////////
  // TestUsersMain
  /////////////////////////////////////////////////////
  case class TestUsersMain(id: TestUsersMain.Id, name: TestUsersMain.Name)
  object TestUsersMain {
    def create(id: Int, name: String): TestUsersMain = {
      TestUsersMain(Id(id), Name(name))
    }
    case class Id(value: Int)      extends AnyVal with WrappedValue[Int]
    case class Name(value: String) extends AnyVal with WrappedValue[String]
  }
}
```

It could in theory also generate the code differently.

## CLI

```shell
$ db-codegen --help
db-codegen 0.1.0
Usage: db-codegen [options]
  --usage
        Print usage and exit
  --help | -h
        Print help message and exit
  --user  <value>
        user on database server
  --password  <value>
        password for user on database server
  --url  <value>
        jdbc url
  --schema  <value>
        schema on database
  --jdbc-driver  <value>
        only tested with postgresql
  --imports  <value>
        top level imports of generated file
  --package  <value>
        package name for generated classes
  --type-map  <value>
        Which types should write to which types? Format is: numeric,BigDecimal;int8,Long;...
  --excluded-tables  <value>
        Do not generate classes for these tables.
  --file  <value>
        Write generated code to this filename. Prints to stdout if not set.
```

## Standalone library

```scala
// 2.11 only
libraryDependencies += "com.geirsson" %% "codegen" % "0.1.0"
```

Consult the source usage, at least for now ;)


