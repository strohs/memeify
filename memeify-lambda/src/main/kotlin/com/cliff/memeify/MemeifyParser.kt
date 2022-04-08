package com.cliff.memeify

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import java.io.File
import java.util.*

/**
 * uses MultipartFormParser class to parse multipart/form-data and perform some hooks on the submitted parameters
 *
 * @author Cliff
 */
object MemeifyParser {

    // currently supporting only jpg and png ....for now
    val validFileTypes = setOf("jpg", "png")
    // character limit for each line of text in an image
    const val MAX_CHARS_PER_LINE = 75

    /**
     * extracts the multipart/form-input from the request body, verifies required fields are present
     *
     * Memeify expects the form input to contain three fields named as follows:
     *   topText = String that contains text to place on the top of the image
     *   botText = String that contains text to place on the bottom of the image
     *   image = ByteArray containing the actual file (either jpeg or png)
     *
     * @returns MultipartFormParser that exposes the fields submitted as MultiPart form-data:
     * The multipart data is stored in separate Maps as properties of the MultipartFormInput map:
     *    mfp.paramsMap contains the simple parameter fields (with the form field name as the key)
     *    mfp.filesMap contains the filename as the key and a ByteArray of the file data as the value
     *
     *
     * @throws IllegalArgumentException if a required field is missing
     */
    fun parse(input: APIGatewayProxyRequestEvent): FormData {
        // parse the content type and boundary information from the header
        val contentTypeHeader: String? = input.headers.get("content-type") ?: input.headers.get("Content-Type")
        val boundary = contentTypeHeader?.let { MultipartFormParser.parseBoundaryFromHeader(it) }
                ?: throw IllegalArgumentException("boundary string not found in content-type header, content-type=$contentTypeHeader")

        // API Gateway will Base64 encode the entire body, decode it here into a ByteArray
        val bodyBytes = Base64.getDecoder().decode(input.body)
        //println("decoded request:\n${bodyBytes.toString(Charsets.ISO_8859_1)}")

        // use our customized apache commons file upload to parse the fields from the body into Maps
        val formData = MultipartFormParser(boundary, bodyBytes).parse()

        // verify required fields are present
        validFormParameters(formData.paramsMap)
        validFileParameter(formData.filesMap)

        return formData
    }


    /**
     *
     * validates that required form fields were sent in the request
     * @returns true if submitted form fields are valid, else throws an IllegalArgumentException
     */
    fun validFormParameters(paramsMap: Map<String, String> ): Boolean = when {
        Handler.TOP_TEXT_KEY !in paramsMap -> throw IllegalArgumentException("topText field was not sent as part of the request")
        Handler.BOT_TEXT_KEY !in paramsMap -> throw IllegalArgumentException("botText field was not sent as part of the request")
        paramsMap[Handler.TOP_TEXT_KEY]?.length!! > MAX_CHARS_PER_LINE -> throw java.lang.IllegalArgumentException("topText field cannot exceed $MAX_CHARS_PER_LINE characters")
        paramsMap[Handler.BOT_TEXT_KEY]?.length!! > MAX_CHARS_PER_LINE -> throw java.lang.IllegalArgumentException("botText field cannot exceed $MAX_CHARS_PER_LINE characters")
        else -> true
    }

    /**
     * validate that one image file was sent and that it is a .jpg or .png file
     */
    fun validFileParameter(filesMap: Map<String, ByteArray>): Boolean = when {
        filesMap.keys.size != 1 ->
            throw IllegalArgumentException("exactly one image should be sent in the request, ${filesMap.keys.size} image(s) were found")
        // make sure a valid file type was sent TODO need to check file header instead of extension
        File(filesMap.keys.first()).extension !in validFileTypes ->
            throw IllegalArgumentException("only .jpg and .png image are supported, posted image type was ${File(filesMap.keys.first()).extension}")
        else -> true
    }
}