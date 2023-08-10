package com.booklist

import com.booklist.client.NYTClient
import com.booklist.service.BookService
import com.twitter.finagle.Http
import com.twitter.util.logging.{Logger, Logging}
import com.twitter.util.Await

object Main extends App with Logging {
  private[this] val log = Logger(getClass)
  // Fetching the API key from environment variables
  val apiKey: String = sys.env.getOrElse(
    "NYT_API_KEY",
    throw new IllegalStateException("NYT_API_KEY environment variable not set")
  )
  val nytClient = new NYTClient(apiKey)
  private val service = new BookService(nytClient)

  try {
    val server = Http.server.serve(":9090", service)
    log.info("Server started on port 9090")
    Await.ready(server)
  } catch {
    case ex: Exception =>
      log.error(s"Error starting the server, ${ex}")
  }
}
