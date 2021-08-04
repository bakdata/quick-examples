# Install quick
pip install quick_cli-0.1.1-py3-none-any.whl -q

# Check that we successfully installed it
quick -v

# Initialize quick with your domain and API key
quick context create --host $QUICK_HOST --key $KEY

# Create a new topic named "product-topic"
quick topic create product-topic -k long -v string

# Deploy a gateway
quick gateway create product-gateway

# apply schema to gateway
quick gateway apply product-gateway -f schema.graphqls

# Add one value
curl --request POST --url https://$QUICK_URL/ingest/product-topic/ \
    --header "content-type: application/json" \
    --header "X-API-Key:$KEY" \
    --data '{"key": 123, "value": "T-Shirt"}'

# Request data from gateway - you can also do this with your preferred client
curl --request POST --url https://$QUICK_URL/gateway/product-gateway/graphql \
    --header "Content-Type: application/json" \
    --header "X-API-Key:$KEY" \
    --data '{"query":"{findProduct(productId: 123)}"}'

# Add multiple values
curl --request POST --url https://$QUICK_URL/ingest/product-topic/ \
    --header "content-type: application/json" \
    --header "X-API-Key:$KEY" \
    --data '[ {"key": 123, "value": "T-Shirt (black)"}, {"key": 456, "value": "Jeans"}, {"key": 789, "value": "Shoes"}]'

# Query GraphQL again - they value should updated
curl --request POST --url https://$QUICK_URL/gateway/product-gateway/graphql \
    --header "Content-Type: application/json" \
    --header "X-API-Key:$KEY" \
    --data '{"query":"{findProduct(productId: 123)}"}'

# Delete a key value pair
curl --request DELETE --url https://$QUICK_URL/ingest/product-topic/123 \
    --header "X-API-Key:$KEY"

# Query once more: the value shouldn't exist anymore
curl --request POST --url https://$QUICK_URL/gateway/product-gateway/graphql \
    --header "Content-Type: application/json" \
    --header "X-API-Key:$KEY" \
    --data '{"query":"{findProduct(productId: 123)}"}'
