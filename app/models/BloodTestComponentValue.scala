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

case class BloodTestComponentValue(@ApiModelProperty(value="Values of a blood test component",
  example="[{date:1945-07-20,value:3.664},{date:2011-11-15,value:2.81611}]")
                                   date: String, value: Double)


