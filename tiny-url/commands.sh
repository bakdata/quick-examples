#!/bin/bash

helm repo add streams-bootstrap https://raw.githubusercontent.com/bakdata/streams-bootstrap/1.9.0/charts

quick context create --host $QUICK_HOST --key $API_KEY

quick topic create tiny-url --key string --value string --immutable

quick topic create track-fetch --key string --value string

quick topic create count-fetch --key string --value long

helm upgrade --install \
	--kube-context $QUICK_KUBE_CONTEXT \
	--namespace $QUICK_KUBE_NAMESPACE \
	--set streams.brokers=$KAFKA_BROKER \
	--set streams.schemaRegistryUrl=$SR_URL \
	--values deployment/values.yaml \
	tiny-url-counter-app streams-bootstrap/streams-app

quick gateway create tinyurl-gateway

quick gateway apply tinyurl-gateway -f schema.gql

curl --request POST --url "$QUICK_HOST"/ingest/tiny-url/ \
	--header 'content-type: application/json' \
	--header "X-API-Key:$API_KEY" \
	--data '{"key": "hey", "value": "https://www.d9p.io"}'

curl --request POST --url "$QUICK_HOST"/ingest/track-fetch/ \
	--header 'content-type: application/json' \
	--header "X-API-Key:$API_KEY" \
	--data '{"key": "hey", "value": ""}'
