package models

import io.swagger.annotations._
import play.api.libs.json.{Json, Reads, Writes}


object RequestBloodTestTypeForPatient {
  implicit val requestQueryWrites: Writes[RequestBloodTestTypeForPatient] = Json.writes[RequestBloodTestTypeForPatient]
  implicit val requestQueryReads: Reads[RequestBloodTestTypeForPatient] = Json.reads[RequestBloodTestTypeForPatient]
}


case class RequestBloodTestTypeForPatient(
                           @ApiModelProperty(value="SSN", example="XGXCLS09X31T865C") patientSSN:       String,
                           @ApiModelProperty(value="requester id", example="7bff1d74-e3f0-4188-8acb-905f06705e43") requesterId:   String,
                           @ApiModelProperty(value="blood test type", example="antithrombin") bloodTestType:   String)


