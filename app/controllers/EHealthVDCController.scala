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

import bootstrap.Init
import io.swagger.annotations._
import javax.inject.Inject
import org.apache.spark.sql.SparkSession
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future

import scala.concurrent.ExecutionContext


// TODO thread pool!!!
@Api("EHealthVDCController")
class EHealthVDCController @Inject() (config: Configuration, initService: Init, ws: WSClient) extends InjectedController {
  private val LOGGER = LoggerFactory.getLogger("EHealthVDCController")

  @ApiOperation(nickname = "getPatientBiographicalData",
    value = "Get patient's biographical data",
    notes = "This method returns the biographical data for the specified patient (identified via SSN), to be used by medical doctors",
    response = classOf[models.Patient], responseContainer = "List", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Patient not found")))
  def getPatientDetails(
                         @ApiParam(value = "SSN", required = true,
                           allowMultiple = false) socialId: String,
                         @ApiParam(value = "RequesterId", required = true,
                           allowMultiple = false) requesterId: String) = Action.async {
    implicit request =>
      val spark = initService.getSparkSessionInstance
      val patientSSN = socialId
      val query: String = ("SELECT socialId as SSN, name, surname, gender, birthDate, addressCity, addressRoad, " +
        "addressRoadNumber, addressPostalCode, addressTelephoneNumber, birthCity, nationality, job, schoolYears  " +
        "FROM patientsProfiles WHERE socialId=\"%s\"").format(patientSSN)

      val data = Json.obj(
        "query" -> query,
        "purpose" -> "Treatment",
        "access" -> "read",
        "requester" -> "",
        "blueprintId" -> "",
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
  def getTestValues(@ApiParam(value = "SSN", required = true, allowMultiple = false) socialId: String,
                    @ApiParam(value = "requesterId", required = true,
                      allowMultiple = false) requesterId: String,
                    @ApiParam(value = "component", required = true,
                      allowMultiple = false) testType: String)= Action.async {
    implicit request =>
      val spark = initService.getSparkSessionInstance
      val patientSSN = socialId
      var origtestType = testType
      var newTestType: String = null
      if (origtestType.equals("cholesterol")) {
        newTestType = "cholesterol_hdl_value, cholesterol_hdl_value, cholesterol_tryglicerides_value, cholesterol_total_value"
      } else {
        newTestType = "%s_value".format(origtestType).replaceAll("\\.", "_")
      }
      val queryToEngine = "SELECT patientId, date, %s FROM blood_tests".format(testType)
      val data = Json.obj(
        "query" -> queryToEngine,
        "purpose" -> "Treatment",
        "access" -> "read",
        "requester" -> "",
        "blueprintId" -> "",
        "requesterId" -> requesterId
      )
      if (config.has("policy.enforcement.play.url")) {
        val url: String = config.get[String]("policy.enforcement.play.url")

        val futureResponse = ws.url(url).addHttpHeaders("Content-Type" -> "application/json")
          .addHttpHeaders("Accept" -> "application/json").withRequestTimeout(Duration.Inf).post(data)

        val res = Await.result(futureResponse, 100 seconds)
        val resultStr = ProcessResultsUtils.getBloodTestsTestTypeCompilantResult(spark, res.body[String].toString,
          config, newTestType, patientSSN)

        Future.successful(Ok(resultStr))
      } else {
        Future.successful(NotFound("Missing url"))
      }
  }


  @ApiOperation(nickname = "getLastValuesForBloodTest",
    value = "Get patient's latest values for all measured components",
    notes = "This method returns the latest values of all the blood test components measured on a patient (identified by his SSN), to be used by medical doctors",
    response = classOf[models.Patient], responseContainer = "List", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Patient not found")))
  def getAllTestValues(@ApiParam(value = "SSN", required = true,
    allowMultiple = false) socialId: String,
                       @ApiParam(value = "requesterId", required = true,
                         allowMultiple = false) requesterId: String) = Action.async {
    implicit request =>
      val spark = initService.getSparkSessionInstance
      val queryObject = request.body
      val patientSSN = socialId
      var origtestType:String = "antithrombin.value, cholesterol.hdl.value, cholesterol.ldl.value, " +
        "cholesterol.total.value, cholesterol.tryglicerides.value , fibrinogen.value , haemoglobin.value , " +
        "plateletCount.value, prothrombinTime.value, totalWhiteCellCount.value  "

      var testType:String = origtestType.replaceAll("\\.","_")
      val queryToEngine = "SELECT date, patientId, %s FROM blood_tests".format(testType)

      val data = Json.obj(
        "query" -> queryToEngine,
        "purpose" -> "Treatment",
        "access" -> "read",
        "requester" -> "",
        "blueprintId" -> "",
        "requesterId" -> requesterId
      )
      if (config.has("policy.enforcement.play.url")) {
        val url: String = config.get[String]("policy.enforcement.play.url")

        val futureResponse = ws.url(url).addHttpHeaders("Content-Type" -> "application/json")
          .addHttpHeaders("Accept" -> "application/json").withRequestTimeout(Duration.Inf).post(data)

        val res = Await.result(futureResponse, 100 seconds)
        val resultStr = ProcessResultsUtils.getAllBloodTestsTestTypeCompilantResult(spark, res.body[String].toString,
          config, testType, patientSSN)
        println ("revita" + resultStr)
        Future.successful(Ok(resultStr))
      } else {
        Future.successful(NotFound("Missing url"))
      }
  }

  @ApiOperation(nickname = "getBloodTestComponentAverage",
    value = "Get average of component over an age range",
    notes =  "This method returns the average value for a specific blood test component in a specific age range, to be used by researchers. Since data are for researchers, patients' identifiers and quasi-identifiers won't be returned, making the output of this method anonymized.",
    response = classOf[models.Patient], responseContainer = "List", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Component never measured")))
  def getTestAverage(@ApiParam(value = "component", required = true,
    allowMultiple = false) testType: String,
                     @ApiParam(value = "startAgeRange", required = true,
                       allowMultiple = false) startAgeRange: Int,
                     @ApiParam(value = "endAgeRange", required = true,
                       allowMultiple = false) endAgeRange: Int) = Action.async {
    implicit request =>
      val spark = initService.getSparkSessionInstance
      val queryObject = request.body
      var origtestType:String = testType
      var newTestType:String = null
      var avgTestType:String = null
      if (origtestType.equals("cholesterol")) {
        newTestType = "cholesterol_hdl_value, cholesterol_hdl_value, cholesterol_tryglicerides_value, cholesterol_total_value"
        avgTestType = "AVG(cholesterol_hdl_value), AVG(cholesterol_hdl_value), AVG(cholesterol_tryglicerides_value), AVG(cholesterol_total_value)"
      } else {
        newTestType = "%s_value".format(origtestType).replaceAll("\\.", "_")
        avgTestType = "AVG("+newTestType+")"
      }

      val queryToEngine = "SELECT patientId, date, %s FROM blood_tests".format(testType)
      val data = Json.obj(
        "query" -> queryToEngine,
        "purpose" -> "Research",
        "access" -> "read",
        "requester" -> "",
        "blueprintId" -> "",
        "requesterId" -> ""
      )
      if (config.has("policy.enforcement.play.url")) {
        val url: String = config.get[String]("policy.enforcement.play.url")

        val futureResponse = ws.url(url).addHttpHeaders("Content-Type" -> "application/json")
          .addHttpHeaders("Accept" -> "application/json").withRequestTimeout(Duration.Inf).post(data)

        val res = Await.result(futureResponse, 100 seconds)
        val resultStr = ProcessResultsUtils.getAvgBloodTestsTestTypeCompilantResult(spark, res.body[String].toString,
          config, newTestType, avgTestType, startAgeRange, endAgeRange)

        Future.successful(Ok(resultStr))
      } else {
        Future.successful(NotFound("Missing url"))
      }
  }
}




