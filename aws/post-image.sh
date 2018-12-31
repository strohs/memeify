#!/usr/bin/env bash

#
# use this to post an image file to memeify
# image file should be <= 1MB
# topText and botText should each be <= 75 characters
#

# api gateway URL
URL=https://URL-HERE.execute-api.us-east-1.amazonaws.com/Stage/memeify

# posts an image to an API Gateway endpoint using type multipart/form-data
curl -v -F 'topText=this text goes to the top of the image' \
-F 'botText=while this text would go on the bottom of the image' \
-F "image=@../images/grumpy-cat.jpg;type=image/jpeg" $URL
