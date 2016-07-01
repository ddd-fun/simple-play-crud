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
                    .post(Json.obj("title" -> "Audi A4",
                                   "fuel"-> "diesel",
                                   "price" -> 5000)))

      response.status must equalTo(OK).setMessage(response.body)

      val json = Json.parse(response.body)

      (json \ "guid").as[String] must not empty

    }

    "return not found for unknown advert" in new WithServer(port=PORT_9000){

      val guid = java.util.UUID.randomUUID.toString

      val response = await(WS.url(APP_URL+s"/adverts/$guid").get)

      response.status must equalTo(NOT_FOUND)

    }


    "return stored advert by guid" in new WithServer(port=PORT_9000){

      val addResponse = await(WS.url(APP_URL+"/adverts")
                          .post(Json.obj("title" -> "Audi A4",
                                         "fuel"-> "diesel",
                                         "price" -> 5000)))

      addResponse.status must equalTo(OK)

      val guid = (Json.parse(addResponse.body) \ "guid").as[String]

      val response = await(WS.url(APP_URL+s"/adverts/$guid").get)

      response.status must equalTo(OK).setMessage(response.body)

      val json = Json.parse(response.body)

      (json \ "title").as[String] must equalTo("Audi A4")
      (json \ "fuel").as[String] must equalTo("diesel")
      (json \ "price").as[Int] must equalTo(5000)

    }

  }
}
