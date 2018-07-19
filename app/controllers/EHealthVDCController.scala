package controllers


import bootstrap.Init
import io.swagger.annotations._
import javax.inject.Inject
import org.apache.spark.sql.SparkSession
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future

// TODO thread pool!!!
@Api("EHealthVDCController")
class EHealthVDCController @Inject() (config: Configuration, initService: Init) extends InjectedController {

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
  def getPatientDetails(
      @ApiParam(value = "SSN", required = true,
                        allowMultiple = false) socialId: String) = Action.async { 
    implicit request => 
      val spark = initService.getSparkSessionInstance
      readData(spark)

      val query = "select socialId as SSN, name, surname, gender, birthDate," +
        " addressCity, addressRoad, addressRoadNumber, addressPostalCode, telephoneNumber, birthCity, nationality, job, schoolYears" +
      " from patients where socialId='%s'".format(socialId)
      val patientDetailsDF = spark.sql(query)
      patientDetailsDF.show(false)

      val rawJson = patientDetailsDF.toJSON.collect().mkString
      Future.successful(Ok(Json.toJson(rawJson)))
  }

  @ApiOperation(nickname = "getAllValuesForBloodTestComponent",
    value = "Get timeseries of patient's blood test component",
    notes =  "This method returns the collected values for a specific blood test component of a patient (identified by his SSN), to be used by medical doctors",
    response = classOf[models.Patient], responseContainer = "List", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Patient not found")))
  def getTestValues(@ApiParam(value = "SSN", required = true,
    allowMultiple = false) socialId: String,
                    @ApiParam(value = "component", required = true,
                      allowMultiple = false) testType: String): Action[AnyContent] = {
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

  @ApiOperation(nickname = "getLastValuesForBloodTest",
    value = "Get patient's latest values for all measured components",
    notes = "This method returns the latest values of all the blood test components measured on a patient (identified by his SSN), to be used by medical doctors",
    response = classOf[models.Patient], responseContainer = "List", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Patient not found")))
  def getAllTestValues(@ApiParam(value = "SSN", required = true,
                          allowMultiple = false) socialId: String): Action[AnyContent] = {
    Action.async {
      val spark = initService.getSparkSessionInstance
      readData(spark)
      val query = "select date, antithrombin.value as antithrombin, cholesterol.hdl.value as hdl, cholesterol.ldl.value al ldl, cholesterol.total.value as cholesterol, " +
      "cholesterol.tryglicerides.value as tryglicerides, fibrinogen.value as fibrinogen, haemoglobin.value as haemoglobin, plateletCount.value as plateletCount, " +
      "prothrombinTime.value as prothrombinTime, totalWhiteCellCount.value as totalWhiteCellCount from joined where socialId='%s'".format(socialId)
      val patientBloodTestsDF = spark.sql(query)
      patientBloodTestsDF.limit(10).show(false)
      patientBloodTestsDF.printSchema

      val rawJson = patientBloodTestsDF.limit(10).toJSON.collect().mkString
      Future.successful(Ok(Json.toJson(rawJson)))
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
                       allowMultiple = false) endAgeRange: Int): Action[AnyContent] = {
    Action.async {
      val spark = initService.getSparkSessionInstance
      readData(spark)
      val todayDate =  java.time.LocalDate.now
      val minBirthDate = todayDate.minusYears(endAgeRange)
      val maxBirthDate = todayDate.minusYears(startAgeRange)
      val query = "select AVG(%s.value) from joined where birthDate>%d AND birthDate<%d".format(minBirthDate, maxBirthDate)
      val patientBloodTestsDF = spark.sql(query)
      patientBloodTestsDF.limit(10).show(false)
      patientBloodTestsDF.printSchema

      val rawJson = patientBloodTestsDF.limit(10).toJSON.collect().mkString
      Future.successful(Ok(Json.toJson(rawJson)))
    }  
  }
}


