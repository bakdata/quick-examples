# Tiny URL Counter Application

This a simple Kafka Streams Application demonstrating the custom application deployment of quick. This application
counts the keys of the input topic and writes a key value pair containing the key and its count to the output topic.
Concretely, we want to count the number of tiny-urls (keys) that users are retrieving. For more information on how to
build a Kafka Streams Application, please visit the
[streams-bootstrap repository](https://github.com/bakdata/streams-bootstrap).

## Topology

The counter application has a simple toplogy. The topology contains a source topic (let's call it `track-fetcher`) and a
sink topic (let's call it `count-fetcher`)
<p align="center">
 <img src="https://github.com/bakdata/quick/tree/master/docs/examples/TinyURL/TinyUrlTopology.png" />
</p>

## Usage
The application can be deployed using [helm](https://helm.sh/):

```shell
quick app deploy tiny-url-counter \
--registry us.gcr.io/d9p-quick/demo \
--image tiny-url-counter \
--tag 1.0.0 \
--args input-topics=track-fetch output-topic=count-fetch productive=false
```

The input-topic (track-fetch) has a key type of `String` and a value type of `String`. The value is always an empty
string. The output topic has a key of type `String` and value type of `Long`. Every time a key-value pair is ingested in
the input topic, the output topic gets updated respectively. Each pair in the output topic show how many times a user
queries a key (tiny-url).

## Deployment

We use the [Google Jib](https://github.com/GoogleContainerTools/jib) plugin to containerize and create an image of the
application. The container is hosted in `us.gcr.io/d9p-quick/demo/tiny-url-counter`
and the image version is based on the program version defined in the `build.gradle.kts` file.
