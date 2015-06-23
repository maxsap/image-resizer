package org.ntb.imageresizer.resize

import java.awt.image.BufferedImage
import java.io.{ InputStream, File }
import java.net.URL

import akka.util.ByteString
import org.ntb.imageresizer.resize.Resizer._

object ResolutionTool extends JavaImageIOImageReader {
  def readResolution(input: InputStream): (Int, Int) =
    usingImage(read(input))(getResolution)

  def readResolution(input: File): (Int, Int) =
    usingImage(read(input))(getResolution)

  def readResolution(input: URL): (Int, Int) =
    usingImage(read(input))(getResolution)

  def readResolution(input: ByteString): (Int, Int) =
    usingImage(read(input))(getResolution)

  def getResolution(image: BufferedImage): (Int, Int) =
    (image.getWidth, image.getHeight)
}
