package com.booklist

import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.{Http, Service}
import com.twitter.util.Future
import com.fasterxml.jackson.databind.json.JsonMapper

import scala.jdk.CollectionConverters.IteratorHasAsScala
import com.fasterxml.jackson.module.scala.ClassTagExtensions

class NYTClient(apiKey: String) {
  val client: Service[Request, Response] = Http.client
    .withTls("api.nytimes.com")
    .newService("api.nytimes.com:443")

  def getBooks(author: String, year: Option[String] = None): Future[List[Book]] = {
    val params = Map(
      "author" -> author,
      "api-key" -> apiKey
    )

    val paramString = params.map { case (key, value) => s"$key=$value" }.mkString("&")
    val request = Request(s"/svc/books/v3/lists/best-sellers/history.json?$paramString")
    client(request).flatMap { response =>
      if (response.status != Status.Ok) Future.exception(new Exception(s"Error calling NYT API, upstream status code ${response.status}"))
      else Future.value(parseBooks(response, year))
    }
  }

  private def parseBooks(response: Response, year: Option[String]): List[Book] = {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build() :: ClassTagExtensions


    val json = mapper.readTree(response.contentString)
    val booksJson = json.path("results").elements().asScala.toList

    booksJson.flatMap { bookJson =>
      val publishedDate = bookJson.path("ranks_history").elements().asScala.toList.headOption.flatMap(node => Option(node.path("published_date").asText()))
      val bookYear = publishedDate.map(_.split("-")(0))

      if (year.isDefined && bookYear != year) None
      else {
        val title = bookJson.path("title").asText()
        val publisher = bookJson.path("publisher").asText()
        val author = bookJson.path("author").asText()
        Some(Book(title, publisher, author, bookYear))
      }
    }
  }
}
