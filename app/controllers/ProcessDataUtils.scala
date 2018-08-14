package controllers

import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.libs.json._ // JSON library
import play.api.libs.json.Reads._ // Custom validation helpers
import play.api.libs.functional.syntax._ // Combinator syntax

object ProcessDataUtils extends Serializable {
  private val LOGGER = LoggerFactory.getLogger("ProcessResultsUtils")
  var debugMode = false

  def setDebugMode (debug: Boolean) : Unit = {
    debugMode = debug;
  }

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
    if (connInfo.contains("s3a")) {
      var dataDF: DataFrame = null

      dataDF = spark.read.parquet(connInfo)
      return dataDF
    }
    //Use jdbc connection:
    val url = config.get[String]("db.mysql.url")
    val user = config.get[String]("db.mysql.username")
    val pass = config.get[String]("db.mysql.password")
    var jdbcDF = spark.read.format("jdbc").option("url", url).option("dbtable", connInfo).
      option("user", user).option("password", pass).load
    return jdbcDF
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

  def createJoinDataFrame (spark: SparkSession, query:String, config: Configuration, testType: String): Unit = {

    val json: JsValue = Json.parse(query)
    val table: String = new String("table")
    var index: Integer = 0;
    var cond = true;
    while (cond) {
      var tableKey = table + index.toString
      index = index + 1
      val tableConfigName = (json \ tableKey).validate[String]
      tableConfigName match {
        case s: JsSuccess[String] => addTableToSpark(spark, config, s.get);
        case e: JsError => cond = false
      }
    }
    val newQuery = (json \ "newQuery").validate[String]
    if (debugMode) {
      println("the re-written query: " + newQuery.get)
    }
    val bloodTestsDF = spark.sql(newQuery.get).toDF().filter(row => anyNotNull(row))
    if (debugMode) {
      bloodTestsDF.show(false)
    }
    val profilesDF = loadTableDFFromConfig(null, spark, config, "patientsProfiles")
    if (debugMode) {
      profilesDF.show(false)
    }
    //TODO: check if inner join can be applied
    var joinedDF = bloodTestsDF.join(profilesDF, bloodTestsDF.col(Constants.SUBJECT_ID_COL_NAME).
      equalTo(profilesDF.col(Constants.SUBJECT_ID_COL_NAME)), "left_outer")
    joinedDF = joinedDF.drop(profilesDF.col(Constants.SUBJECT_ID_COL_NAME))
    joinedDF.createOrReplaceTempView("joined")
    if (debugMode) {
      joinedDF.show(false)
    }
  }

  def getBloodTestsComponentCompilantResult (spark: SparkSession, query:String, config: Configuration, testType: String,
                                             patientSSN: String): String = {

    createJoinDataFrame(spark, query, config, testType)
    val queryOnJoinTables = "SELECT patientId, date, %s FROM joined WHERE socialId=\"%s\"".format(testType, patientSSN)
    var patientBloodTestsDF = spark.sql(queryOnJoinTables).toDF().filter(row => anyNotNull(row))
    if (debugMode) {
      patientBloodTestsDF.limit(10).show(false)
      patientBloodTestsDF.printSchema
      patientBloodTestsDF.explain(true)
    }
    //Adjust output to blueprint
    patientBloodTestsDF = patientBloodTestsDF.withColumnRenamed(testType, "value").drop(Constants.SUBJECT_ID_COL_NAME).distinct()
    patientBloodTestsDF.toJSON.collect.mkString("[", ",", "]")
  }

  def getAvgBloodTestsTestTypeCompilantResult (spark: SparkSession, query:String, config: Configuration, testType: String, avgTestType: String,
                                               origtestType: String, startAgeRange: Int, endAgeRange: Int) : String = {
    val todayDate =  java.time.LocalDate.now
    val minBirthDate = todayDate.minusYears(endAgeRange)
    val maxBirthDate = todayDate.minusYears(startAgeRange)
    createJoinDataFrame(spark, query, config, testType)

    val queryOnJoinTables = "SELECT "+avgTestType+" FROM joined where birthDate > \""+minBirthDate+"\" AND birthDate < \""+maxBirthDate +"\""
    var patientBloodTestsDF = spark.sql(queryOnJoinTables).toDF().filter(row => anyNotNull(row))
    if (debugMode) {
      println ("Range: " + startAgeRange + " " + endAgeRange)
      println (queryOnJoinTables)
      patientBloodTestsDF.limit(10).show(false)
      patientBloodTestsDF.printSchema
      patientBloodTestsDF.explain(true)
    }
    //Adjust output to blueprint
    patientBloodTestsDF = patientBloodTestsDF.withColumnRenamed(avgTestType, "value")
    patientBloodTestsDF.toJSON.collect.mkString(",")
  }
}
