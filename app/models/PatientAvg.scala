package models

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, Reads, Writes}
import play.api.libs.functional.syntax._
import io.swagger.annotations._

object PatientAvg {
  implicit val patientInfoWrites: Writes[PatientAvg] = Json.writes[PatientAvg]
  implicit val patientInfoReads: Reads[PatientAvg] = Json.reads[PatientAvg]
}

case class PatientAvg(@ApiModelProperty(value="json containing the avg", example="{value:2.722}") value: String)


