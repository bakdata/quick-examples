# TinyURL

TinyURL is an application that shortens any given URL to a tiny one. Shortened URLs are quite helpful, for example, for people posting on Twitter. This tutorial shows how to implement your URL shortening web service app with Quick .

## Quick

To be able to use your tiny url app you need to create your project first.
To do that, open your terminal and initialize quick with the given `$QUICK_HOST` and `$KEY`.
```sh
quick context create 
```
Use the quick-cli to create a new topic in wich all token, url pairs will be stored with the command:
```sh
quick topic create tiny-url --key string --value string --immutable
```
The topic is immutable, that means that there will be no two keys that are the same.

In order to count how many times someone fetched our url we use the quick counter application. To use it we create input and output topics.
The input topic we create with the command:
```sh
quick topic create track-fetch --key string --value string
```
And the output topic with the command:
```sh
quick topic create count-fetch --key string --value long 
```
We deploy the counter application with the following comand:
```sh
quick app deploy tiny-url-counter \
--registry us.gcr.io/d9p-quick/demo \
--image tiny-url-counter \
--tag 1.0.0 \
--args input-topics=track-fetch output-topic=count-fetch productive=false 

```
Then create a new gateway using this command:
```sh
quick gateway create tinyurl-gateway
```
Apply your GraphQL schema on the tinyurl-gateway by using the following command:
```sh
quick gateway apply tinyurl-gateway -f schema.gql
```
The schema contains the query structure, which you will see on GraphQL.
```graphql
type Query {
  fetchURL(token: String): TinyUrl
}
type TinyUrl {
  url: String @topic(name: "tiny-url", keyArgument: "token")
  count: Long @topic(name: "count-fetch", keyArgument: "token")
}
```

**NOTE**: You can see those commands in commands.sh file.

## Frontend

We use a form for creating key-value pairs with input fields for the word and the url and one more for fetching the saved url in the word.

## Backend
With every click on the Submit button happens an ingest in the tinyURL topic, that can also be written with this command:
```sh
curl --request POST --url https://$QUICK_HOST/ingest/tiny-url/ \
--header 'content-type: application/json' \
--header "X-API-Key:$KEY" \
--data '{"key": "d9p", "value": "https://www.d9p.io"}' 
```
With every click of the fetch button happens an ingest in the input topic of the counter, that is the same as this:
```sh
curl --request POST --url https://$QUICK_HOST/ingest/track-fetch/ \
--header 'content-type: application/json' \
--header "X-API-Key:$KEY" \
--data '{"key": "hey", "value": ""}'
```
Then the counter counts how many times the same key is ingested in the input topic and outputs the number as a value in the output topic.

## Results
In GraphQl we can query our data. We use the gateway_ip.

```graphql
{
  fetchURL(token: "d9p") {
    url
    count
  }
}

```
And the output is:
```json
{
  "data": {
    "fetchURL": {
      "url": "https://www.d9p.io",
      "count": 2
    }
  }
}
```
