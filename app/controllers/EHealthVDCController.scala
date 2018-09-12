package controllers


import java.io.FileInputStream

import org.apache.commons.lang3.StringUtils
import javax.inject.Inject
import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}
import javax.inject.Inject
import java.util.concurrent.CompletionStage

import org.yaml.snakeyaml.constructor.Constructor
import javax.inject.Inject
import org.slf4j.LoggerFactory
import javax.inject.Inject
import org.apache.spark.sql.SparkSession
import play.api.Configuration
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
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
import play.api.libs.ws.ahc.AhcWSClient
import akka.stream.ActorMaterializer
import akka.actor.ActorSystem

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
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import bootstrap.Init
import io.swagger.annotations._
import org.apache.spark.sql.functions._

// TODO thread pool!!!
@Api("EHealthVDCController")
class EHealthVDCController @Inject() (config: Configuration, initService: Init, ws: WSClient) extends InjectedController {
  private val LOGGER = LoggerFactory.getLogger("EHealthVDCController")
  var debugMode = false
  var dfShowLen = 10

  private def createDataAndProfileJoinDataFrame (spark: SparkSession, response:String, config: Configuration): Boolean = {

    val bloodTestsCompliantDF: DataFrame = ProcessEnforcementEngineResponse.processResponse(spark, config, response,
      debugMode, dfShowLen)
    if (bloodTestsCompliantDF == spark.emptyDataFrame)
      return false
    val profilesDF = DataFrameUtils.loadTableDFFromConfig(null, spark, config,
      "patientsProfiles")
    if (debugMode) {
      profilesDF.distinct().show(dfShowLen, false)
    }
    //TODO: check if inner join can be applied
    var joinedDF = bloodTestsCompliantDF.join(profilesDF, bloodTestsCompliantDF.col(Constants.SUBJECT_ID_COL_NAME).
      equalTo(profilesDF.col(Constants.SUBJECT_ID_COL_NAME)), "left_outer")
    joinedDF = joinedDF.drop(profilesDF.col(Constants.SUBJECT_ID_COL_NAME))
    joinedDF.createOrReplaceTempView("joined")
    if (debugMode) {
      joinedDF.distinct().show(dfShowLen, false)
    }
    true
  }

  private def getCompliantBloodTestsAndProfiles (spark: SparkSession, query:String, config: Configuration,
                                                 queryOnJoinTables: String): DataFrame = {
    if (!createDataAndProfileJoinDataFrame(spark, query, config)) {
      LOGGER.error("Error in createDataAndProfileJoinDataFrame")
      return spark.emptyDataFrame
    }

    var patientBloodTestsDF = spark.sql(queryOnJoinTables).toDF().filter(row => DataFrameUtils.anyNotNull(row))
    if (debugMode) {
      println (queryOnJoinTables)
      patientBloodTestsDF.distinct().show(dfShowLen, false)
      patientBloodTestsDF.printSchema
      patientBloodTestsDF.explain(true)
    }
    patientBloodTestsDF
  }

