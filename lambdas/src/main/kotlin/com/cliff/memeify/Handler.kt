package com.cliff.memeify

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.cliff.memeify.dto.MemeifyResponse
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import software.amazon.awssdk.services.s3.S3Client
import java.io.File
import java.util.*
import kotlin.streams.asSequence

/**
 *
 *
 * NOTES:
 *  POSTed data must be multipart/form-data and contain the following:
 *    ONE image file section, either (jpg or png)
 *    a form field named "topText" containing the text to place on the top of the image
 *    a form field name "botText" containing the text to place on the bottom of the image
 *
 * The total size of the HTTP Request, should not exceed 5MB, API Gateway may reject the request with a
 * @author Cliff
 */


class Handler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // bucket were the memeified files will be written
    val outBucket = System.getenv("OUTPUT_BUCKET_NAME")

    // jackson mapper for serializing/deserializing JSON
    val mapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL) //do not serialize NULL fields

    val s3Client = S3Client.builder().build()


    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        var response: APIGatewayProxyResponseEvent
        val s3Utils = S3Utils(s3Client)

        try {
            // parse and validate the multipart/form-data parameters
            val mfp = MemeifyParser.parseMultipartFormBody(input)
            println("paramsMap size:${mfp.paramsMap.size}")
            println("filesMap size:${mfp.filesMap.size}")

            // get the filename of the image
            val filename = mfp.filesMap.keys.first()
            println("filename=$filename")

            // memeify the image
            val memeifiedBytes = Memeify.memeify(mfp.filesMap[filename]!!,
                    File(filename).extension,
                    mfp.paramsMap[TOP_TEXT_KEY]!!,
                    mfp.paramsMap[BOT_TEXT_KEY]!!)
            println("memeified bytes ${memeifiedBytes.size}")

            // save memeified file, with a random filename prefix, to S3
            val savedFilename = "${randomString(10)}-$filename"
            s3Utils.putObject(outBucket, savedFilename, memeifiedBytes)

            // build a 200 response containing location of memeified image in S3
            response = buildResponse(
                    200,
                    null,
                    mapper.writeValueAsString( MemeifyResponse( s3Utils.buildS3PathUrl(outBucket,savedFilename), null)))

        } catch (ex: IllegalArgumentException) {
            response = buildResponse(
                    400,
                    mutableMapOf("X-Amzn-ErrorType" to "IllegalArgumentException"),
                    mapper.writeValueAsString(MemeifyResponse(null, ex.message)))
        } catch (ex: Exception) {
            response = buildResponse(
                    400,
                    mutableMapOf("X-Amzn-ErrorType" to "Exception"),
                    mapper.writeValueAsString(MemeifyResponse(null, ex.message)))
        }
        return response
    }



    /**
     * build an APIGatewayProxyResponse object with (optional) headers
     */
    fun buildResponse(code: Int, headers: MutableMap<String, String>?, body: String): APIGatewayProxyResponseEvent {
        return APIGatewayProxyResponseEvent()
                .withStatusCode(code)
                .withHeaders(headers)
                .withBody(body)
    }



    companion object {
        const val TOP_TEXT_KEY = "topText"  // name of the form field containing the text to place on top of the image
        const val BOT_TEXT_KEY = "botText"  // name of the form field containing the text to place on the bottom of the image

        // generate a random string of length characters
        fun randomString(length: Int): String {
            val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            return Random().ints(length.toLong(), 0, source.length)
                    .asSequence()
                    .map(source::get)
                    .joinToString("")
        }

        /**
         * util function to log incoming request details
         */
        fun logIncomingRequest(input: APIGatewayProxyRequestEvent) {
            val contentTypeHeader: String? = input.headers.get("content-type")
            val body: String = input.body
            println("----> content-type: ${contentTypeHeader}")
            println("----> base64encoded? ${input.isBase64Encoded}")
            println("----> headers")
            input.headers.forEach { k, v -> println("         $k:::$v") }
            println("----> orig body: ${body}")
        }
    }
}