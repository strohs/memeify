package com.cliff.memeify

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Test memeify image manipulation
 *
 * @author Cliff
 */
internal class MemeifyTest {


    @Test
    fun `should return fontsize of 20 for 800w x 400h image`() {
        assertEquals( 20, Memeify.computeFontSize(800, 400))
    }

    @Test
    fun `should return fontsize of 20 for 400w x 800h image`() {
        assertEquals( 20, Memeify.computeFontSize(400, 800))
    }

    @Test
    fun `xStartPos should be 25 when image width is 800 and stringWidth is 750`() {
        assertEquals( 25, Memeify.xStartPos(750, 800))
    }

    @Test
    fun `should not split string into two lines when stringWidth LT imageWidth`() {
        val imageWidth = 800
        val stringWidth = 700
        val text = "this is a sample line of text that should not be split"
        val lines = Memeify.fitLineToWidth(text, stringWidth, imageWidth)
        assertEquals(1, lines.size)
    }

    @Test
    fun `should split string into two lines when stringWidth GT imageWidth`() {
        val imageWidth = 700
        val stringWidth = 800
        val text = "this is a sample line of text that exceeds image width"
        val lines = Memeify.fitLineToWidth(text, stringWidth, imageWidth)
        assertEquals(2, lines.size)
    }

    @Test
    fun `first line should contain 80 percent of words`() {
        val origLine = "this is the first line of text to be split into two lines this is second line"
        val lines = Memeify.fitLineToWidth( origLine, 490, 400)
        val firstLine = "this is the first line of text to be split into two lines"
        assertEquals( firstLine, lines[0])
    }

    @Test
    fun `second line should contain 20 percent of words`() {
        val origLine = "this is the first line of text to be split into two lines this is second line"
        val lines = Memeify.fitLineToWidth( origLine, 490, 400)
        val secondLine = "this is second line"
        assertEquals( secondLine, lines[1])
    }
}