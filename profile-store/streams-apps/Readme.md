# Kafka Streams Apps

The kafka streams apps independently calculate the values in the user profiles.
Most apps have simple topologies. They are implemented using bakdata's [streams-bootstrap](https://github.com/bakdata/streams-bootstrap) library.

### Overview

* **common**: Avro schemas, utils and shared definitions
* **itemtables**: Produce the item data (artists, albums, tracks) to kafka topics
* **listen-activity**: Track the first or last listening event per user
* **listen-charts**: Track the k most listenend artists/albums/tracks per user
* **listen-count**: Count the listeningevents per user
* **listeningevents**: Produce listeningevents to kafka. The app supports using the dataset as a realtime simulation.
* **recommender**: A REST-service implementig the SALSA algorithm in kafka.
