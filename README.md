# Stock Trading Simulation

A Scala-based stock trading simulation system that processes stock lending and price information using Kafka and DB2.

## Features

- Real-time stock price processing
- Stock lending management
- Kafka-based event streaming
- DB2 database integration
- JSON serialization/deserialization
- Comprehensive error handling and logging

## Prerequisites

- Scala 2.13.8
- SBT (Scala Build Tool)
- Apache Kafka
- IBM DB2 Database
- Java 8 or higher
- Docker

## Project Structure

```
src/main/scala/com/stocktrading/
├── model/
│   └── Models.scala         # Domain models and JSON serialization
├── kafka/
│   └── KafkaConfig.scala    # Kafka configuration and services
├── db/
│   └── DatabaseService.scala # DB2 database operations
└── StockTradingApp.scala    # Main application
```

## Setup

### 1. Database Setup (DB2)

1. Pull and run DB2 container:
```bash
docker pull ibmcom/db2

docker run -h db2server \
  --name db2server \
  --restart=always \
  --detach \
  --privileged=true \
  -p 50000:50000 \
  -e LICENSE=accept \
  -e DB2INST1_PASSWORD=password \
  -e DBNAME=STOCKDB \
  -v db2data:/database \
  ibmcom/db2
```

2. Connect to DB2 container:
```bash
docker exec -it db2server bash
```

3. Switch to DB2 user:
```bash
su - db2inst1
```

4. Connect to the database:
```bash
db2 connect to STOCKDB
```

5. Create the required tables:
```sql
db2 "CREATE TABLE stocks (
    symbol VARCHAR(10) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    current_price DECIMAL(10,2) NOT NULL,
    last_updated TIMESTAMP NOT NULL
)"

db2 "CREATE TABLE stock_lending (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    symbol VARCHAR(10) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    FOREIGN KEY (symbol) REFERENCES stocks(symbol)
)"
```

6. Insert sample data:
```sql
db2 "INSERT INTO stocks (symbol, name, current_price, last_updated) VALUES 
    ('AAPL', 'Apple Inc.', 175.50, CURRENT TIMESTAMP),
    ('GOOGL', 'Alphabet Inc.', 142.75, CURRENT TIMESTAMP),
    ('MSFT', 'Microsoft Corporation', 330.25, CURRENT TIMESTAMP),
    ('AMZN', 'Amazon.com Inc.', 145.80, CURRENT TIMESTAMP),
    ('TSLA', 'Tesla Inc.', 245.30, CURRENT TIMESTAMP)"
```

### 2. Kafka Setup

1. Pull and run Kafka container:
```bash
docker pull confluentinc/cp-kafka

docker run -d --name kafka \
  -p 9092:9092 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  confluentinc/cp-kafka
```

2. Create Kafka topics:
```bash
# Connect to Kafka container
docker exec -it kafka bash

# Create topics
kafka-topics --bootstrap-server localhost:9092 --create --topic stock-prices --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server localhost:9092 --create --topic stock-lending --partitions 1 --replication-factor 1

# Verify topics
kafka-topics --bootstrap-server localhost:9092 --list
```

3. Reset Kafka topics (if needed):
```bash
# Delete topics
kafka-topics --bootstrap-server localhost:9092 --delete --topic stock-prices
kafka-topics --bootstrap-server localhost:9092 --delete --topic stock-lending

# Recreate topics
kafka-topics --bootstrap-server localhost:9092 --create --topic stock-prices --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server localhost:9092 --create --topic stock-lending --partitions 1 --replication-factor 1
```

4. Verify Kafka topics and messages:
```bash
# List all topics
kafka-topics --bootstrap-server localhost:9092 --list

# Check messages in stock-prices topic
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic stock-prices \
  --from-beginning \
  --max-messages 1

# Check messages in stock-lending topic
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic stock-lending \
  --from-beginning \
  --max-messages 1

# Reset consumer group offsets (if needed)
kafka-consumer-groups --bootstrap-server localhost:9092 --list

kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group stock-price-processor \
  --reset-offsets \
  --to-earliest \
  --execute \
  --topic stock-prices

kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group stock-lending-processor \
  --reset-offsets \
  --to-earliest \
  --execute \
  --topic stock-lending
```

5. Useful Kafka commands for troubleshooting:
```bash
# Check topic details
kafka-topics --bootstrap-server localhost:9092 --describe --topic stock-prices
kafka-topics --bootstrap-server localhost:9092 --describe --topic stock-lending

# Check consumer groups
kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Check consumer group details
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group stock-price-processor \
  --describe

# Check consumer group offsets
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group stock-price-processor \
  --describe \
  --all-topics
```

### 3. Application Configuration

Update the database connection settings in `StockTradingApp.scala` if needed:
```scala
DatabaseService.setConfig(new DatabaseServiceConfig(
  url = "jdbc:db2://localhost:50000/STOCKDB",
  user = "db2inst1",
  password = "password"
))
```

## Building and Running

1. Build the project:
```bash
sbt clean compile
```

2. Run the application:
```bash
sbt run
```

## Configuration

The application can be configured by modifying the following:

- Kafka settings in `KafkaConfig.scala`
- Database connection settings in `StockTradingApp.scala`
- Processing intervals in the main application loop

## Error Handling

The application includes comprehensive error handling for:
- Database operations
- Kafka message processing
- JSON serialization/deserialization
- General application errors

All errors are logged using SLF4J with Logback.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request 