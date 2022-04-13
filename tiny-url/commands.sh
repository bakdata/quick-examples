#!/bin/bash

quick context create --host "$QUICK_HOST" --key "$QUICK_API_KEY" --context guide

quick context activate guide

quick gateway create tiny-url-gateway

quick gateway apply tiny-url-gateway -f schema.gql

quick topic create tiny-url --key string --value schema --schema tiny-url-gateway.TinyUrl --immutable

quick topic create track-fetch --key string --value string

quick topic create count-fetch --key string --value long

quick app deploy tiny-url-counter \
    --registry bakdata \
    --image quick-demo-tinyurl \
    --tag 1.0.0 \
    --args input-topics=track-fetch output-topic=count-fetch productive=false

curl --request POST --url "$QUICK_URL/ingest/tiny-url/" \
  --header 'content-type: application/json' \
  --header "X-API-Key: $QUICK_API_KEY" \
  --data '@./tiny-urls.json'

curl --request POST --url "$QUICK_HOST"/ingest/track-fetch/ \
	--header "content-type: application/json" \
	--header "X-API-Key: $QUICK_API_KEY" \
	--data '{"key": "d9p", "value": ""}'

curl --request POST --url "$QUICK_HOST"/ingest/track-fetch/ \
	--header "content-type: application/json" \
	--header "X-API-Key: $QUICK_API_KEY" \
	--data '{"key": "d9p", "value": ""}'
