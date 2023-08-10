package com.booklist

import com.twitter.finagle.http.{Request, Response, Status, Version}
import com.twitter.finagle.Service
import com.twitter.util.Future
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.benmanes.caffeine.cache.Caffeine
import com.twitter.util.logging.Logger

import java.util.concurrent.TimeUnit

class BookService(nytClient: NYTClient) extends Service[Request, Response] {
  private[this] val log = Logger(getClass)

  // Define the cache in BookService
  private val booksCache = Caffeine
    .newBuilder()
    .expireAfterWrite(3, TimeUnit.MINUTES)
    .maximumSize(5000)
    .build[String, List[Book]]()

  def apply(request: Request): Future[Response] = {
    if (request.path != "/v1/books/list") {
      val response = Response(Version.Http11, Status.NotFound)
      response.setContentString("Endpoint not found")
      Future.value(response)
    } else {
      val authorOpt = request.params.get("author")
      val year = request.params.get("year")

      authorOpt match {
        case Some(author) =>
          val cacheKey = s"$author-${year.getOrElse("N/A")}"
          val cachedBooks = Option(booksCache.getIfPresent(cacheKey))

          cachedBooks match {
            case Some(books) =>
              log.info(s"Retrieved data for key $cacheKey from cache.")
              val mapper =
                JsonMapper.builder().addModule(DefaultScalaModule).build()
              val response = Response(Version.Http11, Status.Ok)
              response.setContentString(mapper.writeValueAsString(books))
              Future.value(response)

            case None =>
              log.info(
                s"No data in cache for key $cacheKey. Fetching from NYTClient."
              )
              nytClient.getBooks(author, year).map { books =>
                booksCache.put(cacheKey, books) // Add to cache after fetching
                log.info(s"Data for key $cacheKey added to cache.")
                val mapper =
                  JsonMapper.builder().addModule(DefaultScalaModule).build()
                val response = Response(Version.Http11, Status.Ok)
                response.setContentString(mapper.writeValueAsString(books))
                response
              } rescue { case ex: Exception =>
                log.error(s"Exception: ${ex} occurred during apply method")
                val response =
                  Response(Version.Http11, Status.InternalServerError)
                response.setContentString("Internal server error")
                Future.value(response)
              }

          }

        case None =>
          val response = Response(Version.Http11, Status.BadRequest)
          response.setContentString("Missing 'author' parameter")
          Future.value(response)
      }
    }
  }
}
