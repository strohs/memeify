package com.cliff.memeify

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

/**
 * @author Cliff
 */
internal class BasicMultipartParserTest {

    private val body = "--------------------------aec2c36c2349b791\r\nContent-Disposition: form-data; name=\"image\"; filename=\"laptop.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n����\r\n--------------------------aec2c36c2349b791--"
    private val boundary = "--------------------------aec2c36c2349b791"
    private val contentDisposition = "Content-Disposition: form-data; name=\"image\"; filename=\"laptop.jpg\""
    private val contentTypeHeader = "multipart/form-data; boundary=------------------------aec2c36c2349b791"


    @Test
    fun `should return body string between the boundary strings`() {
        val expectedBody = "Content-Disposition: form-data; name=\"image\"; filename=\"laptop.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n����\r\n"
        val bodyStr = BasicMultipartParser.parseWithinBoundary( boundary, body)
        assertEquals( expectedBody, bodyStr)
    }

    @Test
    fun `should return boundary String from content-type header`() {
        val expectedBoundary = "------------------------aec2c36c2349b791"
        assertEquals(expectedBoundary, BasicMultipartParser.parseBoundary(contentTypeHeader))
    }

    @Test
    fun `should parse all three content disposition fields to a Map`() {
        val map = BasicMultipartParser.parseContentDispositionToMap(body)
        assertTrue( map.containsKey("type"))
        assertEquals( "form-data", map["type"])
        assertTrue( map.containsKey("name"))
        assertEquals( "image", map["name"])
        assertTrue( map.containsKey("filename"))
        assertEquals("laptop.jpg", map["filename"])
    }

    @Test
    fun `should return null if content disposition name field is missing from body`() {
        val missingBody = "--------------------------aec2c36c2349b791\r\nContent-Disposition: form-data; filename=\"laptop.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n����\r\n--------------------------aec2c36c2349b791--"
        val map = BasicMultipartParser.parseContentDispositionToMap(missingBody)
        assertTrue( map.containsKey("name"))
        assertNull( map["name"])
    }

    @Test
    fun `should return content-type of image-jpeg`() {
        assertEquals( "image/jpeg", BasicMultipartParser.parseContentType(body))
    }

    @Test
    fun `should parse content to ByteArray with size 12 bytes`() {
        val bytes = BasicMultipartParser.parseContentToBytes(boundary,body)
        assertNotNull(bytes)
        assertEquals(12, bytes?.size)
    }
}