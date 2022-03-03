# TinyURL Counter Application

This a simple Kafka Streams Application demonstrating the custom application deployment of quick. This application
counts the keys of the input topic and writes a key value pair containing the key and its count to the output topic.
Concretely, we want to count the number of TinyURLs (keys) that users are retrieving. For more information on how to
build a Kafka Streams Application, please visit the
[streams-bootstrap repository](https://github.com/bakdata/streams-bootstrap).

## Topology

The counter application has a simple topology, which is demonstrated in the diagram below. The input-topic `track-fetch`
has a key type of `String` and a value type of `String`. The value is always an empty string. The output
topic `count-fetch` has a key of type `String` and value type of `Long`. Whenever a user fetches a TinyURL the key is
ingested in the `track-fetch` topic. The application creates a count over the keys in the input topic and writes the
values in the `count-fetch`, which represents the number of times users fetched a TinyURL.

<p align="center">
 <img src="https://github.com/bakdata/quick-examples/tree/master/tiny-url/TinyUrlTopology.png" />
</p>

## Deployment

The application can be deployed using [helm](https://helm.sh/). It uses
the [streams-bootsrap helm chart](https://github.com/bakdata/streams-bootstrap/tree/master/charts/streams-app):

```shell
helm upgrade --install --kube-context <YOUR_CONTEXT> --namespace <YOUR_NAMESPACE> --values deployment/values.yaml tiny-url-counter-app streams-bootstrap/streams-app
```

You should change the values
in [values.yaml](https://github.com/bakdata/quick/tree/master/docs/examples/TinyURL/counter/deployment/values.yaml) file
based on your infrastructure.

To uninstall the application just run:

```shell
helm uninstall --kube-context <YOUR_CONTEXT> --namespace <YOUR_NAMESPACE> tiny-url-counter-app
```
