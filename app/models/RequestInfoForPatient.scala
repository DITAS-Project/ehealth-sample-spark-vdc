package models

import io.swagger.annotations._
import play.api.libs.json.{Json, Reads, Writes}


object RequestProfileInfoForPatient {
  implicit val requestQueryWrites: Writes[RequestProfileInfoForPatient] = Json.writes[RequestProfileInfoForPatient]
  implicit val requestQueryReads: Reads[RequestProfileInfoForPatient] = Json.reads[RequestProfileInfoForPatient]
}


case class RequestProfileInfoForPatient(
                         @ApiModelProperty(value="SSN", example="XGXCLS09X31T865C") patientSSN:       String,
                         @ApiModelProperty(value="requester id", example="7bff1d74-e3f0-4188-8acb-905f06705e43") requesterId:   String)


