package com.booklist.utils

import com.booklist.model.Book
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twitter.finagle.http.{Response, Status, Version}
import com.twitter.util.Future

object ResponseUtils {

  val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
  def constructResponse(books: List[Book]): Future[Response] = {
    responseWith(Status.Ok, mapper.writeValueAsString(books))
  }

  def responseWith(
      status: Status,
      content: String
  ): Future[Response] = {
    val response = Response(Version.Http11, status)
    response.setContentString(content)
    Future.value(response)
  }

}
