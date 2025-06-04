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

1. Create a DB2 database named `STOCKDB`
2. Create the required tables:

```sql
CREATE TABLE stocks (
    symbol VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    current_price DECIMAL(10,2) NOT NULL,
    last_updated TIMESTAMP NOT NULL
);

CREATE TABLE stock_lending (
    id VARCHAR(36) PRIMARY KEY,
    symbol VARCHAR(10) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    FOREIGN KEY (symbol) REFERENCES stocks(symbol)
);
```

3. Configure Kafka topics:
   - `stock-prices`: For stock price updates
   - `stock-lending`: For stock lending operations

4. Update the database connection settings in `StockTradingApp.scala` if needed

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