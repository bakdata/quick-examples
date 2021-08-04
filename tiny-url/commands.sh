#!/bin/bash

quick context create 

quick topic create tiny-url --key string --value string --immutable

quick topic create track-fetch --key string --value string

quick topic create count-fetch --key string --value long

quick app deploy test-tiny-counter \
--registry us.gcr.io/d9p-quick/demo \
--image test-tiny-url-counter \
--tag 1.0.0 \
--args input-topics=track-fetch output-topic=count-fetch productive=false 

quick gateway create tinyurl-gateway

cat schema.gql

quick gateway apply tinyurl-gateway -f schema.gql

curl --request POST --url https://$QUICK_HOST/ingest/tiny-url/ \
--header 'content-type: application/json' \
--header "X-API-Key:$KEY" \
--data '{"key": "hey", "value": "https://www.d9p.io"}'

curl --request POST --url https://$QUICK_HOST/ingest/track-fetch/ \
--header 'content-type: application/json' \
--header "X-API-Key:$KEY" \
--data '{"key": "hey", "value": ""}'
