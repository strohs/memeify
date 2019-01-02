#!/usr/bin/env bash

#
# use this to post an image file to your memeify endpoint. Memeify expects one image file, with a field name of
# 'image' and two text fields named 'topText' and 'botText'
#
# image file should be <= 1MB
# topText and botText should each be <= 75 characters
#

# api gateway URL
URL=https://URL-HERE.execute-api.us-east-1.amazonaws.com/Stage/memeify

# posts an image to an API Gateway endpoint using type multipart/form-data
curl -v -F 'topText=I tried to proxy multipart form data to a lambda once' \
-F 'botText=I was not amused' \
-F "image=@../images/grumpy-cat.jpg;type=image/jpeg" $URL
