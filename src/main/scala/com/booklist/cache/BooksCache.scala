package com.booklist.cache

import com.booklist.model.Book
import com.github.benmanes.caffeine.cache.Caffeine
import com.twitter.util.logging.Logger

import java.util.concurrent.TimeUnit

class BooksCache {
  private val log = Logger(getClass)

  private val cache = Caffeine
    .newBuilder()
    .expireAfterWrite(3, TimeUnit.MINUTES)
    .maximumSize(5000)
    .build[String, List[Book]]()

  def get(key: String): Option[List[Book]] = {
    val data = Option(cache.getIfPresent(key))
    if (data.isDefined) {
      println(s"Retrieved data for key $key from cache.")
      log.info(s"Retrieved data for key $key from cache.")
    }
    data
  }

  def put(key: String, books: List[Book]): Unit = {
    cache.put(key, books)
    println(s"Data for key $key added to cache.")
    log.info(s"Data for key $key added to cache.")
  }
}