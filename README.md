Memeify
================================================================================================================
Memeify is a AWS lambda function (written in Kotlin) that allows you to create memes by adding text to an 
image (jpeg or png). It is powered by a serverless backend that includes API gateway (to proxy incoming requests to the
lambda) and S3 to store the final "memeified" images. 


![grumpy-cat](https://github.com/strohs/memeify/blob/master/memeified-grumpy-cat.jpg)

## Overview
Memeify expects to receive an image file and two text strings as input, (POSTed as multipart/form-data.)
The two strings represent the text to place at the top and bottom of the image respectively. 
API Gateway uses Lambda Proxy Integration to pass the request body to the lambda as an `ApiGatewayProxyRequestEvent`. 
The lambda code parses the form-data from the event body, adds the text to the image, and writes the "memeified" image 
to an S3 bucket.
If all goes well, the lambda will return a JSON response containing a URL to the image in S3. 
    For example: 
    ```json
    { "imageUrl" : "https://s3.amazonaws.com/memeify-imageoutputbucket-AABBCC/VHERDZTFLS-grumpy-cat.jpg"}
    ```
    Or, if an error occurs, JSON containing the error message will be returned:
    ```json
    { "errorMsg" : "image size must be <= 1MB"}
    ```
   

## Building
This is a maven project consisting of two modules: `lambdas` and `frontend`. `Lambdas` contains the 
lambda code while `frontend` contains a sample web page that I used for playing around with vue.js and AWS
integration (see the frontend section at the end of this readme)

* to build the memeify lambda jar file, cd into the project root directory and run
    * `mvn clean package -pl lambdas`
        * the jar artifact will be built in `lambdas/target/memeify-lambdas-0.1.jar`
* OPTIONAL - to build everything including the frontend resources
    * from the project root directory run `mvn clean package`

#### Deploying to AWS
A cloudformation [template](aws/memeify.yaml) has been provided for creating the Memeify stack. It will create the
following resources:
    * the memeify lambda function, 
    * an API Gateway POST method
    * a S3 bucket for storing the memeified images
        * Note that **this S3 bucket will have public read access**

1. build the lambda .jar file (as described above)
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
Nothing fancy here, this is a basic serverless architecture. The image file to "memeify", plus the two text strings 
to add to the top and bottom of the image are POSTed (as multipart/form-data) to API Gateway.  API Gateway uses Lambda
Proxy Integration to pass the request body to the lambda as an `ApiGatewayProxyRequestEvent`. The lambda code 
parses the form-data from the event body, add the text to the image, and writes the "memeified" image to an S3 bucket.
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
The memeify lambda expects to receive three fields POSTed as multipart/form-data. The data must contain the following
field names and types:

| field name |           type          |    constraints   |
|:----------:|:-----------------------:|:----------------:|
| image      | image/jpeg OR image/png | image size <= 1 MB |
| topText    | String                  | <= 75 characters |
| botText    | String                  | <= 75 characters |


## Notes and Observations
This project started as a practice application to help me study for an AWS Developer Certification. The exam
featured many questions on AWS serverless, and building an actual application was the best way to learn. 

Kotlin was chosen as the implementation language as I had used it in the past and wanted to come up to speed on 
 how to use it within a Lambda. The Kotlin code uses Java's *BufferedImage*, *Font* and *Graphics2d* classes for all 
 image manipulation. This was relatively easy to do, as these libraries have been around for years and there are 
 plenty of examples on image manipulation on the internet. Overall, Kotlin was a joy to work with, the Java integration was 
 flawless, and I hope to work with it again.

Getting image data from API Gateway into lambda, without API Gateway munging the data, was a little more of 
an adventure. After reading dozens of forum posts, my understanding is that API-Gateway did not support 
multipart/form-data until late 2017. Before that time, you could not POST form data to API-GW and I am not
sure how developers worked around this (if it all).  Fortunately, support for multipart/form-data was added in late 
2017 but you have to manually configure support for it within API Gateway 
[settings](https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-payload-encodings.html). Basically,
you tell API Gateway to treat multipart/form-data as binary data and it will automatically BASE64 encode the entire
multipart/form-data body. This body is then attached to the `ApiGatewayProxyEvent` and sent to your lambda function. 
This process keeps any binary image data from being corrupted, but it increases the request size by about 33%. 
An alternate approach, and one that avoids using API Gateway, would be to post new images directly into a S3 bucket 
and have your lambda trigger off of new bucket PUT events. I did not take this approach because I wanted to explore 
using API Gateway with Lambda proxy integration.  

Another observation is that my current implementation of memeify is a memory hog, as all processing is done in memory.
For example:
The incoming JSON event data is de-serialized in memory, then the entire request body is extracted from the JSON event 
 and BASE64 decoded into a ByteArray. That ByteArray (containing the actual multipart/form-data) is then also parsed 
 in memory, and finally the actual image data is extracted into another ByteArray and manipulated in memory. 
 
In this particular application, memory usage was not a major concern, but if it is for you, one alternative 
option would be to start writing data to the lambdas `/tmp` storage (max size of 512MB) 
and then work with your data from there, trading some processing speed for reduced memory usage. On the flip side, you 
might want your lambda functions to run as fast as possible, as it may 
[ultimately be cheaper](https://medium.com/@jconning/aws-lambda-faster-is-cheaper-6bf32f58d741) to raise your lambda's
 memory limit in order to run faster.

## Frontend Notes
The frontend module contains a vue.js/npm application for submitting images to memeify. It's basically just an HTML form
 that submits data to API Gateway endpoint. I may eventually use it as a starting point for a fictitious, 
 "meme creating" web-site. 

If you have experience with node/npm then you can start the front-end in development mode using the following steps:
    * deploy the memeify stack onto AWS
    * configure the API gateway URL of the memeify endpoint into the [.env file](frontend/.env)
    * cd into the `frontend` directory and type the following command
        * `npm run serve`
        * open the development server URL in your browser