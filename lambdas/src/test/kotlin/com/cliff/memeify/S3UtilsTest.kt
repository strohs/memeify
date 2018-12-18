package com.cliff.memeify


import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest
import software.amazon.awssdk.services.s3.model.GetBucketLocationResponse
import kotlin.test.assertEquals

/**
 * uses mockK for mocking
 *
 * @author Cliff
 */
internal class S3UtilsTest {

    val s3Client = mockk<S3Client>()

    val s3Utils = S3Utils(s3Client)


    @BeforeEach
    fun clear() = clearMocks(s3Client)


    @Test
    fun `should build s3 url for us-west-1 when location response is us-west-1`() {
        val bucket = "bucket"
        val key = "key"
        val region = "us-west-1"
        val getLocResp = GetBucketLocationResponse.builder().locationConstraint("us-west-1").build()
        val expectedUrl = "https://s3-$region.amazonaws.com/$bucket/$key"

        every { s3Client.getBucketLocation(any<GetBucketLocationRequest>()) } returns getLocResp

        val url = s3Utils.buildS3PathUrl(bucket, key)

        assertEquals(expectedUrl, url)
    }

    @Test
    fun `should build s3 url for us-east-1 when location response is us-east-1`() {
        val bucket = "bucket"
        val key = "key"
        val getLocResp = GetBucketLocationResponse.builder().locationConstraint("").build()
        val expectedUrl = "https://s3.amazonaws.com/$bucket/$key"

        every { s3Client.getBucketLocation(any<GetBucketLocationRequest>()) } returns getLocResp

        val url = s3Utils.buildS3PathUrl(bucket, key)

        assertEquals(expectedUrl, url)
    }


}