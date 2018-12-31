#!/usr/bin/env bash
sam package \
   --template-file ./memeify.yaml \
   --output-template-file ./serverless-output.yaml \
   --s3-bucket strohs-ci-cd