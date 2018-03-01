package controllers

import javax.inject.Inject
import javax.inject._
import play.api.Configuration

import scala.concurrent.Future

import org.apache.spark.sql.{DataFrame, SparkSession}
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Controller}
import scala.util.parsing.json.JSONObject
import scala.collection.mutable.ArrayBuffer

import bootstrap.Init

// TODO thread pool!!!

class EHealthVDCController @Inject() (config: Configuration, initService: Init) (implicit webJarAssets: WebJarAssets,
    val messagesApi: MessagesApi) extends Controller {


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

def getPatientDetails(socialId: String): Action[AnyContent] = { Action.async {
   val spark = initService.getSparkSessionInstance
   readData(spark)
   val query = "select patientId, name, surname from patients where socialId='%s'".format(socialId)
   val patientDetailsDF = spark.sql(query)
   patientDetailsDF.show(false)

   val rawJson = patientDetailsDF.toJSON.collect().mkString
   Future.successful(Ok(Json.toJson(rawJson)))
  }
}

def getTestValues(patientId: String, testType: String): Action[AnyContent] = { Action.async {
   val spark = initService.getSparkSessionInstance
   readData(spark)
   val query = "select patientId, date, %s.value as %s from bloodTests where patientId='%s'".format(testType, testType, patientId)
   val patientBloodTestsDF = spark.sql(query)
   patientBloodTestsDF.show(false)
   patientBloodTestsDF.printSchema

   val rawJson = patientBloodTestsDF.toJSON.collect().mkString
   Future.successful(Ok(Json.toJson(rawJson)))
  }
}
}


