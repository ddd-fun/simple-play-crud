import org.specs2.runner._
import org.junit.runner._
import play.api.http.Writeable
import play.api.libs.json.{JsPath, Json}

import play.api.libs.ws._
import play.api.test._



@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends PlaySpecification {

  val PORT_9000 = 9000
  val APP_URL = "http://localhost:" + PORT_9000

  "Application" should {

    "render the index page" in new WithServer(port=PORT_9000) {

      val response = await(WS.url(APP_URL).get())

      response.status must equalTo(OK)
      response.body must contain("Your new application is ready.")
    }

    "add advert and return guid" in new WithServer(port=PORT_9000){

      val response = await(WS.url(APP_URL+"/adverts")
                    .post(Json.obj("guid"-> "1231456789",
                                   "title" -> "Audi A4",
                                   "fuel"-> "diesel",
                                   "price" -> "5000")))

      response.status must equalTo(OK)

      val json = Json.parse(response.body)

      (json \ "guid").as[String] must equalTo("1231456789")

    }
  }
}
