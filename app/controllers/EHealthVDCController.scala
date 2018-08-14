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

  private def sendRequestToEnforcmentEngine (query:String, purpose: String, requesterId: String, url:String): String = {
    val data = Json.obj(
      "query" -> query,
      "purpose" -> purpose,
      "access" -> "read",
      "requester" -> "",
      "blueprintId" -> "",
      "requesterId" -> requesterId
    )
    val futureResponse = ws.url(url).addHttpHeaders("Content-Type" -> "application/json")
      .addHttpHeaders("Accept" -> "application/json").withRequestTimeout(Duration.Inf).post(data)

    val res = Await.result(futureResponse, 100 seconds)
    res.body[String]
  }

  @ApiOperation(nickname = "getAllValuesForBloodTestComponent",
    value = "Get timeseries of patient's blood test component",
    notes =  "This method returns the collected values for a specific blood test component of a patient (identified by his SSN), to be used by medical doctors",
    response = classOf[models.BloodTestComponentValue], responseContainer = "List", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Patient not found")))
  def getAllValuesForBloodTestComponent(@ApiParam(value = "SSN", required = true, allowMultiple = false) socialId: String,
                                        @ApiParam(value = "component", required = true,
                                          allowMultiple = false) testType: String)= Action.async {
    implicit request =>
      val spark = initService.getSparkSessionInstance
      val patientSSN = socialId
      var origtestType = testType
      var newTestType: String = null
      if (origtestType.equals("cholesterol")) {
        newTestType = "cholesterol_total_value"
      } else {
        newTestType = "%s_value".format(origtestType)
      }
      val queryToEngine = "SELECT patientId, date, %s FROM blood_tests".format(newTestType)

      if (config.has("policy.enforcement.play.url")) {
        val url: String = config.get[String]("policy.enforcement.play.url")
        val response = sendRequestToEnforcmentEngine(queryToEngine, request.headers("Purpose"), request.headers("RequesterId"), url)

        val resultStr = ProcessDataUtils.getBloodTestsComponentCompilantResult(spark, response,
          config, newTestType, patientSSN)
        val json: JsValue = Json.parse(resultStr)

        Future.successful(Ok(json))
      } else {
        Future.successful(NotFound("Missing url"))
      }
  }


  @ApiOperation(nickname = "getBloodTestComponentAverage",
    value = "Get average of component over an age range",
    notes =  "This method returns the average value for a specific blood test component in a specific age range, to be used by researchers. Since data are for researchers, patients' identifiers and quasi-identifiers won't be returned, making the output of this method anonymized.",
    response = classOf[models.ComponentAvg], responseContainer = "List", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Component never measured")))
  def getBloodTestComponentAverage(@ApiParam(value = "component", required = true,
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
        newTestType = "cholesterol_total_value"
        avgTestType = "avg(cholesterol_total_value)"
      } else {
        newTestType = "%s_value".format(origtestType).replaceAll("\\.", "_")
        avgTestType = "avg("+newTestType+")"
      }

      val queryToEngine = "SELECT patientId, date, %s FROM blood_tests".format(newTestType)

      if (config.has("policy.enforcement.play.url")) {
        val url: String = config.get[String]("policy.enforcement.play.url")
        val response = sendRequestToEnforcmentEngine(queryToEngine, request.headers("Purpose"), "", url)

        val newJsonObj = ProcessDataUtils.getAvgBloodTestsTestTypeCompilantResult(spark, response,
          config, newTestType, avgTestType, origtestType, startAgeRange, endAgeRange)

        val json: JsValue = Json.parse(newJsonObj)

        Future.successful(Ok(json))
      } else {
        Future.successful(NotFound("Missing config file"))
      }
  }
}




