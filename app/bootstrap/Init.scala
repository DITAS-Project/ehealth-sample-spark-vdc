package bootstrap

import org.apache.spark.sql.SparkSession
import play.api._

import org.apache.spark.sql.types._

object Init extends GlobalSettings {

  var sparkSession: SparkSession = _

  /**
   * On start create the Spark session
   */
  override def onStart(app: Application) {
    sparkSession = SparkSession.builder
      .master("spark://SPARK_HOSTNAME:7077")
      .appName("VDCMethods")
      .config("spark.jars", "connector/mysql-connector-java-5.1.45/mysql-connector-java-5.1.45-bin.jar,connector/hadoop-aws-3.0.0.jar,connector/aws-java-sdk-bundle-1.11.271.jar")
      .config("spark.hadoop.fs.s3a.endpoint","http://MINIO_HOSTNAME:9000")
      .config("spark.hadoop.fs.s3a.access.key",ACCESS_KEY)
      .config("spark.hadoop.fs.s3a.secret.key",SECRET_KEY)
      .config("spark.hadoop.fs.s3a.path.style.access","true")
      .config("spark.hadoop.fs.s3a.impl","org.apache.hadoop.fs.s3a.S3AFileSystem")
      .config("spark.hadoop.fs.AbstractFileSystem.s3a.impl", "org.apache.hadoop.fs.s3a.S3A")
      .getOrCreate()

    println("Starting VDCMethods application")

  }

  /**
   * On stop clear the sparksession
   */
  override def onStop(app: Application) {
    println("Stopping VDCMethods application")
    sparkSession.stop()
  }

  def getSparkSessionInstance = {
    sparkSession
  }
}


