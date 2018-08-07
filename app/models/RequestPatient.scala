package models

import io.swagger.annotations._
import play.api.libs.json.{Json, Reads, Writes}


object RequestPatient {
  implicit val requestQueryWrites: Writes[RequestPatient] = Json.writes[RequestPatient]
  implicit val requestQueryReads: Reads[RequestPatient] = Json.reads[RequestPatient]
}


case class RequestPatient(
                         @ApiModelProperty(value="SSN", example="XGXCLS09X31T865C") patientSSN:       String,
                         @ApiModelProperty(value="requester id", example="7bff1d74-e3f0-4188-8acb-905f06705e43") requesterId:   String)


