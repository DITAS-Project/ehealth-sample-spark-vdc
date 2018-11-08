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
import javax.inject.Inject

import controllers.EHealthVDCController
import play.api.i18n.Messages
import play.api.mvc._
import play.api.test._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.libs.json.Json
import play.api.Configuration

import scala.concurrent.Future

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import bootstrap.Init




@RunWith(classOf[JUnitRunner])
class EHealthVDCControllerTest extends PlaySpecification with Results  {

  
//  val application = new GuiceApplicationBuilder()
//  .configure(Map("ehcacheplugin" -> "disabled"))
//  .overrides(bind[Init].to[MockInit])
//  .build()
  
  "User" should {
    "have a name" in {
      val user = "Player"
      user must beEqualTo("Player")
    }
  }

//  
//  private def controller = {
//    new EHealthVDCController() {
//      override def controllerComponents: ControllerComponents = Helpers.stubControllerComponents()
//    }
//  }
//
//  
//
//  "Query Rewrite" should {
//    "be valid" in {
//      val socialId = "123"
//      val result: Future[Result] = controller.getPatientDetails(socialId)
//      status(result) must equalTo(OK)
//      contentType(result) must equalTo(Some("application/json"))
//      val expectedJson = Json.toJson(Patient(socialId))
//      contentAsJson(result) must equalTo(expectedJson)
//
//    }
//  }
}
