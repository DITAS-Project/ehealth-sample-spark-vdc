package controllers


import java.io.FileInputStream
import org.apache.commons.lang3.StringUtils


import bootstrap.Init
import io.swagger.annotations._
import javax.inject.Inject
import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.json._

import concurrent.Await
import concurrent.duration._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.util.Try
import javax.inject.Inject
import play.mvc._
import play.libs.ws.{WSRequest, _}
import java.util.concurrent.CompletionStage

import play.api.libs.ws.ahc.AhcWSClient
import akka.stream.ActorMaterializer
import akka.actor.ActorSystem
import models.RequestPatient
import org.yaml.snakeyaml.constructor.Constructor
import javax.inject.Inject

import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.http.HttpEntity
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext


// TODO thread pool!!!
@Api("EHealthVDCController")
class EHealthVDCController @Inject() (config: Configuration, initService: Init, ws: WSClient) extends InjectedController {
  private val LOGGER = LoggerFactory.getLogger("EHealthVDCController")
  val debugMode = true


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



  def getCompilantResult (spark: SparkSession, query:String, config: Configuration): Dataset[Row] =
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
    resultDataDF

  }

  def readData(spark: SparkSession): Unit = {
    val bloodTestsDF = spark.read.parquet(config.get[String]("s3.filename"))
    // Displays the content of the DataFrame to stdout
    bloodTestsDF.limit(5).show(false)
    bloodTestsDF.printSchema
    bloodTestsDF.createOrReplaceTempView("bloodTests")

    val table = config.get[String]("db.mysql.table")
    val user = config.get[String]("db.mysql.username")
    val password = config.get[String]("db.mysql.password")
    val jdbcConnectionString = config.get[String]("db.mysql.url")

    val patientsDF = spark.read.format("jdbc")
      .option("url", jdbcConnectionString)
      .option("dbtable", table)
      .option("user", user)
      .option("password", password)
      .load()
    patientsDF.limit(5).show(false)
    patientsDF.printSchema
    patientsDF.createOrReplaceTempView("patients")

    val joinedDF = bloodTestsDF.join(patientsDF, "patientId")
    joinedDF.createOrReplaceTempView("joined")
  }

  @ApiOperation(nickname = "getPatientBiographicalData",
    value = "Get patient's biographical data",
    notes = "This method returns the biographical data for the specified patient (identified via SSN), to be used by medical doctors",
    response = classOf[models.Patient], responseContainer = "List", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Patient not found")))
  def getPatientDetails = Action.async(parse.json[RequestPatient]) { request =>
    val spark = initService.getSparkSessionInstance
    val queryObject = request.body
    val patientSSN = queryObject.patientSSN
    val requesterId = queryObject.requesterId
    val query: String = ("SELECT socialId as SSN, name, surname, gender, birthDate, addressCity, addressRoad, " +
      "addressRoadNumber, addressPostalCode, addressTelephoneNumber, birthCity, nationality, job, schoolYears  " +
      "FROM patientsProfiles WHERE socialId=\"%s\"").format(patientSSN)

    val data = Json.obj(
      "query" -> query,
      "purpose" -> "Treatment",
      "access" -> "read",
      "requester" -> "r1",
      "blueprintId" -> "2",
      "requesterId" -> requesterId
    )
    if (config.has("policy.enforcement.play.url")) {
      val url: String = config.get[String]("policy.enforcement.play.url")

      val futureResponse = ws.url(url).addHttpHeaders("Content-Type" -> "application/json")
        .addHttpHeaders("Accept" -> "application/json").withRequestTimeout(Duration.Inf).post(data)

      val res = Await.result(futureResponse, 100 seconds)
      val resultDF = getCompilantResult(spark, res.body[String].toString, config)

      Future.successful(Ok(resultDF.toJSON.collect.mkString("[", ",", "]")))
    }else {
      Future.successful(NotFound("Missing url"))
    }
  }

}


