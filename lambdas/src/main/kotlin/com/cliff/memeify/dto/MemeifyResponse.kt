package com.cliff.memeify.dto

/**
 * Data class representing a Memeify response, it can be serialized into JSON containing an image URL
 * or an error message
 *
 * @author Cliff
 */
data class MemeifyResponse(
        val imageUrl: String?,
        val errorMsg: String?
)