package com.stocktrading

import com.stocktrading.kafka.{KafkaConfig, KafkaConsumerService, KafkaProducerService}
import com.stocktrading.db.DatabaseService
import com.stocktrading.db.DatabaseServiceConfig
import com.stocktrading.model.Models.{stockLendingDecoder, stockPriceDecoder}
import com.stocktrading.model.{Active, LendingStatus, StockLending, StockPrice}
import com.typesafe.scalalogging.LazyLogging

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
//import scala.concurrent.duration._
import scala.util.{Failure, Success}

object StockTradingApp extends App with LazyLogging {
  // Initialize services
  private val dbInstance = new DatabaseService()
    dbInstance.setConfig(DatabaseServiceConfig(
    url = "jdbc:db2://localhost:50000/STOCKDB",
    user = "db2inst1",
    password = "password"
  ))
  
  private val kafkaProducer = new KafkaProducerService
  private val stockPriceConsumer = new KafkaConsumerService[StockPrice](
    KafkaConfig.stockPriceTopic,
    "stock-price-processor"
  )
  private val stockLendingConsumer = new KafkaConsumerService[StockLending](
    KafkaConfig.stockLendingTopic,
    "stock-lending-processor"
  )

  private def sendMockData(): Unit = {
    try {
      val yesterday = Instant.now.minus(1, ChronoUnit.DAYS)
      kafkaProducer.sendStockPrice(StockPrice("AAPL", 999.50, Instant.now))
      kafkaProducer.sendStockPrice(StockPrice("TSLA", 265.30, Instant.now))
      kafkaProducer.sendStockPrice(StockPrice("GOOGL", 162.75, Instant.now))
      kafkaProducer.sendStockPrice(StockPrice("MSFT", 360.25, Instant.now))
      kafkaProducer.sendStockPrice(StockPrice("AMZN", 165.80, Instant.now))

      // Add some stock lending records
      kafkaProducer.sendStockLending(StockLending(
        id = UUID.randomUUID().toString,
        symbol = "AAPL",
        userId = "USER001",
        quantity = 50,
        startDate = Instant.now,
        endDate = None,
        status = Active
      ))

      kafkaProducer.sendStockLending(StockLending(
        id = UUID.randomUUID().toString,
        symbol = "TSLA",
        userId = "USER002",
        quantity = 25,
        startDate = Instant.now,
        endDate = None,
        status = Active
      ))
    } catch {
      case ex: Exception =>
        logger.error(s"Error sending mock data through ${ex.getMessage}")
    }
  }
  
  // Process stock price updates
  private def processStockPrices(): Unit = {
    val prices = stockPriceConsumer.poll()
    prices.foreach { price =>
      dbInstance.getStock(price.symbol) match {
        case Success(Some(stock)) =>
          val updatedStock = stock.copy(
            currentPrice = price.price,
            lastUpdated = price.timestamp
          )
          dbInstance.saveStock(updatedStock) match {
            case Success(_) => logger.info(s"Updated stock price for ${price.symbol}")
            case Failure(ex) => logger.error(s"Failed to update stock price: ${ex.getMessage}")
          }
        case Success(None) =>
          logger.warn(s"Received price update for unknown stock: ${price.symbol}")
        case Failure(ex) =>
          logger.error(s"Error processing stock price: ${ex.getMessage}")
      }
    }
  }
  
  // Process stock lending requests
  private def processStockLending(): Unit = {
    val lendings = stockLendingConsumer.poll()
    lendings.foreach { lending =>
      dbInstance.saveStockLending(lending) match {
        case Success(_) => logger.info(s"Processed lending request for ${lending.symbol}")
        case Failure(ex) => logger.error(s"Failed to process lending request: ${ex.getMessage}")
      }
    }
  }
  
  // Main processing loop
  private def run(): Unit = {
    logger.info("Starting Stock Trading Application")
    
    while (true) {
      sendMockData()
      processStockPrices()
      processStockLending()
      //Thread.sleep(1000) // Process every second
    }
  }
  
  // Start the application
  try {
    run()
  } catch {
    case ex: Exception =>
      logger.error(s"Application error: ${ex.getMessage}")
  } finally {
    kafkaProducer.close()
    stockPriceConsumer.close()
    stockLendingConsumer.close()
  }
} 