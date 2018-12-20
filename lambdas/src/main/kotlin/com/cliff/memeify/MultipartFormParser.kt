package com.cliff.memeify

import org.apache.commons.fileupload.FileItem
import org.apache.commons.fileupload.FileUpload
import org.apache.commons.fileupload.UploadContext
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import java.io.InputStream

/**
 * Uses Apache commons fileupload to parse the BODY portion of a multipart/form-data into two separate maps
 *
 * @author Cliff
 */
class MultipartFormParser(private val boundary: String, private val body: ByteArray): UploadContext {


    /**
     * parse the submitted form fields into a Pair of Maps, the first map of the pair contains the "simple" form fields
     * and the second contains the "file" data
     */
    fun parse(): Pair<Map<String,String>, Map<String,ByteArray>> {
        // map of form field name to form field value
        val paramsMap = mutableMapOf<String,String>()
        // map of image filename to ByteArray of the image
        val filesMap = mutableMapOf<String, ByteArray>()

        // parse form parameters from the body
        val factory = DiskFileItemFactory()
        val upload = FileUpload(factory)
        // Set overall request size constraint and convert to bytes
        upload.sizeMax = System.getenv("MAX_BODY_SIZE_MB").toLong() * 1024 * 1024
        val fileItems: List<FileItem> = upload.parseRequest(this)
        fileItems.forEach { fi ->
            // is the FileItem a simple form field?
            if ( fi.isFormField ) {
                paramsMap.put( fi.fieldName, fi.string )
            } else {
                filesMap.put( fi.name, fi.get() )
                println("---> type:${fi.contentType}")
                println("---> size:${fi.size}")
                println("---> name:${fi.name}")
                println("---> mem?:${fi.isInMemory}")
            }
        }
        return Pair (paramsMap, filesMap)
    }

    override fun getCharacterEncoding(): String {
        // ISO-8859-1 is used by multipart/form-upload
        return "ISO-8859-1"
    }

    override fun contentLength(): Long {
        return body.size.toLong()
    }

    override fun getContentLength(): Int {
        return -1
    }

    override fun getContentType(): String {
        return "multipart/form-data, boundary=$boundary"
    }

    override fun getInputStream(): InputStream {
        return body.inputStream()
    }

    companion object {
        val boundaryHeaderPat = Regex("boundary=(.+)")

        /**
         * parse the multipart boundary string a multipart-form content-type header
         */
        fun parseBoundaryFromHeader (contentTypeHeader: String): String? {
            return boundaryHeaderPat.find(contentTypeHeader)?.groupValues?.get(1)
        }
    }
}