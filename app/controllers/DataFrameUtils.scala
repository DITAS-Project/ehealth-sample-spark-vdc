package controllers

import controllers.ProcessEnforcementEngineResponse.debugMode
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.slf4j.LoggerFactory
import play.api.Configuration

object DataFrameUtils {
  private val LOGGER = LoggerFactory.getLogger("DataFrameUtils")

  def anyNotNull(row: Row): Boolean = {
    val len = row.length

    var i = 0
    var fieldNames = row.schema.fieldNames
    //print patientId if its the only col
    if (len == 1 && fieldNames(0).equals(Constants.SUBJECT_ID_COL_NAME))
      return true
    //skip patientId
    while (i < len) {
      if (!fieldNames(i).equals(Constants.SUBJECT_ID_COL_NAME) && !row.isNullAt(i)) {
        return true
      }
      i += 1
    }
    false
  }


  def loadTableDFFromConfig(tableFilePrefix : String, spark: SparkSession, config: Configuration,
                            dataConfigName: String): DataFrame = {
    LOGGER.info("PloadTableDFFromConfig")

    val connInfo = config.get[String](dataConfigName)
    val connTypeKey = dataConfigName+"_type"
    val connType = config.get[String](connTypeKey)
    if (connType.equals("s3a")) {
      var dataDF: DataFrame = null
      dataDF = spark.read.parquet(connInfo)
      return dataDF
    } else if (connType.equals("jdbc")) {
      //Use jdbc connection:
      val url = config.get[String]("db.mysql.url")
      val user = config.get[String]("db.mysql.username")
      val pass = config.get[String]("db.mysql.password")
      var jdbcDF = spark.read.format("jdbc").option("url", url).option("dbtable", connInfo).
        option("user", user).option("password", pass).load
      return jdbcDF
    }
    LOGGER.error("unrecognized data frame connection type")
    spark.emptyDataFrame
  }


  def addTableToSpark (spark: SparkSession, config: Configuration,
                       dataConfigName: String) : Unit = {
    LOGGER.info("addTableToSpark")
    var tableDF = loadTableDFFromConfig(null, spark, config, dataConfigName)
    var sparkName = dataConfigName.toString()
    if (dataConfigName.toString().contains(Constants.CLAUSES)) {
      sparkName = Constants.CLAUSES
    }
    tableDF.createOrReplaceTempView(sparkName)
    if (debugMode) {
      println("============= " + sparkName + " ===============")
      tableDF.show(false)
    }
  }

}
