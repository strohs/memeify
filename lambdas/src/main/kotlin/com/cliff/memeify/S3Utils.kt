package com.cliff.memeify

import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse

/**
 * Utilities for working with S3
 * @author Cliff
 */
class S3Utils(val s3Client: S3Client) {


    /**
     * put an object into the specified S3 bucket
     */
    fun putObject(bucket: String, key: String, data: ByteArray): PutObjectResponse {
        val putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()
        return s3Client.putObject(putReq, RequestBody.fromBytes(data))
    }

    /**
     * build a S3 "path" style URL to a key within S3, this method requires "GetBucketLocation" permission in order to
     * function properly
     */
    fun buildS3PathUrl(bucket: String, key: String): String {
        val locationReq = GetBucketLocationRequest.builder().bucket(bucket).build()
        val locationResp = s3Client.getBucketLocation(locationReq)
        val region = locationResp.locationConstraintAsString()
        // if location == null. bucket is US-EAST-1, else location will have a region name
        return if (region.isNullOrEmpty()) "https://s3.amazonaws.com/$bucket/$key" else "https://s3-$region.amazonaws.com/$bucket/$key"
    }

}