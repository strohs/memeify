Memeify
================================================================================================================

# S3 Branch
experimental branch that gets rid of API Gateway and instead uses the S3 `CreateObject` trigger to notify the 
Memeify lambda that an image has been uploaded to the S3 bucket.



Memeify is a AWS lambda function (written in Kotlin) that allows you to create memes by adding text to an 
image (jpeg or png). It's completely serverless and uses S3 to trigger our Memeify lambda whenever a new image
 is uploaded to our "input" bucket.  Memeified images are stored in separate "output" bucket. 


![grumpy-cat](https://github.com/strohs/memeify/blob/master/memeified-grumpy-cat.jpg)


## Building
This is a maven project consisting of two modules: `lambdas` and `frontend`. `Lambdas` contains the 
lambda code while `frontend` contains a sample web page that I used for playing around with vue.js and AWS.
(see the frontend section at the end of this readme for more information)

* to build the memeify lambda jar file, cd into the project root directory and run
    * `mvn clean package -pl lambdas`
        * the jar artifact will be built in `lambdas/target/memeify-lambdas-0.1.jar`

#### Deploying to AWS
A cloudformation [template](aws/template.yaml) has been provided for creating the Memeify stack. It will create the
following resources:
* the memeify lambda function, 
* two S3 buckets:
    * the "input" bucket receives images to be memeified
    * the "output" bucket holds the memeifed images
    * Note that **both S3 buckets will have public read/write access**

Deploy Steps:
1. build the lambda .jar file (as described above)
2. copy the lambda jar to an S3 bucket
3. run the template in cloudformation and point it to the S3 bucket containing the lambda code

## Running
Once the stack is up, you must upload an image file (.jpg or .pmg) into the input bucket. S3 will send an
`ObjectCreated` event to the memeify lambda which will place the memeified image in the "output" bucket.

