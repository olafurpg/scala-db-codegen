package com.geirsson.codegen.codegen

case class CodegenOptions(
    username: String = "postgres",
    url: String = "jdbc:postgresql:postgres",
    password: String = "postgres",
    schema: String = "public",
    jdbcDriver: String = "org.postgresql.Driver",
    imports: String = """|import java.util.Date
                         |import io.getquill.WrappedValue""".stripMargin,
    packageName: String = "is.launaskil.models",
    typeMap: TypeMap = TypeMap.default,
    file: Option[String]
)
