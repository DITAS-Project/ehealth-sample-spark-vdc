package models

import io.swagger.annotations.{ApiModelProperty, _}
import play.api.libs.json.{Json, Reads, Writes}


object RequestAvgBloodTestTypeForPatient {
  implicit val requestQueryWrites: Writes[RequestAvgBloodTestTypeForPatient] = Json.writes[RequestAvgBloodTestTypeForPatient]
  implicit val requestQueryReads: Reads[RequestAvgBloodTestTypeForPatient] = Json.reads[RequestAvgBloodTestTypeForPatient]
}


case class RequestAvgBloodTestTypeForPatient(
                                              @ApiModelProperty(value="blood test type", example="antithrombin") bloodTestType:   String,
                                              @ApiModelProperty(value="end age range", example="70") endAgeRange:   Long,
                                              @ApiModelProperty(value="start age range", example="50") startAgeRange:   Long)


