package com.cliff.memeify

import java.awt.Font
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import javax.imageio.ImageIO


/**
 * Adds text to the top and bottom of a image. Uses Java's BufferedImage, Font and Graphics2d classes from the
 * java.awt package for all image manipulation. Image should be at least 400 x 300 otherwise the generated text
 * will be too small to read.
 *
 * @author Cliff
 */
class Memeify {

    companion object {
        // font size should 5% of image height
        val FONT_SIZE_PERCENTAGE = 0.05

        fun memeify(image:ByteArray, imageFormat: String, topText:String, botText: String): ByteArray {
            val img = loadImage(image)
            val width = img.width
            val height = img.height
            println("image width: $width height: $height")

            val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val g2d = bufferedImage.createGraphics()

            // compute font size relative to image dimensions
            val fontSize = computeFontSize(width, height)
            println("fontSize=$fontSize")
            g2d.font = Font(Font.SANS_SERIF, Font.BOLD, fontSize)

            // draw graphics
            g2d.drawImage(img, 0, 0, null)

            // determine if top line needs to be split, then draw the top text lines
            println("top text width: ${g2d.fontMetrics.stringWidth(topText)}")
            val topLines = fitLineToWidth(topText, g2d.fontMetrics.stringWidth(topText), width)
            drawTopText(g2d, topLines, width, height)
            // draw bottom lines
            println("bottom text width: ${g2d.fontMetrics.stringWidth(botText)}")
            val botLines = fitLineToWidth(botText, g2d.fontMetrics.stringWidth(botText), width)
            drawBottomText(g2d, botLines, width, height)

            g2d.dispose()

            val imageBytes = toByteArray(bufferedImage, imageFormat )
            return imageBytes
        }

        fun loadImage(path: String): BufferedImage {
            return ImageIO.read( File(path) )
        }

        fun loadImage(imageBytes: ByteArray): BufferedImage {
            return ImageIO.read( imageBytes.inputStream() )
        }

        fun saveAs(imageName: String, extension: String, image: BufferedImage): File {
            val file = File("$imageName.$extension")
            ImageIO.write(image, extension, file)
            return file
        }

        fun toByteArray(bufImage: BufferedImage, imgFormat: String ): ByteArray {
            val baos = ByteArrayOutputStream()
            baos.use {
                ImageIO.write(bufImage, imgFormat, it)
            }
            return baos.toByteArray()
        }

        fun computeFontSize(imageWidth: Int, imageHeight: Int): Int {
            return if (imageWidth > imageHeight) {
                Math.ceil(imageHeight * FONT_SIZE_PERCENTAGE).toInt()
            } else {
                Math.ceil(imageWidth * FONT_SIZE_PERCENTAGE).toInt()
            }
        }

        // return the x coordinate start position so that text will be centered within an image
        fun xStartPos(stringWidth: Int, imageWidth: Int): Int {
            return Math.abs(imageWidth - stringWidth) / 2
        }

        fun fitLineToWidth(line: String, stringWidth: Int, imageWidth: Int): List<String> {
            val lines = ArrayList<String>()
            lines.add(line)
            if (stringWidth > imageWidth && stringWidth > 0) {
                val df = DecimalFormat("#.####")
                df.roundingMode = RoundingMode.CEILING
                // compute the percentage of words to take for the first line
                val splitPct = 1.0 - ( (stringWidth - imageWidth) / stringWidth.toDouble() )
                val words = line.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                val wordsToTake = (words.size * splitPct).toInt()
                val firstLine = words.take( wordsToTake ).joinToString(" ")
                val secondLine = words.takeLast( words.size - wordsToTake ).joinToString(" ")
                lines.clear()
                lines.add(firstLine)
                lines.add(secondLine)
            }
            return lines
        }

        // draw the lines of text that will appear at the top of the image
        fun drawTopText(g: Graphics, lines: List<String>, width: Int, height: Int) {
            // draw top text lines
            var yPos = computeFontSize(width, height)
            for (line in lines) {
                val xPos = xStartPos(g.fontMetrics.stringWidth(line), width)
                g.drawString(line, xPos, yPos)
                yPos += yPos
            }
        }

        // draw the lines of text that will appear at the bottom of the image
        fun drawBottomText(g: Graphics, lines: List<String>, width: Int, height: Int) {
            val fontSize = computeFontSize(width, height)
            var yPos = height - (fontSize * 2) // * 2 because we only allow a max of two lines
            for (line in lines) {
                val xPos = xStartPos(g.fontMetrics.stringWidth(line), width)
                g.drawString(line, xPos, yPos)
                yPos += fontSize
            }
        }

    }

}