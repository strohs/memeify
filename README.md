Memeify
================================================================================================================
Memeify is a AWS lambda function that allows you to create memes by adding text to an image (jpeg or png).  
Is powered by a completely serverless backend and uses API gateway to proxy incoming requests to the lambda and S3 
to store the final "memeified" images.


![grumpy-cat](https://github.com/strohs/memeify/blob/master/memeified-grumpy-cat.jpg)

    
The lambda code is written in Kotlin, and uses Java's *BufferedImage*, *Font* and *Graphics2d* classes for all 
image manipulation. It's nothing too fancy but I wrote it in order to explore Kotlin as well as get some experience
writing a completely serverless backend using AWS. 

## Building

## Running

## Frontend
[Vue.js](https://vuejs.org/) (using [Vuetify](https://vuetifyjs.com))

## Notes and Observations