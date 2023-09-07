Memeify
================================================================================================================
Memeify is an example Amazon Web Services (AWS) application that adds user submitted text to the top and bottom of a user
submitted jpeg/png file.

It uses AWS Lambda to process the image, API Gateway to send/receive the image and text, and S3 to store the final image file.
The Lambda function is written in Kotlin.

This application is inspired by popular "meme" creation services that exist on the web today, such as [imgFlip.com](https://imgflip.com/memegenerator), but with 
much more basic functionality.


The mile-high architectural view is as follows:
1. A user submits an image file, along with text to be placed into the image, via an HTTP POST request to an AWS API Gateway endpoint.
2. API Gateway receives the image and text as multipart/form data and send it to the memeify lambda function
3. The memeify lambda function validates the image and text to make sure it doesn't exceed size limitations, and if everything is ok, it
   will "bake" the text into the image and then save the image into an S3 bucket. If an error occured, a JSON response with a description
   of the error is returned to the caller.


![grumpy-cat](https://github.com/strohs/memeify/blob/master/memeified-grumpy-cat.jpg)


## Prerequisites
- Java 1.8 installed (OpenJDK 1.8 was used during development and testing)
- Apache Maven (at least maven 3.5, versions greater than 3.5 should work)
- An AWS account along with experience using AWS. You should know how to use the AWS console and be familiar with CloudFormation and S3. 
  Specifically, you will need to be familiar with creating/accessing S3 bucket(s) and be able to deploy CloudFormation 
  templates.
- API Gateway knowledge is nice to have, but not a strict requirement. At a minimum, you should be able to access the API Gateway
  management console to verify that the memeify endpoint was created


## Building
Building Memeify consists of the following steps:
1. build the `memeify-lambda-0.1.jar` file using java and maven
2. copying the memeify lamda .jar file and CloudFormation [template](aws/memeify-template.yaml) to one of your S3 buckets
3. using CloudFormation to build the memeify stack, which will create:
   1. the memeify lambda function
   2. an API Gateway Endpoint that will receive multipart/form data and forward it to the lambda function
   3. an S3 bucket to store the final images **this S3 bucket will be given public read access**

### Building the memeify-lambda.jar file
to build the memeify lambda jar file, cd into the project root directory and run the following maven command
    - `mvn clean package -pl memeify-lambda`
    - the jar artifact will be built and then saved at `memeify-lambda/target/memeify-lambda-0.1.jar`

### Copy the lambda .jar file and Cloudformation to an S3 bucket
- copy the `memeify-lambda-0.1.jar` file and the CloudFormation [template](aws/memeify-template.yaml) file to an
s3 bucket of your choosing. This bucket is used as a staging area so that CloudFormation can find and deploy the 
lambda function
    
### Deploying to AWS
Deploy Steps:
1. make sure the `memeify-lambda-0.1.jar` file is built and uploaded to a S3 bucket (as described above)
2. use CloudFormation (either the console or CLI) to create the memeify stack using the provided [template](aws/memeify-template.yaml):
    - you will need to provide three parameters to CloudFormation:
        - the stack name, for example: "MyMemeifyStack"
        - the name of the S3 bucket (created in step 2) containing the `memeify-lambda-0.1.jar`
        - the file-name of the memeify lambda .jar file itself. This will default to `memeify-lambda-0.1.jar` so you only need
          to change this if you renamed the .jar file
    - once the above parameters are provided the stack will be ready for creation, and you can accept the rest of the
    cloudformation defaults.
    - If everything went well, the stack will have created the following resources:
        - An API Gateway endpoint `/memeify`, that accepts POST requests
        - a public S3 bucket to hold the final memefied images, i.e.: "mymemifystack-imageoutputbucket-ab5334bnf" 
        - the memeify lambda function: i.e.: "MyMemeifyStack-MemeifyLambda-34GZG44"
    - The outputs section (within the CloudFormation management console) will contain the ApiUrl that can be used
      to submit images to memeify, for example: `https://dfXDFA34asdf.execute-api.us-east-1.amazonaws.com/Stage/memeify`
      

## Submitting Images and Text to Memeify
I've provided two options for sending images to memeify:

- [Option 1 use the example curl script](#option-1-use-curl) to POST your image and text to the API Gateway endpoint
- [Option 2 use the example HTML Page](#option-2-use-HTML-Page) to submit the image and text to the API Gateway endpoint

Both approaches will require you to edit the script (or html page) with the memeify API Gateway endpoint. 

Please be aware that you should not submit images > 2 MegaBytes in size. This is because the memeify lambda is 
(by default) configured with 256 MB of memory and all image processing is done... in memory. If you wish to use larger 
images, you will need to increase the lambda's memory.


### Option 1 use curl
You can use the [provided curl script](aws/post-image.sh) to send data to your memeify endpoint. The script
will send an image file plus two text strings to memeify as multipart/form-data. Before running you must configure the 
script with your memeify API Gateway endpoint.
If successful, memeify will return a json response containing a link to the image in s3:

```json
{ "imageUrl" : "https://s3.amazonaws.com/memeify-imageoutputbucket-AABBCC/VHERDZTFLS-grumpy-cat.jpg"}
``` 

You should then be able to put this URL into a web-browser and view the image


### Option 2 use HTML Page
There is a sample [HTTP page](memeify-lambda/src/main/resources/index.html) that can be used to submit your image and text via an HTML form. 
The page uses the javascript fetch API to submit data to the memeify API endpoint (as multipart form-data) and then
displays the final, "memeified", image within the page. Before using the page, you must edit its javascript code with
 your memeify API URL. Simply open the index.html page and search for the text: "TODO"




## Memeify Architecture
Nothing too fancy here. Multipart/form-data is posted to API Gateway which will BASE64 encode the entire request 
body and send it to the memeify lambda as a `ApiGatewayProxyEvent`.  The memeify lambda takes the request body, 
BASE64 decodes it, and then parses the image data and text strings from the multipart form-data. The image is 
then "memeified" and written to a S3 bucket. Finally, a JSON response containing a URL to the "memeified" image 
is returned by the lambda.  
 
### input
The memeify lambda expects to receive three fields POSTed as multipart/form-data. The data must contain the following
field names and types:

| field name |          type           |                                                      constraints                                                      |
|:----------:|:-----------------------:|:---------------------------------------------------------------------------------------------------------------------:|
|   image    | image/jpeg OR image/png | for best results, no less than 400x300 and image size <= 2 MB (to process bigger images, increase lambda memory size) |
|  topText   |         String          |                                                   <= 75 characters                                                    |
|  botText   |         String          |                                                   <= 75 characters                                                    |


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
 plenty of examples on image manipulation on the internet. Overall, Kotlin was easy to work with and it integrates 
 seamlessly with Java.

Caveat 1: Getting image data from API Gateway into lambda, without API Gateway mangling the data was not as easy as I
had hoped. After reading dozens of forum posts, I came to understand that API-Gateway did not support 
multipart/form-data until late 2017. Before that time, you couldn't POST multipart/form-data to API-Gateway. I am not
sure how developers worked around this (if it all).  Fortunately, support was eventually added and today you have to 
manually configure support for it within API Gateway 
[settings](https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-payload-encodings.html). Basically,
you tell API Gateway to treat multipart/form-data as binary data and it will automatically BASE64 encode the entire
multipart/form-data body. The body is then attached to the `ApiGatewayProxyEvent` and sent to your lambda function. 
This process keeps the image data from being corrupted, but it increases your request size by about 33%. This can
limit the size of images you send thru API gateway as it has a 10MB request size limit. 

An alternate implementation approach, and one that avoids using API Gateway, would be to post new images directly 
into a S3 bucket and have your lambda trigger off of new bucket PUT events.

Caveat 2: The Memeify lambda is memory intensive as all processing is done in memory. Including: BASE64 decoding, 
multipart/form-data decoding, and the actual image manipulation.
 If memory usage is a real concern, one alternative option would be to use lambda's `/tmp` disk storage 
(max size of 512MB) and manipulate your data from disk, trading processing speed for reduced memory usage. 
On the other hand, it may 
[ultimately be cheaper](https://medium.com/@jconning/aws-lambda-faster-is-cheaper-6bf32f58d741) to raise your lambda's
 memory limit in order to run faster.
