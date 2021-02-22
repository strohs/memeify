package com.cliff.memeify

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.awt.*
import java.awt.font.GlyphVector
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File


internal class GraphicsTest {

    val path = "../images/neon-forest.jpg"
    val imageFormat = "jpg"
    val topText = "this is a font test for the top text"
    val botText = "this is a font text for the bottom text"

    @Test
    @Disabled
    fun memeify() {
        val img: BufferedImage = Memeify.loadImage(path)
        val width = img.width
        val height = img.height
        println("image width: $width height: $height")

        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g2d: Graphics2D = bufferedImage.createGraphics()

        // compute font size relative to image dimensions
        val fontSize = Memeify.computeFontSize(width, height)

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        // build an outlined font
        val font: Font = Font("Arial", Font.BOLD, fontSize)
        val bigFont: Font = font.deriveFont( AffineTransform.getScaleInstance(1.2, 1.2))
        val gv: GlyphVector = bigFont.createGlyphVector( g2d.fontRenderContext, topText )



        //println("fontSize=$fontSize")
        //    g2d.font = Font(Font.SANS_SERIF, Font.BOLD, fontSize)


        // draw 2d graphics
        g2d.drawImage(img, 0, 0, null)

        g2d.translate( 25, 25)
        outlineAndFillFonts(g2d, gv)

//        g2d.transform(transform);
//        g2d.setColor(Color.black);
//        g2d.draw(outline);
//        g2d.setClip(outline);


        // determine if top line of text needs to be split, then draw the top text lines
        //println("top text width: ${g2d.fontMetrics.stringWidth(topText)}")
        //val topLines = Memeify.fitLineToWidth(topText, g2d.fontMetrics.stringWidth(topText), width)
        //Memeify.drawTopText(g2d, topLines, width, height)
        // draw bottom lines
        //println("bottom text width: ${g2d.fontMetrics.stringWidth(botText)}")
        //val botLines = Memeify.fitLineToWidth(botText, g2d.fontMetrics.stringWidth(botText), width)
        //Memeify.drawBottomText(g2d, botLines, width, height)

        g2d.dispose()

        val imageBytes = Memeify.toByteArray( bufferedImage, imageFormat )
        File("mytest.jpg").writeBytes(  imageBytes )

    }

    fun outlineAndFillFonts( g: Graphics2D,  gvs: GlyphVector ): List<Shape> {
        val shapes = mutableListOf<Shape>()

        g.stroke = BasicStroke(1.2f)
        for (i in 0 until gvs.numGlyphs) {
            val letterShape = gvs.getGlyphOutline(i)
            g.paint = Color.WHITE
            g.fill( letterShape )
            g.paint = Color.BLACK
            g.draw( letterShape )
            shapes.add( letterShape )
        }
        return shapes
    }
}