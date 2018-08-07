package models

import io.swagger.annotations._
import play.api.libs.json.{Json, Reads, Writes}

object Patient {
  implicit val patientWrites: Writes[Patient] = Json.writes[Patient]
  implicit val patientReads: Reads[Patient] = Json.reads[Patient]
}

case class Patient(
													@ApiModelProperty(value="json containing the result", example="[{patientId:1,cholesterol:242.0,wbc:7610]") result:       String)


