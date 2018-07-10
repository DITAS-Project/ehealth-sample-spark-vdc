package controllers


import javax.inject.Inject

import io.swagger.annotations._
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future

import org.apache.spark.sql.SparkSession

import bootstrap.Init
import play.api.Configuration
import play.api.libs.json.Json

// TODO thread pool!!!
@Api("EHealthVDCController")
class EHealthVDCController @Inject() (config: Configuration, initService: Init) extends InjectedController {

  def readData(spark: SparkSession): Unit = {
    val bloodTestsDF = spark.read.parquet(config.get[String]("s3.filename"))
    // Displays the content of the DataFrame to stdout
//    bloodTestsDF.limit(5).show(false)
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
//    patientsDF.limit(5).show(false)
    patientsDF.printSchema
    patientsDF.createOrReplaceTempView("patients")

    val joinedDF = bloodTestsDF.join(patientsDF, "patientId")
    joinedDF.createOrReplaceTempView("joined")
  }


  @ApiOperation(nickname = "getPatientDetails",
    value = "Get patient details",
    notes = "",
    response = classOf[models.Patient], responseContainer = "List", httpMethod = "GET")
  @ApiResponses(Array(
      new ApiResponse(code = 400, message = "Invalid social ID value"))) 
  def getPatientDetails(
      @ApiParam(value = "Social ID", required = true,
                        allowMultiple = false) socialId: String) = Action.async { 
    implicit request => 
      val spark = initService.getSparkSessionInstance
      readData(spark)
      val query = "select patientId, socialId, addressCity, addressRoad, addressRoadNumber, birthCity, nationality, job, schoolYears, " +
      "birthDate, gender, name, surname from patients where socialId='%s'".format(socialId)
      val patientDetailsDF = spark.sql(query)
      patientDetailsDF.show(false)

      val rawJson = patientDetailsDF.toJSON.collect().mkString
      Future.successful(Ok(Json.toJson(rawJson)))
  }

  def getTestValues(socialId: String, testType: String): Action[AnyContent] = {
    Action.async {
      val spark = initService.getSparkSessionInstance
      readData(spark)
      val query = "select patientId, date, %s.value as %s from joined where socialId='%s'".format(testType, testType, socialId)
      val patientBloodTestsDF = spark.sql(query)
      patientBloodTestsDF.limit(10).show(false)
      patientBloodTestsDF.printSchema
       patientBloodTestsDF.explain(true)

      val rawJson = patientBloodTestsDF.limit(10).toJSON.collect().mkString
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
      patientBloodTestsDF.limit(10).show(false)
      patientBloodTestsDF.printSchema

      val rawJson = patientBloodTestsDF.limit(10).toJSON.collect().mkString
      Future.successful(Ok(Json.toJson(rawJson)))
    }
  }
  
  def getTestAverage(testType: String, minSchoolYears: Int, maxSchoolYears: Int): Action[AnyContent] = {
    Action.async {
      val spark = initService.getSparkSessionInstance
      readData(spark)
      val query = "select AVG(%s.value) from joined where schoolYears>%d AND schoolYears<%d".format(minSchoolYears, maxSchoolYears)
      val patientBloodTestsDF = spark.sql(query)
      patientBloodTestsDF.limit(10).show(false)
      patientBloodTestsDF.printSchema

      val rawJson = patientBloodTestsDF.limit(10).toJSON.collect().mkString
      Future.successful(Ok(Json.toJson(rawJson)))
    }  
  }
}


