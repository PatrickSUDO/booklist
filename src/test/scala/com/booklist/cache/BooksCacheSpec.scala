package com.booklist.cache

import com.booklist.model.Book
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
class BooksCacheSpec extends AnyFlatSpec with Matchers {

  "A BooksCache" should "store and retrieve a book" in {
    val cache = new BooksCache()
    val bookList = List(Book("Title1", "Author1", "Publisher1", Some("2021")))

    cache.put("key1", bookList)
    cache.get("key1") should be(Some(bookList))
  }

  it should "overwrite an existing book with the same key" in {
    val cache = new BooksCache()
    val bookList1 = List(Book("Title1", "Author1", "Publisher1", Some("2021")))
    val bookList2 = List(Book("Title2", "Author2", "Publisher2", Some("2022")))

    cache.put("key1", bookList1)
    cache.put("key1", bookList2)

    cache.get("key1") should be(Some(bookList2))
  }

  it should "return None for a non-existent key" in {
    val cache = new BooksCache()
    cache.get("nonExistentKey") should be(None)
  }

  it should "evict cache entries after periods" in {
    val cache = new BooksCache(1)
    val bookList = List(Book("Title1", "Author1", "Publisher1", Some("2021")))

    cache.put("key1", bookList)

    // Sleep for a little over 3 minutes (considering some buffer time)
    Thread.sleep(1500)

    cache.get("key1") should be(None)
  }

  it should "handle concurrent accesses safely" in {
    val cache = new BooksCache()
    val bookList = List(Book("Title1", "Author1", "Publisher1", Some("2021")))

    // Use multiple threads to write to and read from the cache concurrently
    val tasks = (1 to 100).map { i =>
      Future {
        cache.put(s"key$i", bookList)
        cache.get(s"key$i")
      }
    }

    // Wait for all tasks to complete
    val results = Await.result(Future.sequence(tasks), 10.seconds)

    results.forall(_ == Some(bookList)) should be(true)
  }
}
