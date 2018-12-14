#!/usr/bin/env bash

# api gateway URL
URL=https://gfjrkqbzua.execute-api.us-east-1.amazonaws.com/Prod/memeify

# posts an image to an API Gateway endpoint using type multipart/form-data
curl -v -F 'topText=this is the top text that might be waaaay toooo long' \
-F 'botText=this is the bottom text that is not tooo long' \
-F "image=@../images/neon-forest.jpg;type=image/jpeg" \
$URL
