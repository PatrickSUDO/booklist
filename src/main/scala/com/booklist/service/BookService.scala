package com.booklist.service

import com.booklist.cache.BooksCache
import com.booklist.client.NYTClient
import com.booklist.model.Book
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status, Version}
import com.twitter.util.Future
import com.twitter.util.logging.Logger

class BookService(nytClient: NYTClient, booksCache: BooksCache = new BooksCache()) extends Service[Request, Response] {
  private[this] val log = Logger(getClass)

  def apply(request: Request): Future[Response] = {
    if (request.path != "/v1/books/list") {
      responseWith(Status.NotFound, "Endpoint not found")
    } else {
      request.params.get("author") match {
        case Some(author) if author.trim.nonEmpty =>
          val cacheKey =
            s"$author-${request.params.get("year").getOrElse("N/A")}"
          booksCache.get(cacheKey) match {
            case Some(books) => constructResponse(books)
            case None =>
              fetchBooksFromNYT(author, request.params.get("year"), cacheKey)
          }
        case _ =>
          responseWith(Status.BadRequest, "Missing 'author' parameter")
      }
    }
  }

  private def fetchBooksFromNYT(
      author: String,
      year: Option[String],
      cacheKey: String
  ): Future[Response] = {
    nytClient
      .getBooks(author, year)
      .flatMap { books =>
        booksCache.put(cacheKey, books)
        constructResponse(books)
      }
      .rescue { case ex: Exception =>
        log.error(s"Exception: $ex occurred during apply method")
        responseWith(Status.InternalServerError, "Internal server error")
      }
  }

  private def constructResponse(books: List[Book]): Future[Response] = {
    val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
    responseWith(Status.Ok, mapper.writeValueAsString(books))
  }

  private def responseWith(
      status: Status,
      content: String
  ): Future[Response] = {
    val response = Response(Version.Http11, status)
    response.setContentString(content)
    Future.value(response)
  }
}
