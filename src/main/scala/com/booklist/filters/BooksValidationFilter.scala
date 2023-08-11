package com.booklist.filters

import com.booklist.utils.ResponseUtils._
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future

class BooksValidationFilter extends SimpleFilter[Request, Response] {

  override def apply(
      request: Request,
      service: Service[Request, Response]
  ): Future[Response] = {
    if (request.path != "/v1/books/list") {
      responseWith(Status.NotFound, "Endpoint not found")
    } else if (
      !request.params
        .contains("author") || request.params("author").trim.isEmpty
    ) {
      responseWith(Status.BadRequest, "Missing or empty 'author' parameter")
    } else {
      service(request)
    }
  }
}
