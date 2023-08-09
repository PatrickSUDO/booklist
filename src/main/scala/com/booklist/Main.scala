package com.booklist

import com.twitter.finagle.Http
import com.twitter.util.Await

object Main extends App {
  val nytClient = new NYTClient("Ei0q3V33kpdp2IX6mJK4rtPe0Vx1A9C1")
  private val service = new BookService(nytClient)

  private val server = Http.server.serve(":9090", service)

  Await.ready(server)
}