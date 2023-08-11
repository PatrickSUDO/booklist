package com.booklist.utils

import com.twitter.finagle.http.Status
import com.twitter.util.Await

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.booklist.model.Book

class ResponseUtilsSpec extends AnyFlatSpec with Matchers {

  "constructResponse" should "return a response with Status.Ok and serialized content" in {
    val books = List(Book("title1", "author1", "publisher1", Some("2023")))
    val response = Await.result(ResponseUtils.constructResponse(books))
    response.status shouldBe Status.Ok

    val expectedContent =
      """[{"title":"title1","author":"author1","publisher":"publisher1","year":"2023"}]"""
    response.contentString shouldBe expectedContent
  }

  "responseWith" should "return a response with the provided status and content" in {
    val testStatus = Status.BadRequest
    val testContent = "Bad Request"
    val response =
      Await.result(ResponseUtils.responseWith(testStatus, testContent))
    response.status shouldBe testStatus
    response.contentString shouldBe testContent
  }
}
