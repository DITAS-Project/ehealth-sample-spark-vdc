package models

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, Reads, Writes}
import play.api.libs.functional.syntax._
import java.util.{Calendar, Date}
import io.swagger.annotations._

object BloodTestComponents {
  implicit val patientInfoWrites: Writes[BloodTestComponents] = Json.writes[BloodTestComponents]
  implicit val patientInfoReads: Reads[BloodTestComponents] = Json.reads[BloodTestComponents]
}

case class BloodTestComponents(@ApiModelProperty(value="json containing the result", example="[{date:1945-07-20,value:3.664},{date:2011-11-15,value:2.81611}]") date: String, value: Double)


