# TinyURL Counter Application

This is a simple Kafka Streams Application demonstrating the custom application deployment of quick. This application
counts the keys of the input topic and writes a key-value pair containing the key and its count to the output topic.
Concretely, we want to count the number of TinyURLs (keys) that users are retrieving. For more information on how to
build a Kafka Streams Application please visit the
[streams-bootstrap repository](https://github.com/bakdata/streams-bootstrap).

## Prerequisite

- Installed [helm](https://helm.sh/) version 3.x.x
-Add [streams-bootsrap helm chart version 1.9.0](https://github.com/bakdata/streams-bootstrap/tree/1.9.0/charts/streams-app)
to your helm repository:
  ```shell
  helm repo add streams-bootstrap https://raw.githubusercontent.com/bakdata/streams-bootstrap/1.9.0/charts
  ```
- Up and running Kafka (version 2.8.0) and Quick instance

## Topology

The counter application has a simple topology, demonstrated in the diagram below. The input-topic `track-fetch`
has a key type of `String` and a value type of `String`. The value is always an empty string. The output
topic `count-fetch` has a key of type `String` and value type of `Long`. Whenever a user fetches a TinyURL, the key is
ingested in the `track-fetch` topic. The application creates a count over the keys in the input topic and writes the
values in the `count-fetch`, representing the number of times users fetched a TinyURL.

<p align="center">
 <img src="https://github.com/bakdata/quick-examples/blob/main/tiny-url/counter/TinyUrlTopology.png" />
</p>

## Deployment

The counter application can be deployed using [helm](https://helm.sh/):

```shell
helm upgrade --install \
--kube-context <YOUR_CONTEXT> \
--namespace <YOUR_NAMESPACE> \
--set streams.brokers=<KAFKA_BROKER> \
--set streams.schemaRegistryUrl=<SCHEMA_REGISTRY_URL> \
--values deployment/values.yaml \
tiny-url-counter-app streams-bootstrap/streams-app
```

You should change the values
in [values.yaml](https://github.com/bakdata/quick/tree/master/docs/examples/TinyURL/counter/deployment/values.yaml) file
based on your infrastructure.

To uninstall the application, just run:

```shell
helm uninstall \
--kube-context <YOUR_CONTEXT> \
--namespace <YOUR_NAMESPACE> \
tiny-url-counter-app
```
