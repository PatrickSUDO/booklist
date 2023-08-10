package com.booklist.client

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.Service
import com.twitter.util.{Await, Future}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.booklist.model.Book
import java.nio.file.{Files, Paths}
class NYTClientSpec extends AnyFlatSpec with Matchers with MockFactory {

  // Helper function to load JSON from a fixture file
  def loadJsonFixture(filename: String): String = {
    new String(Files.readAllBytes(Paths.get(s"fixtures/$filename")))
  }

  "getBooks" should "return a list of books for a valid response" in {
    val mockService = mock[Service[Request, Response]]
    val nytClient = new NYTClient("dummyApiKey") {
      override val client: Service[Request, Response] = mockService
    }

    val mockResponse = mock[Response]
    (mockResponse.status _).expects().returning(Status.Ok)
    (mockResponse.contentString _)
      .expects()
      .returning(loadJsonFixture("test/fixtures/validResponse.json"))

    (mockService.apply _).expects(*).returning(Future.value(mockResponse))

    val result = Await.result(nytClient.getBooks("someAuthor"))
    result shouldBe List(
      Book("PRINCIPLES", "Ray Dalio", "Simon & Schuster", Some("2021")),
      Book("PRINCIPLES FOR DEALING WITH THE CHANGING WORLD ORDER", "Ray Dalio", "Avid Reader", Some("2022"))
    )
  }

  it should "handle errors from the NYT API gracefully" in {
    val mockService = mock[Service[Request, Response]]
    val nytClient = new NYTClient("dummyApiKey") {
      override val client: Service[Request, Response] = mockService
    }

    val mockResponse = mock[Response]
    (mockResponse.status _).expects().returning(Status.BadRequest)
    (mockService.apply _).expects(*).returning(Future.value(mockResponse))

    a[Exception] should be thrownBy Await.result(
      nytClient.getBooks("someAuthor")
    )
  }
}
