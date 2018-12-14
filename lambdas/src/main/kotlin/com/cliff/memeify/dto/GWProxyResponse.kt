package com.cliff.memeify.dto

/**
 *
 * @author Cliff
 */
data class GWProxyResponse(
        val isBase64Encoded: Boolean,
        val statusCode: Int,
        val headers: CustomHeaders?,
        val body: String
)