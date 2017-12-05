package com.geirsson.codegen

import scala.util.control.NonFatal

import caseapp.core.ArgParser

object TypeMap {
  implicit val parser: ArgParser[TypeMap] =
    ArgParser.instance[TypeMap] { s =>
      try {
        val pairs = s.split(";").map { pair =>
          val from :: to :: Nil = pair.split(",", 2).toList
          from -> to
        }
        Right(TypeMap(pairs: _*))
      } catch {
        case NonFatal(e) =>
          Left(s"invalid typeMap $s. Expected format from1,to1;from2,to2")
      }
    }
  val default = TypeMap(
    "text" -> "String",
    "longtext" -> "String",
    "float8" -> "Double",
    "datetime" -> "java.time.LocalDateTime",
    "date" -> "java.time.LocalDate",
    "decimal" -> "BigDecimal",
    "numeric" -> "BigDecimal",
    "tinyint" -> "Byte",
    "int" -> "Int",
    "int4" -> "Int",
    "int8" -> "Long",
    "bool" -> "Boolean",
    "bit" -> "Boolean",
    "varchar" -> "String",
    "serial" -> "Int",
    "bigserial" -> "Long",
    "timestamp" -> "java.time.OffsetDateTime",
    "bytea" -> "Array[Byte]", // PostgreSQL
    "uuid" -> "java.util.UUID", // H2, PostgreSQL
    "json" -> "String" // PostgreSQL
  )
}

case class TypeMap(pairs: (String, String)*)
