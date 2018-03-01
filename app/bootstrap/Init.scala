package bootstrap

import scala.concurrent.Future
import javax.inject.Inject
import javax.inject._
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

import org.apache.spark.sql.SparkSession
import play.api._

import org.apache.spark.sql.types._

@Singleton
class Init @Inject() (lifecycle: ApplicationLifecycle, config: Configuration) {

  
  /**
   * On start load the  SparkSession
   */
  private var sparkSession = SparkSession.builder
      .master(config.getString("spark.master").get)
      .appName(config.getString("spark.app.name").get)
      .config("spark.jars", config.getString("spark.jars").get)
      .config("spark.hadoop.fs.s3a.endpoint",config.getString("spark.hadoop.fs.s3a.endpoint").get)
      .config("spark.hadoop.fs.s3a.access.key",config.getString("spark.hadoop.fs.s3a.access.key").get)
      .config("spark.hadoop.fs.s3a.secret.key",config.getString("spark.hadoop.fs.s3a.secret.key").get)
      .config("spark.hadoop.fs.s3a.path.style.access",config.getString("spark.hadoop.fs.s3a.path.style.access").get)
      .config("spark.hadoop.fs.s3a.impl",config.getString("spark.hadoop.fs.s3a.impl").get)
      .config("spark.hadoop.fs.AbstractFileSystem.s3a.impl", config.getString("spark.hadoop.fs.AbstractFileSystem.s3a.impl").get)      
      .getOrCreate() 
  Logger.info("Starting VDCMethods application")
  
  lifecycle.addStopHook { () =>
    Logger.info("Stopping VDCMethods application")
    Future.successful(sparkSession.stop())    
  }

  def getSparkSessionInstance = {
    sparkSession
  }
}


