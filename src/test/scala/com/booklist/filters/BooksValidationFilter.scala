package com.booklist.filters

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.util.{Await, Future}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.twitter.finagle.Service

class BookBooksValidationFilter
    extends AnyFlatSpec
    with Matchers
    with MockFactory {

  "BooksValidationFilter" should "allow requests with valid path and author" in {
    val filter = new BooksValidationFilter
    val mockService = mock[Service[Request, Response]]
    val request = Request("/v1/books/list?author=testAuthor")

    (mockService.apply _)
      .expects(request)
      .returning(Future.value(Response(Status.Ok)))

    val response = Await.result(filter(request, mockService))
    response.status shouldBe Status.Ok
  }

  it should "reject requests with invalid path" in {
    val filter = new BooksValidationFilter
    val request = Request("/invalid/path")

    val response = Await.result(filter(request, null))
    response.status shouldBe Status.NotFound
    response.contentString shouldBe "Endpoint not found"
  }

  it should "reject requests without author parameter" in {
    val filter = new BooksValidationFilter
    val request = Request("/v1/books/list")

    val response = Await.result(filter(request, null))
    response.status shouldBe Status.BadRequest
    response.contentString shouldBe "Missing or empty 'author' parameter"
  }

  it should "reject requests with empty author parameter" in {
    val filter = new BooksValidationFilter
    val request = Request("/v1/books/list?author=")

    val response = Await.result(filter(request, null))
    response.status shouldBe Status.BadRequest
    response.contentString shouldBe "Missing or empty 'author' parameter"
  }
}
