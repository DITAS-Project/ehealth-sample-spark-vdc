/**
 * Copyright 2018 IBM
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * This is being developed for the DITAS Project: https://www.ditas-project.eu/
 */
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


