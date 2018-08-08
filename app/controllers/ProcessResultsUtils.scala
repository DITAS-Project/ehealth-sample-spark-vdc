package controllers

import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

object ProcessResultsUtils extends Serializable {
  private val LOGGER = LoggerFactory.getLogger("ProcessResultsUtils")
  val debugMode = true

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
    println(dataConfigName)
    val connInfo = config.get[String](dataConfigName)
    if (connInfo.contains("s3a")) {
      var dataDF: DataFrame = null

      dataDF = spark.read.parquet(connInfo)
      return dataDF
    }
    val url = config.get[String]("db.mysql.url")
    val user = config.get[String]("db.mysql.username")
    val pass = config.get[String]("db.mysql.password")
    var jdbcDF = spark.read.format("jdbc").option("url", url).option("dbtable", connInfo).
      option("user", user).option("password", pass).load
    return jdbcDF
  }


  def handleTable (spark: SparkSession, config: Configuration,
                   dataConfigName: String) : Unit = {
    LOGGER.info("handleTable")
    var tableDF = loadTableDFFromConfig(null, spark, config, dataConfigName)
    var sparkName = dataConfigName.toString()
    if (dataConfigName.toString().contains("clauses")) {
      sparkName = "clauses"
    }
    tableDF.createOrReplaceTempView(sparkName)
    if (debugMode) {
      println("============= " + sparkName + " ===============")
      tableDF.show()
    }
  }




  def getCompilantResult (spark: SparkSession, query:String, config: Configuration): String =
  {

    val json: JsValue = Json.parse(query)
    val table: String = new String("table")
    var index: Integer = 0;
    var cond = true;
    while (cond) {
      var tableKey = table + index.toString
      index = index + 1
      val tableConfigName = (json \ tableKey).validate[String]
      tableConfigName match {
        case s: JsSuccess[String] => handleTable(spark, config, s.get);
        case e: JsError => cond = false
      }
    }
    val newQuery = (json \ "newQuery").validate[String]
    val resultDataDF = spark.sql(newQuery.get).toDF().filter(row => anyNotNull(row))
    resultDataDF.toJSON.collect.mkString("[", ",", "]")

  }
}