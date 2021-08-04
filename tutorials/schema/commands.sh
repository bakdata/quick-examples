# Initialize quick with your domain and API key
quick context create 

# create topic with a value schema
quick topic create product-topic -k long -v schema --schema product-schema.gql

# deploy gateway
quick gateway create product-gateway

# apply definition
quick gateway apply product-gateway -f schema-query-single.gql

# add product
curl -X POST --url https://$QUICK_URL/ingest/product-topic \
    --header "content-type: application/json" \
    --header "X-API-Key:$KEY" \
    --data "@./products.json"

# try to ingest this product -> The schema prevents us from ingesting it because its schema is incorrect
curl -X POST --url https://$QUICK_URL/ingest/product-topic \
    --header "content-type: application/json" \
    --header "X-API-Key:$KEY" \
    --data "@./invalid-product.json"

# Query data in graphiql with findProduct(productId: 123) {...}
curl --request POST \
    --url https://$QUICK_URL/gateway/product-gateway/graphql \
    --header "Content-Type: application/json" \
    --header "X-API-Key:$KEY" \
    --data '{"query":"query {\n  findProduct(productId: 123) {\n    productId\n    name\n    description\n    price {\n      total\n      currency\n    }\n    metadata {\n      created_at\n      source\n    }\n  }\n}\n"}'

# apply updated definition - now its possible to query all products
quick gateway apply product-gateway -f schema-query-all.gql

# Query data in graphql with allProducts {...}
curl --request POST \
    --url https://$QUICK_URL/gateway/product-gateway/graphql \
    --header "Content-Type: application/json" \
    --header "X-API-Key:$KEY" \
    --data '{"query":"query {\n  allProducts {\n    productId\n    name\n    description\n    price {\n      total\n      currency\n    }\n    metadata {\n      created_at\n      source\n    }\n  }\n}\n"}'
