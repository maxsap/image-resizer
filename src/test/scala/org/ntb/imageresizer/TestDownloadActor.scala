package org.ntb.imageresizer

import actor.DownloadActor
import io.HttpClientProvider
import org.apache.http.client.HttpClient

class TestDownloadActor(backingHttpClient: HttpClient) extends DownloadActor with HttpClientProvider {
  override val httpClient = backingHttpClient
}