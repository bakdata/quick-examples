# TinyURL

TinyURL is an application that shortens any given URL to a tiny one. For example, shortening URLs are pretty helpful for
people posting on Twitter. This tutorial shows a step-by-step guide on how to run your TinyURL application with Quick.

## Prerequisite
- You should have a Quick instance up and running.
- You should have [quick-cli](https://github.com/bakdata/quick-cli) installed and initialized.
    ```sh
    quick context create --host <HOST_ADDRESS> --key <X_API_KEY>
    ```
- Clone the repository [quick-examples](https://github.com/bakdata/quick-examples).
- Add [streams-bootsrap helm chart version 1.9.0](https://github.com/bakdata/streams-bootstrap/tree/1.9.0/charts/streams-app)
to your helm repository:
  ```shell
  helm repo add streams-bootstrap https://raw.githubusercontent.com/bakdata/streams-bootstrap/1.9.0/charts
  helm repo update
  ```

## Setup and Installation

1. Use the quick-cli to create a new topic called `tiny-url`. This topic stores the tokens as its key along with the
   URLs as its value.

    ```sh
    quick topic create tiny-url --key string --value string --immutable
    ```
   Note the `--immutable` flag. This flag determines that the topic is immutable, so there will be no duplicate keys.

2. In this example, we created a simple Kafka-streams application to create a count over the fetched URLs by the users. You
   can find the source code of the counter
   application [here](https://github.com/bakdata/quick-examples/tree/main/tiny-url/counter). The Kafka-streams
   application topology is demonstrated in the diagram below. The topology uses a source topic called `track-fetcher`
   and a sink topic called `count-fetch`. </br>
   <p align="center">
    <img src="https://github.com/bakdata/quick-examples/blob/main/tiny-url/counter/TinyUrlTopology.png" />
   </p>
   You can create both topics with the following commands:

   ```sh
   quick topic create track-fetch --key string --value string
   quick topic create count-fetch --key string --value long 
   ```

3. Now that we created the topics it is time to deploy
   the [counter](https://github.com/bakdata/quick-examples/tree/main/tiny-url/counter) application:
   ```sh
   helm upgrade --install \
       --kube-context <YOUR_CONTEXT> \
       --namespace <YOUR_NAMESPACE> \
       --set streams.brokers=<KAFKA_BROKER> \
       --set streams.schemaRegistryUrl=<SCHEMA_REGISTRY_URL> \
       --values deployment/values.yaml \
       tiny-url-counter-app streams-bootstrap/streams-app
   ```

4. Then create a new gateway using this command:

   ```sh
   quick gateway create tiny-url-gateway
   ```
   **NOTE:** When you create a gateway, it might take some time until the gateway is running.

5. Apply the [GraphQL schema](https://github.com/bakdata/quick-examples/blob/main/tiny-url/schema.gql) on
   the `tiny-url-gateway` by using the following command:

   ```sh
   quick gateway apply tiny-url-gateway -f schema.gql
   ```

   **NOTE**: You can find all these commands
   in `commands.sh` [file](https://github.com/bakdata/quick-examples/blob/main/tiny-url/commands.sh).

## Frontend

We use a form for creating key-value pairs with input fields for the word and the URL and one more for fetching the
saved URL in the word.

## Backend

To ingest a new TinyURL entity (token and URL), you can use the command below:

```sh
curl --request POST --url "$QUICK_HOST"/ingest/tiny-url/ \
  --header 'content-type: application/json' \
  --header "X-API-Key:$API_KEY" \
  --data '{"key": "d9p", "value": "https://www.d9p.io"}'
```

Now let's simulate a scenario where the user fetches a token. This can be done with the command below:

```sh
curl --request POST --url "$QUICK_HOST"/ingest/track-fetch/ \
  --header 'content-type: application/json' \
  --header "X-API-Key:$API_KEY" \
  --data '{"key": "d9p", "value": ""}'
```

Then the counter counts how many times the same key is ingested in the input topic and outputs the number as a value in
the output topic.

## Results

Imagine a scenario where the users fetched the token `d9p` URL twice. Let's query the data and see the results:

```graphql
query {
  fetchCountOfToken(token: "d9p") {
    url
    count
  }
}

```

And the output should be:

```json
{
  "data": {
    "fetchCountOfToken": {
      "url": "https://www.d9p.io",
      "count": 2
    }
  }
}
```

## Teardown

To delete all the resources, follow these steps:

1. Delete counter application:
   ```shell
   helm uninstall --kube-context <YOUR_CONTEXT> --namespace <YOUR_NAMESPACE> tiny-url-counter-app
   ```
2. Delete topics:
   ```shell
   quick topic delete tiny-url && \
   qucik topic delete track-fetch && \
   quick topic delete count-fetch
   ```
3. Delete gateway:
   ```shell
   quick gateway delete tiny-url-gateway
   ```
