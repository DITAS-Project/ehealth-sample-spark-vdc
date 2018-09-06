package controllers

import controllers.ProcessEnforcementEngineResponse.debugMode
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.slf4j.LoggerFactory
import play.api.Configuration

import scala.collection.mutable.Stack

object ProcessEnforcementEngineResponse {
  private val LOGGER = LoggerFactory.getLogger("ProcessEnforcementEngineResponse")
  var response : String = ""
  var query: String = ""
  var tableNames: Stack[String] = new Stack[String]()
  var queryOnTables: String = ""
  var debugMode: Boolean = false

  def processResponse (spark: SparkSession, config: Configuration, response:String, debugMode:Boolean, dfShowLen: Int): DataFrame = {
    this.debugMode = debugMode
    val json: JsValue = Json.parse(response)
    val table: String = new String("table")
    var index: Integer = 0;
    var cond = true;
    var tableKey: String = null
    while (cond) {
      tableKey = table + index.toString
      index = index + 1
      val tableConfigName = (json \ tableKey).validate[String]
      tableConfigName match {
        case s: JsSuccess[String] => DataFrameUtils.addTableToSpark(spark, config, s.get, dfShowLen)
        case e: JsError => cond = false
      }
    }
    //fail if there is an error parsing the Enforcement Engine
    if ((json \ tableKey).isDefined) {
      LOGGER.error("Error in processing enforcement Engine result")
      return spark.emptyDataFrame
    }
    val newQuery = (json \ "newQuery").validate[String]
    query = newQuery.get
    if (debugMode) {
      println("the re-written query: " + newQuery.get)
    }
    val bloodTestsDF: DataFrame = spark.sql(query).toDF().filter(row => DataFrameUtils.anyNotNull(row))
    if (debugMode) {
      println (query)
      bloodTestsDF.distinct().show(dfShowLen, false)
      bloodTestsDF.printSchema
      bloodTestsDF.explain(true)
    }
    bloodTestsDF
  }



}
