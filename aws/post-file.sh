#!/usr/bin/env bash

# api gateway URL
URL=https://gfjrkqbzua.execute-api.us-east-1.amazonaws.com/Prod/memeify

# posts an image to an API Gateway endpoint using type multipart/form-data
curl -v -F 'topText=when you try to code in kotlin but switch back to java' \
-F 'botText=why you no use clojure!!!' \
-F "image=@../images/small.jpg;type=image/jpeg" $URL
