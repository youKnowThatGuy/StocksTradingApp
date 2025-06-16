name := "stock-trading-simulation"
version := "0.1.0"
scalaVersion := "2.13.10"

libraryDependencies ++= Seq(
  // Kafka
  "org.apache.kafka" %% "kafka-streams-scala" % "3.4.0",
  "org.apache.kafka" % "kafka-clients" % "3.4.0",
  
  // DB2
  "com.ibm.db2.jcc" % "db2jcc" % "db2jcc4",
  
  // JSON handling
  "io.circe" %% "circe-core" % "0.14.5",
  "io.circe" %% "circe-generic" % "0.14.5",
  "io.circe" %% "circe-parser" % "0.14.5",
  
  // Logging
  "ch.qos.logback" % "logback-classic" % "1.4.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  
  // Testing
  "org.scalatest" %% "scalatest" % "3.2.15" % Test
) 