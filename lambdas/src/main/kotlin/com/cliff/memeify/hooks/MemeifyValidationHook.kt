package com.cliff.memeify.hooks

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import java.io.InputStream
import java.io.OutputStream

/**
 * This is a lambda "hook" function that can be used with CodeDeploy to validate that the main Memeify
 * lambda is working
 *
 * Author: Cliff
 */
class MemeifyValidationHook: RequestStreamHandler {

    //todo need to pull example API Gateway Event string from s3 bucket


    override fun handleRequest(input: InputStream?, output: OutputStream?, context: Context?) {
        println("------------== in Memeify Validation Hook ==------------------------")
        val inStr = input?.let {
            it.bufferedReader().use { br -> br.readText() }
        }

        println("input to MemeifyValidationHook is:\n $inStr")

        output?.let {
            it.bufferedWriter().use { bw -> bw.write("some dummy string")}
        }

    }


}