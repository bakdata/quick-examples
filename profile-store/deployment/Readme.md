## Pre-Requisites

Required:
* Access to a quick instance (version => 0.6.0)
* [Helm](https://helm.sh/docs/intro/install/) installed
* [streams-bootstrap-charts](https://github.com/bakdata/streams-bootstrap/tree/master/charts) installed
* [quick-cli](https://github.com/bakdata/quick-cli) => 0.6.0 installed

Optional:
* [just](https://github.com/casey/just)

## Quick Setup

The setup includes the following steps:

1. Initialize a quick context
2. Create a quick gateway
3. Apply `schema.gql` to the gateway
4. Create the topics
5. Deploy the streams-apps
6. Initialize the item data (artist, album and track information)
7. Produce listeningevents

The `justfile` documents all necessary commmands and makes them executable with [just](https://github.com/casey/just).
But you can also execute all `helm` and `quick` commands directly from your shell.

