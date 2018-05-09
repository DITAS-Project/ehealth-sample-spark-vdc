package models

import io.swagger.annotations._
import play.api.libs.json.{Json, Reads, Writes}

object Patient {
  implicit val patientWrites: Writes[Patient] = Json.writes[Patient]
  implicit val patientReads: Reads[Patient] = Json.reads[Patient]
}


case class Patient(                   
	patientId: String, 
	socialId: String, 
	addressCity: String, 
	addressRoad: String, 
	addressRoadNumber: String, 
	birthCity: String, 
	nationality: String, 
	job: String, 
	schoolYears: Int, 
	birthDate: String, 
	gender: String, 
	name: String, 
	surname: String	
)


