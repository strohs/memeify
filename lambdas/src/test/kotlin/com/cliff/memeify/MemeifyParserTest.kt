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
    fun `should return parsed FormData when valid testEvent data sent to MemeifyParser`() {
        val expectedFormData = FormData(paramsMap, filesMap)
        mockkObject(MemeifyParser)
        mockkConstructor(MultipartFormParser::class)
        every { anyConstructed<MultipartFormParser>().parse() } returns FormData( paramsMap, filesMap )

        val formData = MemeifyParser.parse(testEvent)

        assertEquals(expectedFormData, formData)
        verifyAll {
            MemeifyParser.parse( testEvent )
            MemeifyParser.validFormParameters( paramsMap )
            MemeifyParser.validFileParameter(filesMap)
        }
    }

    @Test
    fun `should throw IllegalArgumentException when boundary string is missing from header`() {
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
        every { anyConstructed<MultipartFormParser>().parse() } returns FormData( missingParam, filesMap )

        assertThrows(IllegalArgumentException::class.java) {
            MemeifyParser.parse( testEvent )
        }
    }

    @Test
    fun `should throw IllegalArgumentException if botText parameter is missing`() {
        val missingParam = mutableMapOf("topText" to "blah")
        mockkConstructor(MultipartFormParser::class)
        every { anyConstructed<MultipartFormParser>().parse() } returns FormData( missingParam, filesMap)

        assertThrows(IllegalArgumentException::class.java) {
            MemeifyParser.parse( testEvent )
        }
    }

    @Test
    fun `should throw IllegalArgumentException if an image was not sent in the body`() {
        val missingFile = mutableMapOf<String,ByteArray>()

        assertThrows(IllegalArgumentException::class.java) {
            MemeifyParser.validFileParameter(missingFile)
        }
    }

    @Test
    fun `should throw IllegalArgumentException if more than one image was sent in the body`() {
        val twoFiles = mutableMapOf<String,ByteArray>("file1.jpg" to ByteArray(1), "file2.jpg" to ByteArray(1))

        assertThrows(IllegalArgumentException::class.java) {
            MemeifyParser.validFileParameter(twoFiles)
        }
    }

    @Test
    fun `should return true if one file sent in the body`() {
        val oneFile = mutableMapOf<String,ByteArray>("file1.jpg" to ByteArray(1))

        val res = MemeifyParser.validFileParameter(oneFile)
        assertEquals( res, true )
    }

    @Test
    fun `should throw IllegalArgumentException if image is not jpg or png`() {
        val badExtension = mutableMapOf("filename.gif" to ByteArray(1))

        assertThrows(IllegalArgumentException::class.java) {
            MemeifyParser.validFileParameter(badExtension)
        }
    }
}