package bootstrap

import scala.concurrent.Future

import org.apache.spark.sql.SparkSession

import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import controllers.ProcessResultsUtils

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
  Logger.info("Starting VDCMethods application")
  if (config.has("debug.mode"))
  ProcessResultsUtils.setDebugMode (config.get[Boolean]("debug.mode"))  

  lifecycle.addStopHook { () =>
    Logger.info("Stopping VDCMethods application")
    Future.successful(sparkSession.stop())
  }

  def getSparkSessionInstance = {
    sparkSession
  }
}


