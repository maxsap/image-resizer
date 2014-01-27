package org.ntb.imageresizer.actor.file

import akka.actor.ActorRef
import akka.actor.{ Props, ActorSystem }
import akka.testkit.{ ImplicitSender, TestKit, TestProbe }
import com.google.common.io.Files
import java.io.File
import java.net.URI
import java.util.UUID
import org.ntb.imageresizer.actor.Key
import org.scalatest.FlatSpecLike
import org.scalatest.Matchers
import scala.concurrent.duration._

class FileCacheImageBrokerActorTest extends TestKit(ActorSystem("TestSystem")) with ImplicitSender with FlatSpecLike with Matchers {
  import DownloadActor._
  import FileCacheImageBrokerActor._
  import FileCacheImageBrokerActorTest._
  import ResizeActor._

  val testData: Array[Byte] = Array(1.toByte, 2.toByte, 3.toByte)
  val timeout = 2.seconds

  "FileCacheImageBrokerActor" should "serve existing file" in {
    val testFile = tempFile(testData)
    val downloadActor = TestProbe()
    val resizeActor = TestProbe()
    val testFileProvider: Key ⇒ File = k ⇒ testFile
    val imageBrokerActor = system.actorOf(Props(classOf[TestFileCacheImageBrokerActor], downloadActor.ref, resizeActor.ref, testFileProvider))
    imageBrokerActor ! GetImageRequest(new URI("http://localhost/file1.png"), 200)
    expectMsgPF(timeout) {
      case GetImageResponse(data) ⇒ data.getAbsolutePath should equal(testFile.getAbsolutePath)
    }
    system.stop(imageBrokerActor)
    testFile.delete()
  }

  it should "download and resize nonexisting file" in {
    val testFile = nonExistingFile()
    val downloadProbe = TestProbe()
    val resizeProbe = TestProbe()
    val testFileProvider: Key ⇒ File = k ⇒ testFile
    val imageBrokerActor = system.actorOf(Props(classOf[TestFileCacheImageBrokerActor], downloadProbe.ref, resizeProbe.ref, testFileProvider))
    imageBrokerActor ! GetImageRequest(new URI("http://localhost/file2.png"), 200)
    downloadProbe.expectMsgPF(timeout) {
      case DownloadRequest(uri, target) ⇒
        Files.write(testData, target)
        downloadProbe.reply(DownloadResponse(target, target.length()))
    }
    resizeProbe.expectMsgPF(timeout) {
      case ResizeImageRequest(source, target, _, _) ⇒
        Files.copy(source, target)
        resizeProbe.reply(ResizeImageResponse(target.length()))
    }
    expectMsgPF(timeout) {
      case GetImageResponse(data) ⇒
        testFile should be('exists)
        Files.toByteArray(testFile) should equal(testData)
        data.getAbsolutePath should equal(testFile.getAbsolutePath)
    }
    system.stop(imageBrokerActor)
    testFile.delete()
  }
}

object FileCacheImageBrokerActorTest {
  def nonExistingFile(): File = {
    val file = new File(UUID.randomUUID().toString)
    file.deleteOnExit()
    file
  }

  def tempFile(data: Array[Byte] = Array()): File = {
    val tempFile = File.createTempFile(getClass.getName, ".tmp")
    tempFile.deleteOnExit()
    if (data.length > 0) {
      Files.write(data, tempFile)
    }
    tempFile
  }

  class TestFileCacheImageBrokerActor(downloader: ActorRef, resizer: ActorRef, provider: Key ⇒ File)
      extends FileCacheImageBrokerActor(downloader, resizer) {
    override val cacheFileProvider = provider
  }
}