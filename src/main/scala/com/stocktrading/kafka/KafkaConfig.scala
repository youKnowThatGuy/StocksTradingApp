package com.stocktrading.kafka

import com.stocktrading.model.Models.{stockLendingEncoder, stockPriceEncoder}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import com.stocktrading.model.{StockLending, StockPrice}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import io.circe.syntax._
import io.circe.parser._

import java.time.Duration
import java.util.Properties
import scala.jdk.CollectionConverters._

object KafkaConfig {
  val bootstrapServers = "localhost:9092"
  val stockPriceTopic = "stock-prices"
  val stockLendingTopic = "stock-lending"
  
  def createProducerProps(): Properties = {
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props
  }
  
  def createConsumerProps(groupId: String): Properties = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    props
  }
}

class KafkaProducerService {
  private val producer = new KafkaProducer[String, String](KafkaConfig.createProducerProps())
  
  def sendStockPrice(stockPrice: StockPrice): Unit = {
    val record = new ProducerRecord[String, String](
      KafkaConfig.stockPriceTopic,
      stockPrice.symbol,
      stockPrice.asJson.noSpaces
    )
    producer.send(record)
  }
  
  def sendStockLending(lending: StockLending): Unit = {
    val record = new ProducerRecord[String, String](
      KafkaConfig.stockLendingTopic,
      lending.id,
      lending.asJson.noSpaces
    )
    producer.send(record)
  }
  
  def close(): Unit = producer.close()
}

class KafkaConsumerService[T](topic: String, groupId: String)(implicit decoder: io.circe.Decoder[T]) {
  private val consumer = new KafkaConsumer[String, String](KafkaConfig.createConsumerProps(groupId))
  consumer.subscribe(java.util.Collections.singletonList(topic))
  
  def poll(duration: Duration = Duration.ofMillis(100)): List[T] = {
    val records = consumer.poll(duration).asScala.toList
    records.flatMap { record =>
      decode[T](record.value()).toOption
    }
  }
  
  def close(): Unit = consumer.close()
} 