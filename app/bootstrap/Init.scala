package bootstrap

import scala.concurrent.Future

import org.apache.spark.sql.SparkSession

import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import controllers.EHealthVDCController

@Singleton
class Init @Inject() (lifecycle: ApplicationLifecycle, config: Configuration) {

  /**
    * On start load the  SparkSession
    */
  private var sparkSession = SparkSession.builder
    .master(config.get[String]("spark.master"))
    .appName(config.get[String]("spark.app.name"))
    .config("spark.jars", config.get[String]("spark.jars"))
    .config("spark.hadoop.fs.s3a.endpoint", config.get[String]("spark.hadoop.fs.s3a.endpoint"))
    .config("spark.hadoop.fs.s3a.access.key", config.get[String]("spark.hadoop.fs.s3a.access.key"))
    .config("spark.hadoop.fs.s3a.secret.key", config.get[String]("spark.hadoop.fs.s3a.secret.key"))
    .config("spark.hadoop.fs.s3a.path.style.access", config.get[String]("spark.hadoop.fs.s3a.path.style.access"))
    .config("spark.hadoop.fs.s3a.impl", config.get[String]("spark.hadoop.fs.s3a.impl"))
    .config("spark.hadoop.fs.AbstractFileSystem.s3a.impl", config.get[String]("spark.hadoop.fs.AbstractFileSystem.s3a.impl"))
    .getOrCreate()

  private var debugMode = false
  if (config.has("debug.mode")) {
    debugMode = config.get[Boolean]("debug.mode")
  }

  Logger.info("Starting VDCMethods application")

  lifecycle.addStopHook { () =>
    Logger.info("Stopping VDCMethods application")
    Future.successful(sparkSession.stop())
  }

  def getSparkSessionInstance = {
    sparkSession
  }

  def getDebugMode = {
    debugMode
  }

}
