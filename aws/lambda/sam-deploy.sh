#!/usr/bin/env bash
sam deploy \
   --template-file serverless-output.yaml \
   --stack-name MemeifyLambdaStack \
   --capabilities CAPABILITY_IAM