  private def sendRequestToEnforcmentEngine (purpose: String, requesterId: String, enforcementEngineURL:String, testType: String): String = {
    var newTestType:String = null
    if (testType.equals("cholesterol")) {
      newTestType = "cholesterol_total_value"
    } else {
      newTestType = "%s_value".format(testType)
    }
    val query = "SELECT patientId, date, %s FROM blood_tests".format(newTestType)

    val data = Json.obj(
      "query" -> query,
      "purpose" -> purpose,
      "access" -> "read",
      "requester" -> "",
      "blueprintId" -> "",
      "requesterId" -> requesterId
    )
    val futureResponse = ws.url(enforcementEngineURL).addHttpHeaders("Content-Type" -> "application/json")
      .addHttpHeaders("accept" -> "application/json").withRequestTimeout(Duration.Inf).post(data)

    val res = Await.result(futureResponse, Duration.Inf)
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

      debugMode = initService.getDebugMode
      dfShowLen = initService.getDfShowLen
      if (!config.has("policy.enforcement.play.url")) {
        Future.successful(NotFound("Missing enforcement url"))
      } else if (!origtestType.equals("cholesterol") &&
        !origtestType.equals("antithrombin") &&
        !origtestType.equals("fibrinogen")  &&
        !origtestType.equals("haemoglobin") &&
        !origtestType.equals("plateletCount") &&
        !origtestType.equals("prothrombinTime") &&
        !origtestType.equals("totalWhiteCellCount")) {
        Future.successful(NotFound("Unknown blood type"))
      } else{

        if (!request.headers("Purpose").equals("Marketing")  &&
          !request.headers("Purpose").equals("MedicalTreatment") &&
          !request.headers("Purpose").equals("Research") &&
          !request.headers("Purpose").equals("NutritionConsultation") &&
          !request.headers("Purpose").equals("MedicalResearch") &&
          !request.headers("Purpose").equals("NutritionalResearch")) {
          Future.successful(NotFound("Unknown purpose"))
        } else {

          val enforcementEngineURL: String = config.get[String]("policy.enforcement.play.url")
          val response = sendRequestToEnforcmentEngine(request.headers("Purpose"),
            request.headers("RequesterId"), enforcementEngineURL, origtestType)

          var newTestType: String = null;
          if (testType.equals("cholesterol")) {
            newTestType = "cholesterol_total_value"
          } else {
            newTestType = "%s_value".format(testType).replaceAll("\\.", "_")
          }

          val queryOnJoinTables = "SELECT patientId, date, %s FROM joined WHERE socialId=\"%s\"".format(newTestType,
            patientSSN)
          var resultDF = getCompliantBloodTestsAndProfiles(spark, response, config, queryOnJoinTables)
          if (resultDF == spark.emptyDataFrame) {
            Future.successful(NotAcceptable("Error processing enforcement engine result"))
          } else {

            //Adjust output to blueprint
            resultDF = resultDF.withColumnRenamed(newTestType, "value").drop(Constants.SUBJECT_ID_COL_NAME).distinct()
            resultDF = resultDF.orderBy("date").filter(row => DataFrameUtils.anyNotNull(row, Constants.DATE))
            val res = resultDF.takeAsList(1)
            if (res.size == 0)
              Future.successful(NotFound("No results were found"))
            else {
              val resultStr = resultDF.toJSON.collect.mkString("[", ",", "]")
              val json: JsValue = Json.parse(resultStr)

              Future.successful(Ok(json))
            }
          }
        }
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
      var avgTestType:String = null
      val todayDate =  java.time.LocalDate.now
      val minBirthDate = todayDate.minusYears(endAgeRange)
      val maxBirthDate = todayDate.minusYears(startAgeRange)
      debugMode = initService.getDebugMode
      dfShowLen = initService.getDfShowLen

      if (!config.has("policy.enforcement.play.url")) {
        Future.successful(NotFound("Missing enforcement url"))
      }  else if (startAgeRange >= endAgeRange) {
        Future.successful(NotFound("Wrong age range"))
      } else if (!testType.equals("cholesterol") &&
        !testType.equals("antithrombin") &&
        !testType.equals("fibrinogen")  &&
        !testType.equals("haemoglobin")  &&
        !testType.equals("plateletCount") &&
        !testType.equals("prothrombinTime") &&
        !testType.equals("totalWhiteCellCount"))
      {
        Future.successful(NotFound("Unknown blood type"))

      } else {
        if (!request.headers("Purpose").equals("Marketing") &&
          !request.headers("Purpose").equals("MedicalTreatment") &&
          !request.headers("Purpose").equals("Research") &&
          !request.headers("Purpose").equals("NutritionConsultation") &&
          !request.headers("Purpose").equals("MedicalResearch") &&
          !request.headers("Purpose").equals("NutritionalResearch")) {
          Future.successful(NotFound("Unknown purpose"))
        } else {
          val enforcementEngineURL: String = config.get[String]("policy.enforcement.play.url")
          val response = sendRequestToEnforcmentEngine(request.headers("Purpose"), "", enforcementEngineURL, testType)

          if (debugMode) {
            println("Range: " + startAgeRange + " " + endAgeRange)
          }

          if (testType.equals("cholesterol")) {
            avgTestType = "avg(cholesterol_total_value)"
          } else {
            avgTestType = "avg(" + "%s_value".format(testType).replaceAll("\\.", "_") + ")"
          }
          val queryOnJoinTables = "SELECT " + avgTestType + " FROM joined where birthDate > \"" + minBirthDate + "\" AND birthDate < \"" + maxBirthDate + "\""

          var resultDF = getCompliantBloodTestsAndProfiles(spark, response, config, queryOnJoinTables)
          if (resultDF == spark.emptyDataFrame) {
            Future.successful(NotAcceptable("Error processing enforcement engine result"))
          } else {
            //Adjust output to blueprint
            resultDF = resultDF.withColumnRenamed(avgTestType, "value")
            if (debugMode)
              resultDF.distinct().show(dfShowLen, false)
            val res = resultDF.takeAsList(1)
            if (res.size == 0)
              Future.successful(NotFound("No blood tests were found in the range of ages"))
            else {
              val newJsonObj = resultDF.toJSON.collect.mkString(",")
              val json: JsValue = Json.parse(newJsonObj)

              Future.successful(Ok(json))
            }
          }
        }
      }
  }
}





