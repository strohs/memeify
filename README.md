Memeify
================================================================================================================
Memeify is a simple AWS lambda function (written in Kotlin) that allows you to create meme images by automating the 
process of adding text to an image (jpeg or png). It's an example project designed to showcase using AWS Lambda with
API Gateway and S3.

The mile-high view is as follows:
1. You submit an image along with text to be placed into the image, via an HTTP POST request, to an API Gateway endpoint
2. The memeify lambda function "bakes" the text into the image using the java 2d graphics API 
3. The resultant image is saved into an S3 bucket


![grumpy-cat](https://github.com/strohs/memeify/blob/master/memeified-grumpy-cat.jpg)


## Prerequisites
- Java 1.8 (OpenJDK 1.8 was used in this project)
- Apache Maven (I used maven 3.5, versions >= 3 might work)
- An AWS account along with experience with S3, CloudFormation. 
  Specifically, you will need to be familiar with creating/accessing S3 bucket(s) and be able to deploy CloudFormation 
  templates.
- API Gateway knowledge is nice to have, but not a strict requirement. You should be able to access the API Gateway
  management console to verify that the memeify endpoint was created



### Building the memeify-lambda.jar file
Memeify is a maven project consisting of two sub-modules: `memeify-lambda` and `frontend`. `memeify-lambda` contains 
the lambda hooks and image processing code, while `frontend` contains a sample web page that can be used to submit an 
image and text to memeify via an HTML form.

1. to build the memeify lambda jar file, cd into the project root directory and run
    - `mvn clean package -pl memeify-lambda`
    - the jar artifact will be built at `memeify-lambda/target/memeify-lambda-0.1.jar`



### Deploying to AWS
A cloudformation [template](aws/template.yaml) has been provided for creating the Memeify stack. It will create the
following resources:
* the memeify lambda function (configured with 256MB of memory)
* an API Gateway POST method
* a S3 bucket for storing the final memeified images
    * Note that **this S3 bucket will be given public read access**

Deploy Steps:
1. build the `memeify-lambda-0.1.jar` file (as described above)
2. create a temporary S3 bucket (or use an existing one) and copy the `memeify-lambda-0.1.jar` to it. 
3. use CloudFormation to create the memeify stack by uploading the provided [template](aws/template.yaml) to CloudFormation:
    - you will need to provide three parameters in the CloudFormation console:
        - the stack name, for example: "MyMemeifyStack"
        - the name of the S3 bucket (created in step 2) containing containing the `memeify-lambda-0.1.jar`
        - the actual name of the memeify .jar file. This will default to `memeify-lambda-0.1.jar` so you only need
          to change this if you renamed of the .jar file
    - once the above parameters are provided the stack will be ready for creation, and you can accept the rest of the
    cloudformation defaults.
    - If all went well, the stack will have created the following resources:
        - An API Gateway endpoint `/memeify`, that accepts POST requests
        - a public S3 bucket to hold the final memefied images, i.e.: "mymemifystack-imageoutputbucket-ab5334bnf" 
        - the memeify lambda function: i.e.: "MyMemeifyStack-MemeifyLambda-34GZG44"
    - The outputs section (within the CloudFormation management console) will contain the ApiUrl that can be used
      to submit images to memeify, for example: https://dfXDFA34asdf.execute-api.us-east-1.amazonaws.com/Stage/memeify
      

### Running
Once the stack is deployed, I've provided two ways to send images to memeify. You can either use curl to POST your
image and text, or use the example HTML page to submit your image via an HTML form.  Both approaches will require you
to edit the script (or html page) with the memeify API Gateway endpoint. This can be found in the outputs section of
the stack (within the cloudformation management console).
Please be aware that you should not submit images > 2 MegaBytes in size. This is because the memeify lambda is 
(by default) configured with 256 MB of memory and all image processing is done... in memory. If you wish to use larger 
images, you will need to increase the lambda's memory.


#### Option 1 - use the provided curl script
you can use the [provided curl script](aws/post-image.sh) to send data to your memeify endpoint. The script
will send an image file plus two text strings to memeify as multipart/form-data. Before running you must configure the 
script with your memeify API Gateway endpoint.
If successful, memeify will return a json response containing a link to the image in s3:

```json
{ "imageUrl" : "https://s3.amazonaws.com/memeify-imageoutputbucket-AABBCC/VHERDZTFLS-grumpy-cat.jpg"}
``` 

You should then be able to put this URL into a web-browser and view the image


#### Option 2 - use the provided HTTP Page
There is a sample [HTTP page](frontend/index.html) that can be used to submit your image and text via an HTML form. 
The page uses the javascript fetch API to submit data to the memeify API endpoint (as multipart form-data) and then
displays the final, "memeified", image within the page. Before using the page, you must edit its javascript code with
 your memeify API URL. Simply open the index.html page and search for the text: "TODO"


## Memeify Architecture Flow
Nothing too fancy here. Multipart/form-data is posted to API Gateway which will BASE64 encode the entire request 
body and send it to the memeify lambda as a `ApiGatewayProxyEvent`.  The memeify lambda takes the request body, 
BASE64 decodes it, and then parses the image data and text strings from the multipart form-data. The image is 
then "memeified" and written to a S3 bucket. Finally, a JSON response containing a URL to the "memeified" image 
is returned by the lambda.  
 
### input
The memeify lambda expects to receive three fields POSTed as multipart/form-data. The data must contain the following
field names and types:

| field name |           type          |    constraints   |
|:----------:|:-----------------------:|:----------------:|
| image      | image/jpeg OR image/png | for best results, no less than 400x300 and image size <= 2 MB (to process bigger images, increase lambda memory size) |
| topText    | String                  | <= 75 characters |
| botText    | String                  | <= 75 characters |


### output
If all goes well, the lambda will return a JSON response containing a URL to the image in
 S3. For example: 
```json
{ "imageUrl" : "https://s3.amazonaws.com/memeify-imageoutputbucket-AABBCC/VHERDZTFLS-grumpy-cat.jpg"}
```
Or, if an error occurs, JSON containing the error message will be returned:
```json
{ "errorMsg" : "image size must be <= 1MB"}
```



## Notes and Observations
This project started as a practice application to help me study for an AWS Developer Certification. Specifically,
I wanted to learn how AWS Proxy Integration worked between API-GateWay and Lambda (even though the exam does not go into 
that level of detail).

Kotlin was chosen as the implementation language as I had used it in the past and wanted to explore 
 how to use it with AWS Lambda. The Kotlin code uses Java's *BufferedImage*, *Font* and *Graphics2d* classes for all 
 image manipulation. This was relatively easy to do, as these libraries have been around for years and there are 
 plenty of examples on image manipulation on the internet. Overall, Kotlin was easy to work with, and the Java 
 integration was flawless.

Note 1: Getting image data from API Gateway into lambda, without API Gateway munging the data was not as easy as I
had hoped. After reading dozens of forum posts, I came to understand that API-Gateway did not support 
multipart/form-data until late 2017. Before that time, you couldn't POST multipart/form-data to API-GW. I am not
sure how developers worked around this (if it all).  Fortunately, support was eventually added and today you have to 
manually configure support for it within API Gateway 
[settings](https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-payload-encodings.html). Basically,
you tell API Gateway to treat multipart/form-data as binary data and it will automatically BASE64 encode the entire
multipart/form-data body. The body is then attached to the `ApiGatewayProxyEvent` and sent to your lambda function. 
This process keeps the image data from being corrupted, and it also increases your request size by about 33%. This can
limit the size of images you send thru API gateway (it has a 10MB request size limit). 

An alternate implementation approach, and one that avoids using API Gateway, would be to post new images directly 
into a S3 bucket and have your lambda trigger off of new bucket PUT events.

Note 2: The Memeify lambda is memory intensive as all processing is done in memory. Including: BASE64 decoding, 
decoding the multipart/form-data, and the actual image manipulation.
 If memory usage is a real concern, one alternative option would be to use lambda's `/tmp` disk storage 
(max size of 512MB) and manipulate your data from disk, trading processing speed for reduced memory usage. 
On the other hand, it may 
[ultimately be cheaper](https://medium.com/@jconning/aws-lambda-faster-is-cheaper-6bf32f58d741) to raise your lambda's
 memory limit in order to run faster.
