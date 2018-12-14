package com.cliff.memeify

/**
 * THIS CLASS IS NO LONGER USED - apache commons fileupload has replaced it
 *
 * Utility class that parses the FIRST content "section" of a multipart/form-data upload. Any additional sections will be
 * ignored
 * Methods are provided to parse the various fields of the content, as well as methods to parse the actual content and
 * return it as a ByteArray or as a String
 *
 * @author Cliff
 */
object BasicMultipartParser {
    // example body as contained in APIGatewayProxyRequestEvent.getBody
    // --------------------------aec2c36c2349b791\r\nContent-Disposition: form-data; name=\"image\"; filename=\"laptop.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n����

    val boundaryHeaderPat = Regex("boundary=(.+)")

    // patterns for Content-Disposition
    val contentDispositionPat = Regex("(Content-Disposition: .+)\r\n")
    val contentDispositionTypePat = Regex("Content-Disposition: ([^;]+);")
    val contentDispositionNamePat = Regex(" name=\"(.+?)\"")
    val contentDispositionFilenamePat = Regex(" filename=\"(.+?)\"")

    val contentTypePat = Regex("Content-Type: ([^\r\n]+)")

    // parses the actual content of the form data
    val contentPat = Regex("\r\n\r\n(.+?)\r\n", RegexOption.DOT_MATCHES_ALL)

    private fun parseContentDisposition(body: String): String? {
        val result = contentDispositionPat.find(body)
        return result?.groupValues?.get(1)
    }

    private fun parseContentDispositionType (contentDisposition: String): String? {
        return contentDispositionTypePat.find(contentDisposition)?.groupValues?.get(1)
    }

    private fun parseContentDispositionName (contentDisposition: String): String? {
        return contentDispositionNamePat.find(contentDisposition)?.groupValues?.get(1)
    }

    private fun parseContentDispositionFilename (contentDisposition: String): String? {
        return contentDispositionFilenamePat.find(contentDisposition)?.groupValues?.get(1)
    }

    fun parseContentDispositionToMap (body: String): Map<String,String?> {
        var map = mapOf<String,String?>()
        val content = parseContentDisposition(body)
        if (!content.isNullOrEmpty()) {
            map = map.plus("type" to parseContentDispositionType(content))
            map = map.plus("name" to parseContentDispositionName(content))
            map = map.plus("filename" to parseContentDispositionFilename(content))
        }
        return map
    }

    fun parseWithinBoundary(boundary:String, body:String): String? {
        val pat = Regex("""$boundary\r\n(.+?)$boundary--""",RegexOption.DOT_MATCHES_ALL)
        return pat.find(body)?.groupValues?.get(1)
    }

    fun parseBoundary (contentTypeHeader: String): String? {
        return boundaryHeaderPat.find(contentTypeHeader)?.groupValues?.get(1)
    }

    fun parseContentType (body: String): String? {
        return contentTypePat.find(body)?.groupValues?.get(1)
    }

    fun parseContentToBytes (boundary: String, body:String): ByteArray? {
        val content = parseWithinBoundary(boundary, body)

        return if ( content != null ) {
            contentPat.find(content)?.groupValues?.get(1)?.toByteArray()
        } else {
            null
        }
    }

    fun parseContentToString (boundary: String, body:String): String? {
        val content = parseWithinBoundary(boundary, body)
        return if ( content != null ) {
            contentPat.find(content)?.groupValues?.get(1)
        } else {
            null
        }
    }
}