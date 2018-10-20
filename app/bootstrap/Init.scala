package bootstrap

import scala.concurrent.Future

import org.apache.spark.sql.SparkSession

import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration
import play.api.Logger
import play.api.inject.ApplicationLifecycle

@Singleton
class Init @Inject() (lifecycle: ApplicationLifecycle, config: Configuration) {
  /**
    * On start load the  SparkSession
    */
  private var sparkSession = SparkSession.builder
    .master(config.get[String]("spark.master"))
    .appName(config.get[String]("spark.app.name"))
    .config("spark.jars", config.get[String]("spark.jars"))
    .config("spark.sql.shuffle.partitions", config.get[String]("spark.sql.shuffle.partitions"))
    .config("spark.hadoop.fs.s3a.endpoint", config.get[String]("spark.hadoop.fs.s3a.endpoint"))
    .config("spark.hadoop.fs.s3a.access.key", config.get[String]("spark.hadoop.fs.s3a.access.key"))
    .config("spark.hadoop.fs.s3a.secret.key", config.get[String]("spark.hadoop.fs.s3a.secret.key"))
    .config("spark.hadoop.fs.s3a.path.style.access", config.get[String]("spark.hadoop.fs.s3a.path.style.access"))
    .config("spark.hadoop.fs.s3a.impl", config.get[String]("spark.hadoop.fs.s3a.impl"))
    .config("spark.hadoop.fs.AbstractFileSystem.s3a.impl", config.get[String]("spark.hadoop.fs.AbstractFileSystem.s3a.impl"))
    .getOrCreate()

  private var debugMode = false
  private var showDataFrameLength = 10
  private var enforcementEngineURL = ""
  if (config.has("policy.enforcement.play.url")) {
    enforcementEngineURL = config.get[String]("policy.enforcement.play.url")
  }
  if (config.has("debug.mode")) {
    debugMode = config.get[Boolean]("debug.mode")
  }
  if (config.has("df.show.len")) {
    showDataFrameLength = config.get[Int]("df.show.len")
  }

  Logger.info("Starting VDCMethods application")

  lifecycle.addStopHook { () =>
    sparkSession.stop()
    Logger.info("Stopping VDCMethods application")
    Future.successful(sparkSession.stop())
  }

  def getSparkSessionInstance = {
    sparkSession
  }

  def getDebugMode = {
    debugMode
  }

  def getDfShowLen = {
    showDataFrameLength
  }

  def getEnforcementEngineURL = {
    enforcementEngineURL
  }
}
