package org.ntb.imageresizer

import java.io.ByteArrayInputStream
import java.net.URI

import org.apache.http.client.methods.HttpGet
import org.apache.http.client.HttpClient
import org.apache.http.HttpEntity
import org.apache.http.HttpException
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.junit.runner.RunWith
import org.scalatest.mock.MockitoSugar
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.when
import org.mockito.Matchers.any
import org.ntb.imageresizer.io.Downloader

object MockHttpClients extends MockitoSugar {
  def successfulHttpClient(data: Array[Byte], statusCode: Int = 200): HttpClient = {
    val statusLine = mock[StatusLine]
    val entity = mock[HttpEntity]
    val response = mock[HttpResponse]
    val httpClient = mock[HttpClient]
    when(statusLine.getStatusCode()).thenReturn(statusCode)
    when(entity.getContent()).thenReturn(new ByteArrayInputStream(data)).thenThrow(new RuntimeException("getContent already called"))
    when(response.getStatusLine()).thenReturn(statusLine)
    when(response.getEntity()).thenReturn(entity)
    when(httpClient.execute(any[HttpGet])).thenReturn(response)
    httpClient
  }

  def statusCodeHttpClient(statusCode: Int): HttpClient = {
    val statusLine = mock[StatusLine]
    val response = mock[HttpResponse]
    val httpClient = mock[HttpClient]
    when(statusLine.getStatusCode()).thenReturn(statusCode)
    when(response.getStatusLine()).thenReturn(statusLine)
    when(httpClient.execute(any[HttpGet])).thenReturn(response)
    httpClient
  }

  def failingHttpClient(e: Exception) = {
    val httpClient = mock[HttpClient]
    when(httpClient.execute(any[HttpGet])).thenThrow(e)
    httpClient
  }
}