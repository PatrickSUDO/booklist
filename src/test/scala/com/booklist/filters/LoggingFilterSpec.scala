package com.booklist.filters

import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.{Await, Future}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.twitter.finagle.Service

class LoggingFiltersSpec extends AnyFlatSpec with Matchers with MockFactory {

  "LoggingFilter" should "chain to the provided service" in {
    val mockService = mock[Service[Request, Response]]
    val filter = new LoggingFilter()
    val request = Request("/v1/books/list?author=testAuthor")

    (mockService.apply _).expects(request).returning(Future.value(Response()))

    Await.result(filter(request, mockService))
  }
}
