#!/bin/bash

# Script to produce sample data into the Kafka topic
# Uses kafka-avro-console-producer from the cp-schema-registry image

TOPIC="input-topic"
SCHEMA_REGISTRY="http://karapace-registry:8081"
VALUE_SCHEMA='{"type":"record","name":"User","namespace":"example.avro","fields":[{"name":"name","type":"string"},{"name":"favorite_number","type":["int","null"]},{"name":"favorite_color","type":["string","null"]}]}'

echo "🚀 Producing sample data into topic '$TOPIC'..."
echo ""

docker run --rm -i \
  --network spring-boot-kafka-streams-interactive-queries-example_kafka-network \
  confluentinc/cp-schema-registry:8.1.0 \
  kafka-avro-console-producer \
    --bootstrap-server kafka:29092 \
    --topic "$TOPIC" \
    --property schema.registry.url="$SCHEMA_REGISTRY" \
    --property value.schema="$VALUE_SCHEMA" \
    --property parse.key=true \
    --property key.separator=: \
    --property key.serializer=org.apache.kafka.common.serialization.StringSerializer << 'EOF'
alice:{"name":"Alice","favorite_number":{"int":42},"favorite_color":{"string":"blue"}}
bob:{"name":"Bob","favorite_number":{"int":7},"favorite_color":{"string":"red"}}
charlie:{"name":"Charlie","favorite_number":null,"favorite_color":{"string":"green"}}
david:{"name":"David","favorite_number":{"int":99},"favorite_color":null}
eve:{"name":"Eve","favorite_number":{"int":13},"favorite_color":{"string":"purple"}}
EOF

echo "✅ Sample data produced successfully!"
echo ""
echo "You can now query the API:"
echo "  curl http://localhost:8080/state/keyvalue/user-table/alice | jq"
echo "  curl http://localhost:8080/state/keyvalue/user-table/bob | jq"
echo "  curl http://localhost:8080/state/all/user-table | jq"
