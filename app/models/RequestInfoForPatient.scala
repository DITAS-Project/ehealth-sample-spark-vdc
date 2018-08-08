package models

import io.swagger.annotations._
import play.api.libs.json.{Json, Reads, Writes}


object RequestInfoForPatient {
  implicit val RequestInfoForPatientWrites: Writes[RequestInfoForPatient] = Json.writes[RequestInfoForPatient]
  implicit val RequestInfoForPatientReads: Reads[RequestInfoForPatient] = Json.reads[RequestInfoForPatient]
}


case class RequestInfoForPatient(
                         @ApiModelProperty(value="SSN", example="XGXCLS09X31T865C") patientSSN:       String,
                         @ApiModelProperty(value="requester id", example="7bff1d74-e3f0-4188-8acb-905f06705e43") requesterId:   String)


