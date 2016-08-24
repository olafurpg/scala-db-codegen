package com.geirsson.codegen

import scala.util.control.NonFatal

import caseapp.core.ArgParser

object TypeMap {
  implicit val parser: ArgParser[TypeMap] =
    ArgParser.instance[TypeMap] { s =>
      try {
        val pairs = s.split(";").map { pair =>
          val from :: to :: Nil = pair.split(",", 1).toList
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
    "float8" -> "Double",
    "numeric" -> "BigDecimal",
    "int4" -> "Int",
    "int8" -> "Long",
    "bool" -> "Boolean",
    "varchar" -> "String",
    "serial" -> "Int",
    "bigserial" -> "Long",
    "timestamp" -> "Date"
  )
}

case class TypeMap(pairs: (String, String)*)
