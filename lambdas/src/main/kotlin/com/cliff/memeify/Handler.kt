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
 * This lambda will add text to an image file and then save it in a S3 Bucket. It uses the standard Java graphics
 * libraries (Graphics2D, Font, BufferedImage) to do the "memeifying". To keep things simple, all image processing
 * is done IN MEMORY, so the size of the image you can process will vary with the size of memory allocated to this
 * lambda. Using a 256MB lambda, I've tested with images <= 1MB in size.
 *
 * The image file and associated text is POSTed to API Gateway and then sent to this Lambda via Lambda Proxy Integration.
 * Since the request will contain binary data (an image), it is expected that the Request Body will be BASE64
 * encoded. The use of BASE64 encoding will increase the Body size by about 33%, which can limit the
 * size of images you send. In addition, API Gateway imposes a 10MB limit on request sizes
 *
 * @params
 *  An ApiGatewayProxyEvent is expected as input and its "body" field must contain multipart/form-data (BASE64 encoded)
 *  containing the following fields:
 *    - One image file named "image", in addition, the filename must also be sent with either a jpg or png extension
 *    - a form field named "topText" containing the text to place on the top of the image, <= 75 chars
 *    - a form field name "botText" containing the text to place on the bottom of the image, <= 75 chars
 *
 * @returns an APIGatewayResponseEvent with the "body" field containing the following JSON string:
 *   { "imageUrl" : "https://s3-URL-TO-IMAGE-WILL-BE-HERE" }
 * OR should an error occur, a 400 status code will be returned along with a JSON object in the following format:
 *   { "errorMsg" : "human readable error message" }
 *
 * Environment Variables:
 *  the following two environment variables should be set for this lambda:
 *    OUTPUT_BUCKET_NAME = name of the S3 bucket where image files will be written
 *    MAX_BODY_SIZE_MB = max size (in megabytes) allowed for the HTTP request body sent to this lambda.
 *
 */


class Handler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // bucket where the memeified files will be written
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
            val formData = MemeifyParser.parse(input)
            println("paramsMap size:${formData.paramsMap.size}")
            println("filesMap size:${formData.filesMap.size}")

            // get the filename of the image
            val filename = formData.filesMap.keys.first()
            println("filename=$filename")

            // memeify the image
            val memeifiedBytes = Memeify.memeify(formData.filesMap[filename]!!,
                    File(filename).extension,
                    formData.paramsMap[TOP_TEXT_KEY]!!,
                    formData.paramsMap[BOT_TEXT_KEY]!!)
            println("memeified bytes ${memeifiedBytes.size}")

            // save memeified file, with a random filename prefix, to S3
            val savedFilename = "${randomString(10)}-$filename"
            s3Utils.putObject(outBucket, savedFilename, memeifiedBytes)

            // build a 200 response containing location of memeified image in S3
            response = buildResponse(
                    200,
                    mutableMapOf(),
                    mapper.writeValueAsString( MemeifyResponse( s3Utils.buildS3PathUrl(outBucket,savedFilename), null)))

        } catch (ex: IllegalArgumentException) {
            println(ex)
            response = buildResponse(
                    400,
                    mutableMapOf("X-Amzn-ErrorType" to "IllegalArgumentException"),
                    mapper.writeValueAsString(MemeifyResponse(null, ex.message)))
        } catch (ex: Exception) {
            println(ex)
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
    fun buildResponse(code: Int, headers: MutableMap<String, String>, body: String): APIGatewayProxyResponseEvent {
        // required for axios to accept responses from API Gateway
        headers["Access-Control-Allow-Origin"] = "*"
        return APIGatewayProxyResponseEvent()
                .withStatusCode(code)
                .withHeaders(headers)
                .withBody(body)
    }



    companion object {
        const val TOP_TEXT_KEY = "topText"  // name of the form field containing text to place on top of the image
        const val BOT_TEXT_KEY = "botText"  // name of the form field containing text to place on the bottom of the image

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