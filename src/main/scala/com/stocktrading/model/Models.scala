package com.stocktrading.model

import java.time.Instant
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class Stock(
  symbol: String,
  name: String,
  currentPrice: BigDecimal,
  lastUpdated: Instant
)

case class StockPrice(
  symbol: String,
  price: BigDecimal,
  timestamp: Instant
)

case class StockLending(
  id: String,
  symbol: String,
  userId: String,
  quantity: Int,
  startDate: Instant,
  endDate: Option[Instant],
  status: LendingStatus
)

sealed trait LendingStatus
case object Active extends LendingStatus
case object Returned extends LendingStatus
case object Overdue extends LendingStatus

object Models {
  implicit val stockEncoder: Encoder[Stock] = deriveEncoder[Stock]
  implicit val stockDecoder: Decoder[Stock] = deriveDecoder[Stock]
  
  implicit val stockPriceEncoder: Encoder[StockPrice] = deriveEncoder[StockPrice]
  implicit val stockPriceDecoder: Decoder[StockPrice] = deriveDecoder[StockPrice]
  
  implicit val stockLendingEncoder: Encoder[StockLending] = deriveEncoder[StockLending]
  implicit val stockLendingDecoder: Decoder[StockLending] = deriveDecoder[StockLending]
  
  implicit val lendingStatusEncoder: Encoder[LendingStatus] = deriveEncoder[LendingStatus]
  implicit val lendingStatusDecoder: Decoder[LendingStatus] = deriveDecoder[LendingStatus]
} 