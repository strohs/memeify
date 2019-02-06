package com.cliff.memeify.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @author Cliff
 */
data class CustomHeaders (
        @JsonProperty("X-Amzn-ErrorType")
        val errorType: String?
)