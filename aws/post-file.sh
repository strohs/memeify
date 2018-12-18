#!/usr/bin/env bash

# api gateway URL
URL=https://afaat1feyg.execute-api.us-east-1.amazonaws.com/Stage/memeify

# posts an image to an API Gateway endpoint using type multipart/form-data
curl -v -F 'topText=when you try to code in kotlin but switch back to java' \
-F 'botText=why you no use clojure!!!' \
-F "image=@../images/neon-forest.jpg;type=image/jpeg" $URL
