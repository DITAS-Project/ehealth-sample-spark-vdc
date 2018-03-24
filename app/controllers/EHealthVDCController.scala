package controllers

import scala.concurrent.Future

import org.apache.spark.sql.SparkSession

import bootstrap.Init
import javax.inject._
import javax.inject.Inject
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Controller

// TODO thread pool!!!

class EHealthVDCController @Inject() (config: Configuration, initService: Init)(implicit
  webJarAssets: WebJarAssets, val messagesApi: MessagesApi) extends Controller {

  def readData(spark: SparkSession): Unit = {
    val bloodTestsDF = spark.read.parquet(config.getString("s3.filename").get)
    // Displays the content of the DataFrame to stdout
    bloodTestsDF.show(false)
    bloodTestsDF.printSchema
    bloodTestsDF.createOrReplaceTempView("bloodTests")

    val table = config.getString("db.mysql.table").getOrElse("patient")
    val user = config.getString("db.mysql.username").get
    val password = config.getString("db.mysql.password").get
    val jdbcConnectionString = config.getString("db.mysql.url").get

    val patientsDF = spark.read.format("jdbc")
      .option("url", jdbcConnectionString)
      .option("dbtable", table)
      .option("user", user)
      .option("password", password)
      .load()
    patientsDF.show(false)
    patientsDF.printSchema
    patientsDF.createOrReplaceTempView("patients")

    val joinedDF = bloodTestsDF.join(patientsDF, "patientId")
    joinedDF.createOrReplaceTempView("joined")
  }

  def getPatientDetails(socialId: String): Action[AnyContent] = {
    Action.async {
      val spark = initService.getSparkSessionInstance
      readData(spark)
      val query = "select patientId, socialId, addressCity, addressRoad, addressRoadNumber, birthCity, nationality, job, schoolYears, " +
      "birthDate, gender, name, surname from patients where socialId='%s'".format(socialId)
      val patientDetailsDF = spark.sql(query)
      patientDetailsDF.show(false)

      val rawJson = patientDetailsDF.toJSON.collect().mkString
      Future.successful(Ok(Json.toJson(rawJson)))
    }
  }

  def getTestValues(socialId: String, testType: String): Action[AnyContent] = {
    Action.async {
      val spark = initService.getSparkSessionInstance
      readData(spark)
      val query = "select patientId, date, %s.value as %s from joined where socialId='%s'".format(testType, testType, socialId)
      val patientBloodTestsDF = spark.sql(query)
      patientBloodTestsDF.show(false)
      patientBloodTestsDF.printSchema

      val rawJson = patientBloodTestsDF.toJSON.collect().mkString
      Future.successful(Ok(Json.toJson(rawJson)))
    }
  }
  
  def getAllTestValues(socialId: String): Action[AnyContent] = {
    Action.async {
      val spark = initService.getSparkSessionInstance
      readData(spark)
      val query = "select antithrombin.value as antithrombin, cholesterol.hdl.value as hdl, cholesterol.ldl.value al ldl, cholesterol.total.value as cholesterol, " +
      "cholesterol.tryglicerides.value as tryglicerides, fibrinogen.value as fibrinogen, haemoglobin.value as haemoglobin, plateletCount.value as plateletCount, " +
      "prothrombinTime.value as prothrombinTime, totalWhiteCellCount.value as totalWhiteCellCount from joined where socialId='%s'".format(socialId)
      val patientBloodTestsDF = spark.sql(query)
      patientBloodTestsDF.show(false)
      patientBloodTestsDF.printSchema

      val rawJson = patientBloodTestsDF.toJSON.collect().mkString
      Future.successful(Ok(Json.toJson(rawJson)))
    }
  }
  
  def getTestAverage(testType: String, minSchoolYears: Int, maxSchoolYears: Int): Action[AnyContent] = {
    Action.async {
      val spark = initService.getSparkSessionInstance
      readData(spark)
      val query = "select AVG(%s.value) from joined where schoolYears>%d AND schoolYears<%d".format(minSchoolYears, maxSchoolYears)
      val patientBloodTestsDF = spark.sql(query)
      patientBloodTestsDF.show(false)
      patientBloodTestsDF.printSchema

      val rawJson = patientBloodTestsDF.toJSON.collect().mkString
      Future.successful(Ok(Json.toJson(rawJson)))
    }  
  }
}


