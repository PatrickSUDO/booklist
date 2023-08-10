package com.booklist.client

import com.booklist.model.Book
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.{Http, Service}
import com.twitter.util.logging.Logger
import com.twitter.util.{Duration, Future}

import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.{Failure, Success, Try}

class NYTClient(apiKey: String) {
  private[this] val log = Logger(getClass)

  val client: Service[Request, Response] = Http.client
    .withTls("api.nytimes.com")
    .withRequestTimeout(Duration.fromSeconds(5)) // 5 second timeout
    .newService("api.nytimes.com:443")

  def getBooks(
      author: String,
      year: Option[String] = None
  ): Future[List[Book]] = {
    val params = Map(
      "author" -> author,
      "api-key" -> apiKey
    )

    val paramString =
      params.map { case (key, value) => s"$key=$value" }.mkString("&")
    val request = Request(
      s"/svc/books/v3/lists/best-sellers/history.json?$paramString"
    )

    client(request)
      .flatMap { response =>
        if (response.status != Status.Ok) {
          log.error(s"Error calling NYT API. Status code: ${response.status}")
          Future.exception(
            new Exception(
              s"Error calling NYT API, upstream status code ${response.status}"
            )
          )
        } else {
          parseBooks(response, year) match {
            case Right(books) => Future.value(books)
            case Left(error) =>
              log.error(s"Parsing error: $error")
              Future.exception(new Exception(error))
          }
        }
      }
      .rescue { case ex: Exception =>
        log.error(s"Exception: ${ex} occurred during getBooks")
        Future.exception(ex)
      }
  }

  private def parseBooks(
      response: Response,
      year: Option[String]
  ): Either[String, List[Book]] = {
    val mapper = JsonMapper
      .builder()
      .addModule(DefaultScalaModule)
      .build() :: ClassTagExtensions

    Try(mapper.readTree(response.contentString)) match {
      case Failure(ex) =>
        Left(s"Failed to parse response, ${ex.getMessage}")

      case Success(json) =>
        val booksJson = json.path("results").elements().asScala.toList
        val books = booksJson.flatMap { bookJson =>
          val publishedDate = bookJson
            .path("ranks_history")
            .elements()
            .asScala
            .toList
            .headOption
            .flatMap(node => Option(node.path("published_date").asText()))
          val bookYear = publishedDate.map(_.split("-")(0))

          if (year.isDefined && bookYear != year) None
          else {
            val title = bookJson.path("title").asText("")
            val publisher = bookJson.path("publisher").asText("")
            val author = bookJson.path("author").asText("")
            Some(Book(title, author, publisher, bookYear))
          }
        }

        if (books.isEmpty) {
          Left("No books found or data was missing/malformed.")
        } else {
          Right(books)
        }
    }
  }

}
