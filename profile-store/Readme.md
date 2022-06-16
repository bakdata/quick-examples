# Profile store

This project combines several features of Quick
for user profiles on real-world music listening behaviour.
The results of multiple independent apps are managed in a single schema.
This includes a recommendation REST-service.
The frontend also makes use of graphql subscriptions and mutations.

* **data** contains a sample of the LFM-1b listening dataset 
together with a python script that was used to extract the data.
* **deployment** contains the graphql schema, the environment configuration
 and a justfile to deploy the streams apps and the frontend in kubernetes.
* **frontend** contains a python app that uses the Quick gateway as a backend.
* **streams-apps** contains the source code for the kafka streams apps 
 that calculate the values in the user profiles.
