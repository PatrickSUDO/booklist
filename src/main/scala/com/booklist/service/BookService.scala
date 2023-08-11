package com.booklist.service

import com.booklist.cache.BooksCache
import com.booklist.client.NYTClient

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.util.Future
import com.twitter.util.logging.Logger
import com.booklist.utils.ResponseUtils._

class BookService(
    nytClient: NYTClient,
    booksCache: BooksCache = new BooksCache()
) extends Service[Request, Response] {

  private[this] val log = Logger(getClass)

  def apply(request: Request): Future[Response] = {
    val author = request.params("author")
    val cacheKey = s"$author-${request.params.get("year").getOrElse("N/A")}"
    booksCache.get(cacheKey) match {
      case Some(books) => constructResponse(books)
      case None =>
        fetchBooksFromNYT(author, request.params.get("year"), cacheKey)
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
}
