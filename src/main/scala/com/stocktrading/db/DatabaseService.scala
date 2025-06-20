package com.stocktrading.db

import com.stocktrading.model.{Active, LendingStatus, Overdue, Returned, Stock, StockLending}
import com.typesafe.scalalogging.LazyLogging

import java.sql.{Connection, DriverManager, PreparedStatement, ResultSet}
import java.time.Instant
import scala.util.{Failure, Success, Try}

case class  DatabaseServiceConfig(
  url: String,
  user: String,
  password: String
)

class DatabaseService extends LazyLogging {

  private var config: Option[DatabaseServiceConfig] = None

  def setConfig(dbConfig: DatabaseServiceConfig): Unit = {
    config = Option(dbConfig)
  }
  
  private def getConnection: Try[Connection] = Try {
    Class.forName("com.ibm.db2.jcc.DB2Driver")
    config match{
      case Some(cfg) => 
        DriverManager.getConnection(cfg.url, cfg.user, cfg.password)
      case None => 
        throw new IllegalStateException("Database config is not set")
    }
  }
  
  def saveStock(stock: Stock): Try[Unit] = {
    val query = """
    UPDATE stocks
    SET name = ?, current_price = ?, last_updated = ?
    WHERE symbol = ?
  """
    
    getConnection.flatMap { conn =>
      Try {
        val stmt = conn.prepareStatement(query)
        stmt.setString(1, stock.name)
        stmt.setBigDecimal(2, stock.currentPrice.bigDecimal)
        stmt.setTimestamp(3, java.sql.Timestamp.from(stock.lastUpdated))
        stmt.setString(4, stock.symbol)
        val rowsUpdated = stmt.executeUpdate()
        if (rowsUpdated == 0) {
          logger.info("Stock not found, inserting new record to STOCKS table")
          val insertQuery = """
          INSERT INTO stocks (symbol, name, current_price, last_updated)
          VALUES (?, ?, ?, ?)
          """
          val insertStmt = conn.prepareStatement(insertQuery)
          insertStmt.setString(1, stock.symbol)
          insertStmt.setString(2, stock.name)
          insertStmt.setBigDecimal(3, stock.currentPrice.bigDecimal)
          insertStmt.setTimestamp(4, java.sql.Timestamp.from(stock.lastUpdated))
          insertStmt.executeUpdate()
          insertStmt.close()
        }

        logger.info("Stock not found, inserting new record to STOCKS table")

        stmt.close()
        logger.info(s"Stock saved: ${stock.symbol}")
        conn.close()
      }
    }
  }
  
  def saveStockLending(lending: StockLending): Try[Unit] = {
    val query = """
      INSERT INTO stock_lending (id, symbol, user_id, quantity, start_date, end_date, status)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    """
    
    getConnection.flatMap { conn =>
      Try {
        val stmt = conn.prepareStatement(query)
        stmt.setString(1, lending.id)
        stmt.setString(2, lending.symbol)
        stmt.setString(3, lending.userId)
        stmt.setInt(4, lending.quantity)
        stmt.setTimestamp(5, java.sql.Timestamp.from(lending.startDate))
        stmt.setTimestamp(6, lending.endDate.map(d => java.sql.Timestamp.from(d)).orNull)
        stmt.setString(7, lending.status.toString)
        stmt.executeUpdate()
        stmt.close()
        conn.close()
      }
    }
  }
  
  def getStock(symbol: String): Try[Option[Stock]] = {
    val query = "SELECT * FROM stocks WHERE symbol = ?"
    
    getConnection.flatMap { conn =>
      Try {
        val stmt = conn.prepareStatement(query)
        stmt.setString(1, symbol)
        val rs = stmt.executeQuery()
        
        val result = if (rs.next()) {
          Some(Stock(
            symbol = rs.getString("symbol"),
            name = rs.getString("name"),
            currentPrice = BigDecimal(rs.getBigDecimal("current_price")),
            lastUpdated = rs.getTimestamp("last_updated").toInstant
          ))
        } else None
        
        rs.close()
        stmt.close()
        conn.close()
        result
      }
    }
  }
  
  def getActiveLendings(userId: String): Try[List[StockLending]] = {
    val query = "SELECT * FROM stock_lending WHERE user_id = ? AND status = 'Active'"
    
    getConnection.flatMap { conn =>
      Try {
        val stmt = conn.prepareStatement(query)
        stmt.setString(1, userId)
        val rs = stmt.executeQuery()
        
        val result = Iterator.continually(rs)
          .takeWhile(_.next())
          .map { row =>
            StockLending(
              id = row.getString("id"),
              symbol = row.getString("symbol"),
              userId = row.getString("user_id"),
              quantity = row.getInt("quantity"),
              startDate = row.getTimestamp("start_date").toInstant,
              endDate = Option(row.getTimestamp("end_date")).map(_.toInstant),
              status = row.getString("status") match {
                case "Active" => Active
                case "Returned" => Returned
                case "Overdue" => Overdue
              }
            )
          }.toList
        
        rs.close()
        stmt.close()
        conn.close()
        result
      }
    }
  }
} 