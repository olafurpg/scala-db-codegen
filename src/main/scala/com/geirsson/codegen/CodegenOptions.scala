package com.geirsson.codegen

case class CodegenOptions(
    username: String = "postgres",
    url: String = "jdbc:postgresql:postgres",
    password: String = "postgres",
    schema: String = "public",
    jdbcDriver: String = "org.postgresql.Driver",
    imports: String = """|import java.util.Date
                         |import io.getquill.WrappedValue""".stripMargin,
    `package`: String = "tables",
    typeMap: TypeMap = TypeMap.default,
    excludedTables: List[String] = List("schema_version"),
    file: Option[String] = None
)
