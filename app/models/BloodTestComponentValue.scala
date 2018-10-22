package models

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, Reads, Writes}
import play.api.libs.functional.syntax._
import java.util.{Calendar, Date}
import io.swagger.annotations._

object BloodTestComponentValue {
  implicit val patientInfoWrites: Writes[BloodTestComponentValue] = Json.writes[BloodTestComponentValue]
  implicit val patientInfoReads: Reads[BloodTestComponentValue] = Json.reads[BloodTestComponentValue]
}

case class BloodTestComponentValue(@ApiModelProperty(value="The date in which the blood test was taken",
  example="1945-07-20") date: String, @ApiModelProperty(value="Value of a blood test component",
                                     example="3.664")value: Double)


