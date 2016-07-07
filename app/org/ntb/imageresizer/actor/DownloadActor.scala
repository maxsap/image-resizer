package org.ntb.imageresizer.actor

import javax.inject.Inject

import akka.actor.Actor
import akka.pattern.pipe
import akka.util.ByteString
import play.api.Logger
import play.api.http.{ HeaderNames, Status }
import play.api.libs.ws.WSClient

import scala.concurrent.duration._

class DownloadActor @Inject() (ws: WSClient) extends Actor with ActorUtils {
  import context.dispatcher
  import org.ntb.imageresizer.actor.DownloadActor._

  def receive = {
    case DownloadRequest(url) ⇒
      Logger.debug(s"Downloading image from $url")
      try {
        validateUrl(url)
        val request = ws
          .url(url)
          .withRequestTimeout(10.seconds)
        request.get() map { r ⇒
          if (r.status != Status.OK) {
            throw new DownloadException(r.status, r.statusText, r.body)
          }
          val data = r.bodyAsBytes
          Logger.debug(s"Downloaded ${data.length} bytes of image with type ${r.header(HeaderNames.CONTENT_TYPE).getOrElse("unknown")} from $url")
          DownloadResponse(data)
        } pipeTo sender()
      } catch {
        // ws methods can throw before the future is created
        case e: Exception ⇒ sender() ! akka.actor.Status.Failure(e)
      }
  }
}

object DownloadActor {
  case class DownloadRequest(url: String)
  case class DownloadResponse(data: ByteString)
  class DownloadException(status: Int, statusText: String, body: String) extends RuntimeException(s"Server responded with HTTP $status $statusText: $body")

  class InvalidUrlException(msg: String, cause: Throwable) extends RuntimeException(msg, cause)

  def validateUrl(url: String) {
    try {
      org.asynchttpclient.uri.Uri.create(url)
    } catch {
      case e: NullPointerException ⇒ throw new InvalidUrlException("URL segment is missing", e)
      case e: IllegalArgumentException ⇒ throw new InvalidUrlException("Invalid URL", e)
    }
  }
}

