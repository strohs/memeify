#!/usr/bin/env bash
aws lambda create-function \
--region us-east-1 \
--function-name KotlinS3 \
--zip-file fileb://../../lambdas/target/memeify-lambdas-0.1.jar \
--role ROLE_ARN_HERE \
--handler com.cliff.memeify.Handler::handleRequest \
--runtime java8 \
--timeout 10 \
--memory-size 256
