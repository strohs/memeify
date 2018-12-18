package com.cliff.memeify

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.mockk.*
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.lang.IllegalArgumentException

/**
 * @author Cliff
 */
internal class MemeifyParserTest {

    // dummy testEvent
    val testEvent = APIGatewayProxyRequestEvent()
            .withHeaders(mutableMapOf("content-type" to "multipart/form-data; boundary=032a1ab685934650abbe059cb45d6ff3"))
            .withHttpMethod("POST")
            .withBody("asdjkdffgldfgsdfvbdggrg")
            .withIsBase64Encoded(true)

    @Test
    fun `should throw IllegalArgumentException when boundary is missing`() {
        // remove boundary string from content-type header
        testEvent.headers = mutableMapOf("content-type" to "multipart/form-data")
        assertThrows(IllegalArgumentException::class.java) {
            MemeifyParser.parseMultipartFormBody( testEvent )
        }
    }

    @Test
    fun `should throw IllegalArgumentException if topText parameter is missing`() {
        mockkConstructor(MultipartFormParser::class)
        every { anyConstructed<MultipartFormParser>().paramsMap } returns mutableMapOf("botText" to "blah")

        assertThrows(IllegalArgumentException::class.java) {
            MemeifyParser.parseMultipartFormBody( testEvent )
        }
    }

    @Test
    fun `should throw IllegalArgumentException if botText parameter is missing`() {
        mockkConstructor(MultipartFormParser::class)
        every { anyConstructed<MultipartFormParser>().paramsMap } returns mutableMapOf("topText" to "blah")

        assertThrows(IllegalArgumentException::class.java) {
            MemeifyParser.parseMultipartFormBody( testEvent )
        }
    }

    @Test
    fun `should throw IllegalArgumentException if an image was not sent in the body`() {
        mockkConstructor(MultipartFormParser::class)
        every { anyConstructed<MultipartFormParser>().paramsMap } returns mutableMapOf("topText" to "blah", "botText" to "blah")
        every { anyConstructed<MultipartFormParser>().filesMap } returns mutableMapOf()

        assertThrows(IllegalArgumentException::class.java) {
            MemeifyParser.parseMultipartFormBody( testEvent )
        }
    }

    @Test
    fun `should throw IllegalArgumentException if more than one image was sent in the body`() {
        mockkConstructor(MultipartFormParser::class)
        every { anyConstructed<MultipartFormParser>().paramsMap } returns mutableMapOf("topText" to "blah", "botText" to "blah")
        every { anyConstructed<MultipartFormParser>().filesMap } returns mutableMapOf("file1.jpg" to ByteArray(1), "file2.png" to ByteArray(1))

        assertThrows(IllegalArgumentException::class.java) {
            MemeifyParser.parseMultipartFormBody( testEvent )
        }
    }

    @Test
    fun `should throw IllegalArgumentException if image is not jpg or png`() {
        mockkConstructor(MultipartFormParser::class)
        every { anyConstructed<MultipartFormParser>().paramsMap } returns mutableMapOf("topText" to "blah", "botText" to "blah")
        every { anyConstructed<MultipartFormParser>().filesMap } returns mutableMapOf("file1.gif" to ByteArray(1))

        assertThrows(IllegalArgumentException::class.java) {
            MemeifyParser.parseMultipartFormBody( testEvent )
        }
    }
}