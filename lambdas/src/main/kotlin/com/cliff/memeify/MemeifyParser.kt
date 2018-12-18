package com.cliff.memeify

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import java.io.File
import java.util.*

/**
 * uses MultipartFormParser to parse the form-data body that was submitted, and then performs some validation on
 * the submitted parameters
 *
 * @author Cliff
 */
object MemeifyParser {

    // currently supporting only jpg and png ....for now
    val validFileTypes = setOf("jpg", "png")

    /**
     * extracts the multipart/form-input from the request body, verifies required fields are present
     *
     * Memeify expects the form input to contain three fields named as follows:
     *   topText = String that contains text to place on the top of the image
     *   botText = String that contains text to place on the bottom of the image
     *   image = ByteArray containing the actual file (either jpeg or png)
     *
     * @returns MultipartFormParser that exposes the fields subitted as MultiPart form-data:
     * The multipart data is stored in separate Maps as properties of the MultipartFormInput map:
     *    mfp.paramsMap contains the simple parameter fields (with the form field name as the key)
     *    mfp.filesMap contains the filename as the key and a ByteArray of the file data as the value
     *
     *
     * @throws IllegalArgumentException if a required field is missing
     */
    fun parseMultipartFormBody(input: APIGatewayProxyRequestEvent): MultipartFormParser {
        // parse the content type and boundary information from the header
        val contentTypeHeader: String? = input.headers.get("content-type") ?: input.headers.get("Content-Type")
        val boundary = contentTypeHeader?.let { MultipartFormParser.parseBoundaryFromHeader(it) }
                ?: throw IllegalArgumentException("boundary string not found in content-type header, content-type=$contentTypeHeader")

        // API Gateway will Base64 encode the entire body so lets decode it here into a ByteArray
        val bodyBytes = Base64.getDecoder().decode(input.body)
        //println("decoded request:${bodyBytes.toString(Charsets.ISO_8859_1)}")

        // use our customized apache commons file upload to parse the fields from the body
        val mfp = MultipartFormParser(boundary, bodyBytes)

        // verify required fields are present
        if (!mfp.paramsMap.containsKey(Handler.TOP_TEXT_KEY)) throw IllegalArgumentException("topText field was not sent as part of the request")
        if (!mfp.paramsMap.containsKey(Handler.BOT_TEXT_KEY)) throw IllegalArgumentException("botText field was not sent as part of the request")
        val imageSet = mfp.filesMap.keys
        if (imageSet.size != 1) throw IllegalArgumentException("exactly one image should be sent in the request, ${imageSet.size} image(s) were found")
        // make sure a valid file type was sent
        val fileExt = File(mfp.filesMap.keys.first()).extension
        if (!validFileTypes.contains(fileExt)) throw IllegalArgumentException("only .jpg and .png image are supported, posted image type was $fileExt")

        return mfp
    }
}