package org.ntb.imageresizer

import java.io.ByteArrayOutputStream
import java.net.URI
import org.apache.http.HttpException
import org.apache.http.HttpStatus._
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.ntb.imageresizer.MockHttpClients.canBeEqual
import org.ntb.imageresizer.io.Downloader
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import MockHttpClients.statusCodeHttpClient
import MockHttpClients.successfulHttpClient
import org.ntb.imageresizer.io.HttpClientProvider

@RunWith(classOf[JUnitRunner])
class FileDownloaderTest extends Specification with Mockito {
  import MockHttpClients._
  "FileDownloader" should {
    "successfully download data from HTTP URL to ByteString" in {
      val uri = URI.create("http://localhost/logo.png")
      val testData: Array[Byte] = Array(1, 2, 3)
      val httpClient = successfulHttpClient(testData)
      val downloader = fileDownloader(httpClient)
      val data = downloader.download(uri)
      val captor = ArgumentCaptor.forClass(classOf[HttpGet])
      there was one(httpClient).execute(captor.capture())
      captor.getValue().getURI() === uri
      data.seq === testData.toSeq
    }
    
    "successfully download data from HTTP URL to OutputStream" in {
      val uri = URI.create("http://localhost/logo.png")
      val testData: Array[Byte] = Array(1, 2, 3)
      val httpClient = successfulHttpClient(testData)
      val downloader = fileDownloader(httpClient)
      val output = new ByteArrayOutputStream()
      val data = downloader.download(uri, output)
      output.toByteArray().toSeq === testData.toSeq
    }
    
    "download if content is modified" in {
      val uri = URI.create("http://localhost/logo.png")
      val testData: Array[Byte] = Array(1, 2, 3)
      val httpClient = successfulHttpClient(testData)
      val downloader = fileDownloader(httpClient)
      val result = downloader.downloadIfModified(uri, 1)
      result must beSome.like {
        case data => data.toSeq === testData.toSeq
      }
    }
    
    "not download if content is not modified" in {
      val uri = URI.create("http://localhost/logo.png")
      val httpClient = statusCodeHttpClient(SC_NOT_MODIFIED)
      val downloader = fileDownloader(httpClient)
      val result = downloader.downloadIfModified(uri, 1)
      result must beNone
    }
    
    "throw HttpException when HTTP server respons with an error code" in {
      val httpClient = statusCodeHttpClient(404)
      val downloader = fileDownloader(httpClient)
      downloader.download(URI.create("http://localhost/logo.png")) must throwA[HttpException]
      there was one(httpClient).execute(any[HttpGet])
    }
  }
  
  def fileDownloader(httpClient: HttpClient): Downloader =
    new TestFileDownloader(httpClient)

  class TestFileDownloader(backingHttpClient: HttpClient) extends Downloader with HttpClientProvider {
    override val httpClient = backingHttpClient
  }
}