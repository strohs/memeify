#!/usr/bin/env bash
# invokes a lambda function, use invocation-type:  RequestResponse or Event
aws lambda invoke \
--invocation-type RequestResponse \
--function-name KotlinTest \
--region us-east-1 \
--payload request.txt \
outputfile.txt