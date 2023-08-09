package com.booklist

import com.twitter.finagle.http.{Request, Response, Status, Version}
import com.twitter.finagle.Service
import com.twitter.util.Future
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.json.JsonMapper

class BookService(nytClient: NYTClient) extends Service[Request, Response] {
  def apply(request: Request): Future[Response] = {
    if (request.path != "/v1/books/list") {
      val response = Response(Version.Http11, Status.NotFound)
      response.setContentString("Endpoint not found")
      Future.value(response)
    } else {
      val author = request.params.get("author")
      val year = request.params.get("year")

      author match {
        case Some(a) =>
          nytClient.getBooks(a, year).map { books =>
            val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

            val response = Response(Version.Http11, Status.Ok)
            response.setContentString(mapper.writeValueAsString(books))
            response
          }
        case None =>
          val response = Response(Version.Http11, Status.BadRequest)
          response.setContentString("Missing 'author' parameter")
          Future.value(response)
      }
    }
  }
}