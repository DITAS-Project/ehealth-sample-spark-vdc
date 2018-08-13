package models

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, Reads, Writes}
import play.api.libs.functional.syntax._
import java.util.{Calendar, Date}


object BloodTestComponents {
  implicit val patientInfoWrites: Writes[BloodTestComponents] = Json.writes[BloodTestComponents]
  implicit val patientInfoReads: Reads[BloodTestComponents] = Json.reads[BloodTestComponents]
}

case class BloodTestComponents(date: String, value: Double)


