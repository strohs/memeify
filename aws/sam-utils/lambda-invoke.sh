#!/usr/bin/env bash
# invokes a lambda function, use invocation-type:  RequestResponse for synchronous or Event for asynchronous
aws lambda invoke \
--invocation-type RequestResponse \
--function-name SomeLambda \
--region us-east-1 \
--payload request.txt \
outputfile.txt