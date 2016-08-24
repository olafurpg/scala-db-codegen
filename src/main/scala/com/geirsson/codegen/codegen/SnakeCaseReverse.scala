package com.geirsson.codegen.codegen

import io.getquill.NamingStrategy

object SnakeCaseReverse extends SnakeCaseReverse

trait SnakeCaseReverse extends NamingStrategy {
  override def column(s: String): String = {
    val camelCased = default(s)
    camelCased.head.toLower + camelCased.tail
  }

  override def default(s: String): String = {
    s.toLowerCase
      .split("_")
      .map { // avoid possible collisions caused by multiple '_'
        case "" => "_"
        case s => s
      }
      .map(_.capitalize)
      .mkString("")
  }
}