package com.booklist.model

case class Book(
    title: String,
    author: String,
    publisher: String,
    year: Option[String]
)
