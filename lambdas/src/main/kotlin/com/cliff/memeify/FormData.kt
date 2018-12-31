package com.cliff.memeify

/**
 * holds the submitted form fields submitted to Memeify as a pair of maps
 *
 * @author Cliff
 */
data class FormData(
        val paramsMap: Map<String,String>,   // contains the textual fields
        val filesMap: Map<String,ByteArray>  // contains the sumbitted filename and image bytes
)