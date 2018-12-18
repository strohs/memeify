package com.cliff.memeify

/**
 * holds the form fields submitted to Memeify
 *
 * @author Cliff
 */
data class FormData(
        val paramsMap: Map<String,String>,   // contains the textual fields
        val filesMap: Map<String,ByteArray>  // contains the sumbitted filename and image bytes
)