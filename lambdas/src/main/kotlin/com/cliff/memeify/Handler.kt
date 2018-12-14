package com.cliff.memeify

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.cliff.memeify.dto.MemeifyResponse
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import java.io.File
import java.util.*
import kotlin.streams.asSequence

/**
 *
 *
 * NOTES:
 *  POSTed data must be multipart/form-data and contain the following:
 *    ONE image file (jpg or png)
 *      image min size no less than 400x300
 *      image max size no greater than 3840×2160
 *    a form field named "topText" containing the text to place on the top of the image
 *    a form field name "botText" containing the text to place on the bottom of the image
 * @author Cliff
 */


class Handler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    companion object {
        const val TOP_TEXT_KEY = "topText"  // name of the form field containing the text to place on top of the image
        const val BOT_TEXT_KEY = "botText"  // name of the form field containing the text to place on the bottom of the image

        val validFileTypes = setOf("jpg", "png") // currently supporting only jpg and png ....for now
    }

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
        try {
            // parse and validate the multipart/form-data parameters
            val mfp = parseMultipartFormBody(input)
            println("paramsMap size:${mfp.paramsMap.size}")
            println("filesMap size:${mfp.filesMap.size}")

            // get the filename of the image
            val filename = mfp.filesMap.keys.first()
            println("filename=$filename")

            // memeify the image
            val memeifiedBytes = Memeify.memeify(mfp.filesMap[filename]!!,
                    getFileExtension(filename),
                    mfp.paramsMap[TOP_TEXT_KEY]!!,
                    mfp.paramsMap[BOT_TEXT_KEY]!!)
            println("memeified bytes ${memeifiedBytes.size}")

            // save memeified file to S3
            val memeifiedFilename = "${randomString(10)}-$filename"
            putObject(outBucket, memeifiedFilename, memeifiedBytes)

            // build a 200 response containing location of memeified image in S3
            response = buildResponse(
                    200,
                    null,
                    mapper.writeValueAsString(MemeifyResponse(memeifiedFilename, null)))

        } catch (ex: IllegalArgumentException) {
            response = buildResponse(
                    400,
                    mutableMapOf("X-Amzn-ErrorType" to "IllegalArgumentException"),
                    mapper.writeValueAsString(MemeifyResponse(null, ex.message)))
        } catch (ex: Exception) {
            response = buildResponse(
                    500,
                    mutableMapOf("X-Amzn-ErrorType" to "RuntimeException"),
                    mapper.writeValueAsString(MemeifyResponse(null, ex.message)))
        }
        return response
    }

    /**
     * parses the multipart/form-input from the request body, verified required fields are present and returns
     * a MultipartFormParser (mfp), which can be used to retrieve the "simple" form fields and the "file"
     * data fields
     * The multipart data is stored in separate Maps as properties of the MultipartFormInput map:
     *    mfp.paramsMap contains the simple parameter fields (with the form field name as the key)
     *    mfp.filesMap contains the filename as the key and a ByteArray of the file data as the value
     *
     * Memeify expects the form input to contain three fields named as follows:
     *   topText = String that contains text to place on the top of the image
     *   botText = String that contains text to place on the bottom of the image
     *   image = ByteArray containing the actual file (either jpeg or png)
     * if these fields are not present an IllegalArgumentException will be thrown
     */
    fun parseMultipartFormBody(input: APIGatewayProxyRequestEvent): MultipartFormParser {
        // parse the content type and boundary information from the header
        val contentTypeHeader: String? = input.headers.get("content-type") ?: input.headers.get("Content-Type")
        val boundary = contentTypeHeader?.let { MultipartFormParser.parseBoundaryFromHeader(it) }
                ?: throw IllegalArgumentException("boundary string not found in content-type header, content-type=$contentTypeHeader")

        // API Gateway will Base64 encode the entire body so lets decode it here into a ByteArray
        val bodyBytes = Base64.getDecoder().decode(input.body)
        println("decoded request body size=${bodyBytes.size}")

        // use our customized apache commons file upload to parse the various multipart fields
        val mfp = MultipartFormParser(boundary, bodyBytes)

        // verify required fields are present
        if (!mfp.paramsMap.containsKey(TOP_TEXT_KEY)) throw IllegalArgumentException("topText field was not sent as part of the request")
        if (!mfp.paramsMap.containsKey(BOT_TEXT_KEY)) throw IllegalArgumentException("botText field was not sent as part of the request")
        val imageSet = mfp.filesMap.keys
        if (imageSet.size != 1) throw IllegalArgumentException("exactly one image should be sent in the request, ${imageSet.size} image(s) were found")
        // validate a valid file type was sent
        val fileExt = getFileExtension(mfp.filesMap.keys.first())
        if (!validFileTypes.contains(fileExt)) throw IllegalArgumentException("only .jpg and .png image are supported, posted image type was $fileExt")

        return mfp
    }


    /**
     * put an object into the specified S3 bucket
     */
    fun putObject(bucket: String, key: String, data: ByteArray): PutObjectResponse {
        val putReq = PutObjectRequest.builder().bucket(bucket).key(key).build()
        return s3Client.putObject(putReq, RequestBody.fromBytes(data))
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


    /**
     * build a S3 "path" style URL to a key within S3, this method requires "GetBucketLocation" permission in order to
     * function properly
     */
    fun buildS3PathUrl(bucket: String, key: String): String {
        val location = s3Client.getBucketLocation { builder: GetBucketLocationRequest.Builder? ->
            builder?.bucket(bucket)
        }
        return if (location != null) "https://s3.amazonaws.com/$bucket/$key" else "https://s3-$location.amazonaws.com/$bucket/$key"
    }


    // returns "" if extension is unknown
    fun getFileExtension(filename: String) = File(filename).extension

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