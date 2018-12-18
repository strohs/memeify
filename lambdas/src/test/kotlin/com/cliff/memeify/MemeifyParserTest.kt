package com.cliff.memeify

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.verifyAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * @author Cliff
 */
internal class MemeifyParserTest {

    // map of form parameters
    val paramsMap = mutableMapOf("botText" to "blah", "topText" to "blah")
    // map of filename to ByteArray
    val filesMap = mutableMapOf("filename.jpg" to ByteArray(1))

    // dummy testEvent
    val testEvent = APIGatewayProxyRequestEvent()
            .withHeaders(mutableMapOf("content-type" to "multipart/form-data; boundary=032a1ab685934650abbe059cb45d6ff3"))
            .withHttpMethod("POST")
            .withBody("asdjkdffgldfgsdfvbdggrg")
            .withIsBase64Encoded(true)

    @Test
    fun `should return FormData when valid data sent to MemeifyParser`() {
        val expectedFormData = FormData(paramsMap, filesMap)
        mockkObject(MemeifyParser)
        mockkConstructor(MultipartFormParser::class)
        every { anyConstructed<MultipartFormParser>().parse() } returns Pair( paramsMap, filesMap )

        val formData = MemeifyParser.parse(testEvent)

        assertEquals(expectedFormData, formData)
        verifyAll {
            MemeifyParser.parse( testEvent )
            MemeifyParser.validFormParameters( paramsMap, filesMap )
        }
    }

    @Test
    fun `should throw IllegalArgumentException when boundary is missing`() {
        // remove boundary string from content-type header
        testEvent.headers = mutableMapOf("content-type" to "multipart/form-data")
        assertThrows(IllegalArgumentException::class.java) {
            MemeifyParser.parse( testEvent )
        }
    }

    @Test
    fun `should throw IllegalArgumentException if topText parameter is missing`() {
        val missingParam = mutableMapOf("botText" to "blah")
        mockkConstructor(MultipartFormParser::class)
        every { anyConstructed<MultipartFormParser>().parse() } returns Pair( missingParam, filesMap )

        assertThrows(IllegalArgumentException::class.java) {
            MemeifyParser.parse( testEvent )
        }
    }

    @Test
    fun `should throw IllegalArgumentException if botText parameter is missing`() {
        val missingParam = mutableMapOf("topText" to "blah")
        mockkConstructor(MultipartFormParser::class)
        every { anyConstructed<MultipartFormParser>().parse() } returns Pair( missingParam, filesMap)

        assertThrows(IllegalArgumentException::class.java) {
            MemeifyParser.parse( testEvent )
        }
    }

    @Test
    fun `should throw IllegalArgumentException if an image was not sent in the body`() {
        val missingFile = mutableMapOf<String,ByteArray>()
        mockkConstructor(MultipartFormParser::class)
        every { anyConstructed<MultipartFormParser>().parse() } returns Pair(paramsMap, missingFile)

        assertThrows(IllegalArgumentException::class.java) {
            MemeifyParser.parse( testEvent )
        }
    }

    @Test
    fun `should throw IllegalArgumentException if more than one image was sent in the body`() {
        val twoFiles = mutableMapOf<String,ByteArray>("file1.jpg" to ByteArray(1), "file2.jpg" to ByteArray(1))
        mockkConstructor(MultipartFormParser::class)
        every { anyConstructed<MultipartFormParser>().parse() } returns Pair(paramsMap, twoFiles)

        assertThrows(IllegalArgumentException::class.java) {
            MemeifyParser.parse( testEvent )
        }
    }

    @Test
    fun `should throw IllegalArgumentException if image is not jpg or png`() {
        val badExtension = mutableMapOf("filename.gif" to ByteArray(1))
        mockkConstructor(MultipartFormParser::class)
        every { anyConstructed<MultipartFormParser>().parse() } returns Pair(paramsMap, badExtension)

        assertThrows(IllegalArgumentException::class.java) {
            MemeifyParser.parse( testEvent )
        }
    }
}