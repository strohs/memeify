package com.cliff.memeify

import org.apache.commons.fileupload.FileItem
import org.apache.commons.fileupload.FileUpload
import org.apache.commons.fileupload.UploadContext
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import java.io.InputStream

/**
 * Implements Apache Commons FileUpload::UploadContext interface in order to parse the BODY portion of a
 * multipart/form-data into a two Maps.
 *
 * boundary - is the boundary string that marks the different sections of the multipart/form-data
 * body - is a ByteArray of the multipart form data that must be encoded as ASCII or (ISO-8859-1)
 *
 * @author Cliff
 */
class MultipartFormParser(private val boundary: String, private val body: ByteArray): UploadContext {


    /**
     * parse the submitted form fields into a pair of Maps.
     * The first map of the pair contains the "simple", textual, form fields.
     * The second map contains the "file" data, with the filename mapped to a ByteArray of the actual file data
     */
    fun parse(): FormData {
        // map of form field name to form field value
        val paramsMap = mutableMapOf<String,String>()
        // map of image filename to ByteArray of the image
        val filesMap = mutableMapOf<String, ByteArray>()

        // parse form parameters from the body
        val factory = DiskFileItemFactory()
        val upload = FileUpload(factory)
        // Set default request size constraint to 5MB
        val maxUploadSize:Long = System.getenv("MAX_BODY_SIZE_MB")?.toLong() ?: 5
        upload.sizeMax = maxUploadSize * 1024 * 1024 // convert megabytes to bytes
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
        return FormData(paramsMap, filesMap)
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