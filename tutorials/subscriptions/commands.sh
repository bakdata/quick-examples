quick context create 

# Create a product and purchase topic with a schema
quick topic create product-topic -k long -v schema --schema product-schema.gql
quick topic create purchases-topic -k string -v schema --schema purchase-schema.gql
quick topic create user-topic -k string -v schema --schema user-schema.gql

# Deploy a gateway
quick gateway create product-gateway

# Apply the definition to the newly deployed gateway
quick gateway apply product-gateway -f schema.gql

# Create products and ids
python generator_products.py

# Ingest data
curl -X POST --url https://$QUICK_URL/ingest/products-topic \
    --header "content-type: application/json" \
    --header "X-API-Key: $KEY" \
    --data "@./products.json"

curl -X POST --url https://$QUICK_URL/ingest/user-topic \
    --header "content-type: application/json" \
    --header "X-API-Key: $KEY" \
    --data "@./users.json"
# Ingest streaming data
python generator_purchases.py

# Query Stream with websocket on wss://demo.d9p.io/gateway/product-gateway/graphql