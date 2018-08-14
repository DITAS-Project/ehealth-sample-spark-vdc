package models

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, Reads, Writes}
import play.api.libs.functional.syntax._
import io.swagger.annotations._

object ComponentAvg {
  implicit val patientInfoWrites: Writes[ComponentAvg] = Json.writes[ComponentAvg]
  implicit val patientInfoReads: Reads[ComponentAvg] = Json.reads[ComponentAvg]
}

case class ComponentAvg(@ApiModelProperty(value="The average of component ",
  example="{value:2.722}")
                        value: String)


