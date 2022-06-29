# Profile store

This project combines several features of Quick
for user profiles on real-world music listening behaviour.
The results of multiple independent apps are managed in a single schema.
This includes a recommendation REST-service.
The frontend also makes use of GraphQL subscriptions and mutations.

* **data** a sample of the LFM-1b listening dataset 
together with a python script that was used to extract the data.
* **deployment** the GraphQL schema, the environment configuration
 and a justfile to deploy the streams apps and the frontend in Kubernetes.
* **frontend** a python app that uses the Quick gateway as a backend.
* **streams-apps** the source code for the Kafka Streams apps 
 that calculate the values in the user profiles.
