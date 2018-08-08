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
import models.{RequestInfoForPatient,RequestBloodTestTypeForPatient}
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
  def getPatientDetails = Action.async(parse.json[RequestInfoForPatient]) { request =>
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
      val resultStr = ProcessResultsUtils.getPatientDetailsCompilantResult(spark, res.body[String].toString, config)

      Future.successful(Ok(resultStr))
    } else {
      Future.successful(NotFound("Missing url"))
    }
  }

  @ApiOperation(nickname = "getAllValuesForBloodTestComponent",
    value = "Get timeseries of patient's blood test component",
    notes =  "This method returns the collected values for a specific blood test component of a patient (identified by his SSN), to be used by medical doctors",
    response = classOf[models.Patient], responseContainer = "List", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Patient not found")))
  def getBloodTestValuesForTestType = Action.async(parse.json[RequestBloodTestTypeForPatient]) { request =>
    val spark = initService.getSparkSessionInstance
    val queryObject = request.body
    val patientSSN = queryObject.patientSSN
    val requesterId = queryObject.requesterId
    var origtestType:String = queryObject.bloodTestType
    var testType:String = null
    if (origtestType.equals("cholesterol")) {
      testType = "cholesterol_hdl_value, cholesterol_hdl_value, cholesterol_tryglicerides_value, cholesterol_total_value"
    } else {
      testType = "%s_value".format(origtestType).replaceAll("\\.", "_")
    }
    val queryToEngine = "SELECT patientId, date, %s FROM blood_tests".format(testType)
    val data = Json.obj(
      "query" -> queryToEngine,
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
      val resultStr = ProcessResultsUtils.getBloodTestsTestTypeCompilantResult(spark, res.body[String].toString,
        config, testType, patientSSN)

      Future.successful(Ok(resultStr))
    } else {
      Future.successful(NotFound("Missing url"))
    }
  }
  @ApiOperation(nickname = "getPatientBiographicalData",
    value = "Get patient's biographical data",
    notes = "This method returns the biographical data for the specified patient (identified via SSN), to be used by medical doctors",
    response = classOf[models.Patient], responseContainer = "List", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Patient not found")))
  def getAllValuesForBloodTest = Action.async(parse.json[RequestInfoForPatient]) { request =>
    val spark = initService.getSparkSessionInstance
    val queryObject = request.body
    val patientSSN = queryObject.patientSSN
    val requesterId = queryObject.requesterId
    var origtestType:String = "date, antithrombin.value, cholesterol.hdl.value, cholesterol.ldl.value, " +
      "cholesterol.total.value, cholesterol.tryglicerides.value , fibrinogen.value , haemoglobin.value , " +
      "plateletCount.value, prothrombinTime.value, totalWhiteCellCount.value  "

    var testType:String = origtestType.replaceAll("\\.","_")
    val queryToEngine = "SELECT patientId, %s FROM blood_tests".format(testType)

    val data = Json.obj(
      "query" -> queryToEngine,
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
      val resultStr = ProcessResultsUtils.getPatientDetailsCompilantResult(spark, res.body[String].toString, config)

      Future.successful(Ok(resultStr))
    } else {
      Future.successful(NotFound("Missing url"))
    }
  }
}




