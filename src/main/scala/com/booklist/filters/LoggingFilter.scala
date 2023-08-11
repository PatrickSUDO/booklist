package com.booklist.filters

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import com.twitter.util.logging.Logger

class LoggingFilter extends SimpleFilter[Request, Response] {
  // Using Finagle's built-in logger
  private[this] val logger = Logger(getClass)
  def apply(
      request: Request,
      service: Service[Request, Response]
  ): Future[Response] = {
    logger.info(
      s"Received request at ${request.uri} from ${request.remoteAddress}"
    )
    service(request)
  }
}
