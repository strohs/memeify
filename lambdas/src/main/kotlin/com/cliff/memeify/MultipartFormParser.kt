package com.cliff.memeify

import org.apache.commons.fileupload.FileItem
import org.apache.commons.fileupload.FileItemFactory
import org.apache.commons.fileupload.FileUpload
import org.apache.commons.fileupload.UploadContext
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import java.io.InputStream

/**
 * Uses Apache commons file to parse the BODY portion of a multipart/form-data
 *
 * The simple form fields will be in the paramsMap property, with the form field name as the key, and the value of
 * that form field as the value
 *
 * The file will be in the fileMap, with the filename as the key, and the value will contain a ByteArray of the file
 *
 * @author Cliff
 */
class MultipartFormParser(val boundary: String, val body: ByteArray): UploadContext {

    // holds the simple form fields
    val paramsMap = mutableMapOf<String,String>()
    // holds the filename as a Key and an input stream to the file as a value
    val filesMap = mutableMapOf<String, ByteArray>()

    init {
        // parse out form parameters
        val factory: FileItemFactory = DiskFileItemFactory()
        val upload = FileUpload(factory)
        val fileItems: List<FileItem> = upload.parseRequest(this)
        fileItems.forEach { fi ->
            // is the item a simple form field?
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
    }

    override fun getCharacterEncoding(): String {
        // ISO-8859-1 is the default encoding used by multipart/form-upload
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