import javax.inject.Inject

import controllers.EHealthVDCController
import models.Patient
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