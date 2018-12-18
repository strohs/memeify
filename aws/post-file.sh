#!/usr/bin/env bash

# api gateway URL
URL=https://gfjrkqbzua.execute-api.us-east-1.amazonaws.com/Prod/memeify

# posts an image to an API Gateway endpoint using type multipart/form-data
curl -F 'topText=top line' \
-F 'botText=bottom line' \
-F "image=@../images/small.jpg;type=image/jpeg" \
--trace-ascii ./body-trace \
$URL
