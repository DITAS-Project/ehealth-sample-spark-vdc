package controllers

import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

object ProcessResultsUtils extends Serializable {
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
      tableDF.show(false)
    }
  }




  def getPatientDetailsCompilantResult (spark: SparkSession, query:String, config: Configuration): String =
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
        case s: JsSuccess[String] => handleTable(spark, config, s.get);
        case e: JsError => cond = false
      }
    }
    val newQuery = (json \ "newQuery").validate[String]
    val bloodTestsDF = spark.sql(newQuery.get).toDF().filter(row => anyNotNull(row))
    if (debugMode) {
      bloodTestsDF.show(1000)
    }
    val profilesDF = loadTableDFFromConfig(null, spark, config, "patientsProfiles")
    if (debugMode) {
      profilesDF.show()
    }
    //val joinedDF = bloodTestsDF.join(profilesDF, "patientId", "left_outer")
    var joinedDF = bloodTestsDF.join(profilesDF, bloodTestsDF.col(Constants.SUBJECT_ID_COL_NAME).equalTo(profilesDF.col(Constants.SUBJECT_ID_COL_NAME)), "left_outer")
    joinedDF = joinedDF.drop(profilesDF.col(Constants.SUBJECT_ID_COL_NAME))
    joinedDF.createOrReplaceTempView("joined")
    if (debugMode) {
      joinedDF.show(100)
    }
  }

  def getAllBloodTestsTestTypeCompilantResult (spark: SparkSession, query:String, config: Configuration, testType: String,
                                               patientSSN: String): String = {

    createJoinDataFrame(spark, query, config, testType)
    val queryOnJoinTables = "WITH joined AS (SELECT %s , patientId, date, socialId FROM joined S WHERE date=(SELECT max(DATE) FROM joined where socialId=S.socialId)) SELECT date, socialId, %s FROM joined WHERE socialId=\"%s\"".format(testType, testType, patientSSN);
    val patientBloodTestsDF = spark.sql(queryOnJoinTables).toDF().filter(row => anyNotNull(row))
    if (debugMode) {
      println ("----------------------------")
      println(queryOnJoinTables);
      patientBloodTestsDF.limit(10).show(false)
      patientBloodTestsDF.printSchema
      //      patientBloodTestsDF.explain(true)
    }
    patientBloodTestsDF.toJSON.collect.mkString("[", ",", "]")
  }

  def getBloodTestsTestTypeCompilantResult (spark: SparkSession, query:String, config: Configuration, testType: String,
                                            patientSSN: String): String = {

    createJoinDataFrame(spark, query, config, testType)
    val queryOnJoinTables = "SELECT patientId, date, %s FROM joined WHERE socialId=\"%s\"".format(testType, patientSSN)
    val patientBloodTestsDF = spark.sql(queryOnJoinTables).toDF().filter(row => anyNotNull(row))
    if (debugMode) {
      patientBloodTestsDF.limit(10).show(false)
      patientBloodTestsDF.printSchema
      patientBloodTestsDF.explain(true)
    }
    patientBloodTestsDF.toJSON.collect.mkString("[", ",", "]")
  }

  def getAvgBloodTestsTestTypeCompilantResult (spark: SparkSession, query:String, config: Configuration, testType: String, avgTestType: String,
                                               startAgeRange: Long, endAgeRange: Long) : String = {
    val todayDate =  java.time.LocalDate.now
    val minBirthDate = todayDate.minusYears(endAgeRange)
    val maxBirthDate = todayDate.minusYears(startAgeRange)
    createJoinDataFrame(spark, query, config, testType)

    val queryOnJoinTables = "SELECT "+avgTestType+" FROM joined where birthDate > \""+minBirthDate+"\" AND birthDate < \""+maxBirthDate +"\""
    val patientBloodTestsDF = spark.sql(queryOnJoinTables).toDF().filter(row => anyNotNull(row))
    if (debugMode) {
      println (queryOnJoinTables)
      patientBloodTestsDF.limit(10).show(false)
      patientBloodTestsDF.printSchema
      patientBloodTestsDF.explain(true)
    }
    patientBloodTestsDF.show(false)
    patientBloodTestsDF.toJSON.collect.mkString("[", ",", "]")
  }

}
