Memeify
================================================================================================================
Memeify is a AWS lambda function (written in Kotlin) that allows you to create simple memes by adding text to an 
image (jpeg or png). It is powered by a serverless backend that uses API gateway to proxy incoming requests 
to the lambda, and S3 to store the final "memeified" images.


![grumpy-cat](https://github.com/strohs/memeify/blob/master/memeified-grumpy-cat.jpg)

    

## Building
This is a multi-module maven project consisting of two modules: `lambdas` and `frontend`. `Lambdas` contains the 
lambda code and `frontend` contains an OPTIONAL sample web page I used for playing around with vue.js and AWS integration
(more info on this in the frontend section below)

* to build the memeify lambda jar file, cd into the project root directory and run
    * `mvn clean package -pl lambdas`
        * the jar artifact will be built in `lambdas/target/memeify-lambdas-0.1.jar`
* OPTIONAL - to build everything including the frontend resources
    * from the project root directory run `mvn clean package`

#### Deploying to AWS
A cloudformation [template](aws/memeify.yaml) has been provided for creating the Memeify stack. It will create the
following resources:
    * the memeify lambda function, 
    * a REST API consisting of a single POST method running on API Gateway
    * a S3 bucket for storing the memeified images
        * Note that **this S3 bucket will have public read access**

1. build the lambda function (as described above)
2. copy the lambda code to an S3 bucket
3. run the template in cloudformation and point it to the S3 bucket containing the lambda code

## Running
Once the stack is up, you can use the [provided curl script](aws/post-image.sh) to send data to memeify. The script
will send an image file and two text strings to memeify as multipart/form-data. You must configure the script with
the API Gateway URL to your lambda endpoint. You can find this in the outputs of the memeify stack or in the API
Gateway console. 
If successful, memeify will return a json response containing a link to the image in s3:
    ```{ "imageUrl" : "https://s3.amazonaws.com/memeify-imageoutputbucket-AABBCC/VHERDZTFLS-grumpy-cat.jpg"}``` 


## Memeify Architecture Flow
Nothing fancy here, this is a very basic serverless architecture. The image file to "memeify", plus the two text strings 
to add to the top and bottom of the image are POSTed as multipart/form-data to API Gateway.  API Gateway uses Lambda
Proxy Integration to pass the request body to the lambda as an `ApiGatewayProxyRequestEvent`. The lambda code will
parse the form-data from the event body, add the text to the image, and write the memeified image to an S3 bucket.
If all goes well, the lambda will return a JSON response containing a URL to the image in
 S3. For example: 
```json
{ "imageUrl" : "https://s3.amazonaws.com/memeify-imageoutputbucket-AABBCC/VHERDZTFLS-grumpy-cat.jpg"}
```
Or, if an error occurs, JSON containing the error message will be returned:
```json
{ "errorMsg" : "image size must be <= 1MB"}
```
    
### multipart/form-data structure
As mentioned above, the memeify lambda expects, as input, multipart/form-data. The data should contain the following
field names and types:

| field name |           type          |    constraints   |
|:----------:|:-----------------------:|:----------------:|
| image      | image/jpeg OR image/png | filesize <= 1 MB |
| topText    | String                  | <= 75 characters |
| botText    | String                  | <= 75 characters |


## Notes and Observations
The lambda code is written in Kotlin, and uses Java's *BufferedImage*, *Font* and *Graphics2d* classes for all 
image manipulation. This was relatively easy to do, as these libraries have been around for years and there are 
plenty of examples on how to add text to an image. 

Getting image data from API Gateway into lambda, without API Gateway munging the data, was a little more of 
an adventure. There are few examples of how to do this on the internet (or maybe I was googling the wrong 
keywords) and support for multipart/form-data was recently added to API-Gateway in late 2017.  
The gist of it ... is this:

API Gateway must be configured to treat `multipart/form-data` as a *Binary Media Type*. Once configured,
API-GW will detect multipart/form-data requests, automatically BASE64 encode the entire request body, attach
 it to the "body" field of the `ApiGatewayProxyEvent`, and then send the proxy event to your lambda. 
 This process keeps the image data from being munged, but increases the request size by about 33% (thanks base64 encoding).
 You should also be aware that API Gateway limits incoming request sizes to a max of 10MB, so trying to POST large 
 images would be impossible with current size limitations. An alternate approach, and one that avoids using API
 Gateway, would be to post new images directly into a S3 bucket and have your lambda trigger off of bucket PUT events.
 I did not take this approach because I wanted to explore using API Gateway with Lambda proxy integration.  

Another point to note is that my current code is a memory hog, as all processing is done in memory. For example:
 the incoming JSON event data is de-serialized in memory, then the request body is extracted from the JSON event 
 and BASE64 decoded into a ByteArray. That ByteArray (containing the multipart/form-data) is also parsed in memory, 
 and then the actual image data is extracted into another ByteArray and manipulated in memory. 
 As you can see memory usage adds up pretty quickly.  If this gets to be a concern, one option would 
 be to start writing data to the lambdas `/tmp` storage (max size of 512MB) and then work with data from disk, 
 trading processing speed for reduced memory usage. 


## Frontend Notes
The frontend module contains a vue.js/npm application for submitting images to memeify. It essentially just an HTML form
 that submits data to API Gateway. I may eventually use it as a starting point for a fictitious web-site that 
 creates memes. If you have experience with these node/npm then you can start the front-end
in development mode using the following steps:
    * deploy the memeify lambda onto AWS
    * configure the API gateway URL of the memeify endpoint into the [.env file](frontend/.env)
    * cd into the `frontend` directory and type the following command
        * `npm run serve`
        * open the development server URL in your browser