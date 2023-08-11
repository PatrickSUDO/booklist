package com.booklist.service

import com.booklist.cache.BooksCache
import com.booklist.client.NYTClient
import com.booklist.filters.BooksValidationFilter
import com.twitter.finagle.http.{Method, Request, Status}
import com.twitter.util.{Await, Future}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.booklist.model.Book
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

class BookServiceSpec extends AnyFlatSpec with Matchers with MockFactory {

  val mapper = JsonMapper.builder().addModule(DefaultScalaModule).build()
  private val validationFilter = new BooksValidationFilter()

  private val TestBooksLists = List(
    Book("t1", "a1", "me", Some("2023")),
    Book("t2", "a1", "you", Some("2023"))
  )

  private val ExpectedContent =
    """[
        {
            "title": "t1",
            "author": "a1",
            "publisher": "me",
            "year": "2023"
        },
        {
            "title": "t2",
            "author": "a1",
            "publisher": "you",
            "year": "2023"
        }
    ]"""

  "apply" should "return a list of books from cache if present" in {
    val mockClient = mock[NYTClient]
    val mockCache = mock[BooksCache]

    // Mocking the cache to return the books
    (mockCache.get _).expects("someAuthor-2023").returning(Some(TestBooksLists))

    // Create an instance of BookService using the mocked cache
    val service =
      validationFilter.andThen(new BookService(mockClient, mockCache))

    val request =
      Request(Method.Get, "/v1/books/list?author=someAuthor&year=2023")
    val response = Await.result(service(request))
    response.status shouldBe Status.Ok

    val expectedJson = mapper.readTree(ExpectedContent)
    val actualJson = mapper.readTree(response.contentString)
    actualJson shouldBe expectedJson
  }

  it should "fetch books from NYTClient if not in cache" in {
    val mockClient = mock[NYTClient]
    val service =
      validationFilter.andThen(new BookService(mockClient))

    (mockClient.getBooks _)
      .expects("someAuthor", Some("2023"))
      .returning(Future(TestBooksLists))

    val request =
      Request(Method.Get, "/v1/books/list?author=someAuthor&year=2023")

    val response = Await.result(service(request))
    response.status shouldBe Status.Ok

    val expectedJson = mapper.readTree(ExpectedContent)
    val actualJson = mapper.readTree(response.contentString)
    actualJson shouldBe expectedJson
  }

  it should "return a 400 error if the author is missing" in {
    val mockClient = mock[NYTClient]
    val service = validationFilter.andThen(new BookService(mockClient))
    val request = Request(Method.Get, "/v1/books/list")

    val response = Await.result(service(request))
    response.status shouldBe Status.BadRequest
  }

  it should "return a 404 error if the path is incorrect" in {
    val mockClient = mock[NYTClient]
    val service = validationFilter.andThen(new BookService(mockClient))
    val request = Request(Method.Get, "/incorrect/path")

    val response = Await.result(service(request))
    response.status shouldBe Status.NotFound
    response.contentString shouldBe "Endpoint not found"
  }

  it should "return a 500 error if the NYTClient throws an exception" in {
    val mockClient = mock[NYTClient]
    val service = validationFilter.andThen(new BookService(mockClient))

    (mockClient.getBooks _)
      .expects("someAuthor", None)
      .returning(Future.exception(new Exception("Sample error")))

    val request = Request(Method.Get, "/v1/books/list?author=someAuthor")

    val response = Await.result(service(request))
    response.status shouldBe Status.InternalServerError
    response.contentString shouldBe "Internal server error"
  }

  it should "return a 400 error if the author is an empty string" in {
    val mockClient = mock[NYTClient]
    val service = validationFilter.andThen(new BookService(mockClient))

    val request = Request(Method.Get, "/v1/books/list?author=")

    val response = Await.result(service(request))
    response.status shouldBe Status.BadRequest
    response.contentString shouldBe "Missing or empty 'author' parameter"
  }

  it should "return a 200 status with an empty list if NYTClient returns no books" in {
    val mockClient = mock[NYTClient]
    val service = validationFilter.andThen(new BookService(mockClient))

    // Mocking the client to return an empty list
    (mockClient.getBooks _)
      .expects("someAuthor", Some("2023"))
      .returning(Future(List.empty[Book]))

    val request =
      Request(Method.Get, "/v1/books/list?author=someAuthor&year=2023")

    val response = Await.result(service(request))
    response.status shouldBe Status.Ok

    val expectedContent = "[]"
    response.contentString shouldBe expectedContent
  }
}
