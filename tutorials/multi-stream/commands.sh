# Initialize the quick-cli with you domain and API key
quick context create 

# Create a product and purchase topic with a schema
quick topic create products-topic -k long -v schema --schema product-schema.gql
quick topic create purchase-topic -k string -v schema --schema purchase-schema.gql

# Deploy a gateway
quick gateway create product-gateway

# Apply the definition to the newly deployed gateway
quick gateway apply product-gateway -f schema.gql

# Ingest data
curl -X POST --url https://$QUICK_HOST/ingest/products-topic \
    --header "content-type: application/json" \
    --header "X-API-Key: $KEY" \
    --data "@./products.json"

curl -X POST --url https://$QUICK_HOST/ingest/purchase-topic \
    --header "content-type: application/json" \
    --header "X-API-Key: $KEY" \
    --data "@./purchases.json"

# 
curl --request POST --url https://$QUICK_HOST/gateway/product-gateway/graphql \
    --header "Content-Type: application/json" \
    --header "X-API-Key: $KEY" \
    --data '{"query":"query {\n  findPurchase(purchaseId: \"abc\") {\n    purchaseId\n    amount\n    product {\n      name\n      description\n    }\n}\n}"}'
