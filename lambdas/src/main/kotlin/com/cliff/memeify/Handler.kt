package com.cliff.memeify

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.amazonaws.services.lambda.runtime.events.S3Event
import com.cliff.memeify.dto.MemeifyResponse
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.util.*
import kotlin.streams.asSequence

/**
 * This lambda will add text to an image file and then save it in a S3 Bucket. It uses the standard Java graphics
 * libraries (Graphics2D, Font, BufferedImage) to do the "memeifying". All image processing
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


class Handler : RequestHandler<S3Event, Unit> {

    // bucket where the memeified files will be written
    val outBucket = System.getenv("OUTPUT_BUCKET_NAME")
    val inBucket = System.getenv("INPUT_BUCKET_NAME")

    val s3Client = S3Client.builder().build()


    override fun handleRequest(event: S3Event, context: Context) {
        val s3Utils = S3Utils(s3Client)

        try {
            // DEBUG: save request to s3
            logEvent(event)
            println(event.toJson())

            //get bucket name and object that was created in the bucket
            val bucket = event.records[0].s3.bucket
            val obj = event.records[0].s3.`object`

            //read the object into a ByteArray
            val image: ByteArray = s3Utils.getObject(bucket.name, obj.key)

            // memeify the image
            val memeifiedImage = Memeify.memeify(image,
                    File(obj.key).extension,
                    "top text here",
                    "bottom text here")

            // save memeified file, with a random filename prefix, to S3
            val savedFilename = "${randomString(10)}-${obj.key}"
            s3Utils.putObject(outBucket, savedFilename, memeifiedImage)

        } catch (ex: Exception) {
            println("an exception occured: $ex")
        }
        return
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
         * util function to log incoming events
         */
        fun logEvent (event: S3Event) {
            val bucket = event.records[0]?.s3?.bucket?.name
            val obj = event.records[0]?.s3?.`object`?.key
            val objSize = event.records[0]?.s3?.`object`?.sizeAsLong
            println("new put bucket:$bucket object:$obj objSize:$objSize ")
        }

    }
}