package com.stocktrading

import com.stocktrading.kafka.{KafkaConfig, KafkaProducerService, KafkaConsumerService}
import com.stocktrading.db.DatabaseService
import com.stocktrading.db.DatabaseServiceConfig
import com.stocktrading.model.{Stock, StockPrice, StockLending}
import com.typesafe.scalalogging.LazyLogging

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration._
import scala.util.{Success, Failure}

object StockTradingApp extends App with LazyLogging {
  // Initialize services
  DatabaseService.setConfig(new DatabaseServiceConfig(
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
  
  // Process stock price updates
  def processStockPrices(): Unit = {
    val prices = stockPriceConsumer.poll()
    prices.foreach { price =>
      dbService.getStock(price.symbol) match {
        case Success(Some(stock)) =>
          val updatedStock = stock.copy(
            currentPrice = price.price,
            lastUpdated = price.timestamp
          )
          dbService.saveStock(updatedStock) match {
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
  def processStockLending(): Unit = {
    val lendings = stockLendingConsumer.poll()
    lendings.foreach { lending =>
      dbService.saveStockLending(lending) match {
        case Success(_) => logger.info(s"Processed lending request for ${lending.symbol}")
        case Failure(ex) => logger.error(s"Failed to process lending request: ${ex.getMessage}")
      }
    }
  }
  
  // Main processing loop
  def run(): Unit = {
    logger.info("Starting Stock Trading Application")
    
    while (true) {
      processStockPrices()
      processStockLending()
      Thread.sleep(1000) // Process every second
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