package com.booklist.service

import com.booklist.cache.BooksCache
import com.booklist.client.NYTClient
import com.twitter.finagle.http.{Method, Request, Status, Version}
import com.twitter.util.{Await, Future}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.booklist.model.Book

//import scala.collection.convert.ImplicitConversions.`collection asJava`
import scala.jdk.CollectionConverters
class BookServiceSpec extends AnyFlatSpec with Matchers with MockFactory {

  "apply" should "return a list of books from cache if present" in {
    val mockClient = mock[NYTClient]
    val mockCache = mock[BooksCache]
    val service = new BookService(mockClient)

    (mockCache.get _).expects("someAuthor-2023")
      .returning(Some(List(
        Book("t1", "a1", "me", Some("2023")),
        Book("t2", "a1", "you", Some("2023"))
      ))) // Mocked list of books

    val request = Request(Method.Get, "/v1/books/list")
    request.q.("author", "someAuthor")

    val response = Await.result(service(request))
    response.status shouldBe Status.Ok
    // Validate response content here
  }

  it should "fetch books from NYTClient if not in cache" in {
    val mockClient = mock[NYTClient]
    val service = new BookService(mockClient)

    (mockClient.getBooks _).expects("someAuthor", None).returning(Future.value(List(...))) // Mocked list of books

    val request = Request(Method.Get, "/v1/books/list")
    request.params.add("author", "someAuthor")

    val response = Await.result(service(request))
    response.status shouldBe Status.Ok
    // Validate response content here
  }

  it should "return a 400 error if the author is missing" in {
    val mockClient = mock[NYTClient]
    val service = new BookService(mockClient)

    val request = Request(Method.Get,"/v1/books/list")

    val response = Await.result(service(request))
    response.status shouldBe Status.BadRequest
  }

  it should "return a 404 error if the path is incorrect" in {
    val mockClient = mock[NYTClient]
    val service = new BookService(mockClient)

    val request = Request(Method.Get,"/incorrect/path")
    request.params.add("author", "someAuthor")

    val response = Await.result(service(request))
    response.status shouldBe Status.NotFound
    response.contentString shouldBe "Endpoint not found"
  }

  it should "return a 500 error if the NYTClient throws an exception" in {
    val mockClient = mock[NYTClient]
    val service = new BookService(mockClient)

    (mockClient.getBooks _).expects("someAuthor", None).returning(Future.exception(new Exception("Sample error")))

    val request = Request(Method.Get, "/v1/books/list")
    request.params.add("author", "someAuthor")

    val response = Await.result(service(request))
    response.status shouldBe Status.InternalServerError
    response.contentString shouldBe "Internal server error"
  }

  it should "return a 400 error if the author is an empty string" in {
    val mockClient = mock[NYTClient]
    val service = new BookService(mockClient)

    val request = Request(Method.Get, "/v1/books/list")
    request.params("author", "")

    val response = Await.result(service(request))
    response.status shouldBe Status.BadRequest
    response.contentString shouldBe "Missing 'author' parameter"
  }
}
