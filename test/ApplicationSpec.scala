import org.specs2.runner._
import org.junit.runner._
import play.api.libs.json.{JsObject, JsPath, Json}

import play.api.libs.ws._
import play.api.test._



@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends PlaySpecification {

  val PORT_9000 = 9000
  val APP_URL = "http://localhost:" + PORT_9000

  "Application" should {

    "render the index page" in new WithServer(port = PORT_9000) {

      val response = await(WS.url(APP_URL).get())

      response.status must equalTo(OK)
      response.body must contain("Your new application is ready.")
    }

    "add advert and return guid" in new WithServer(port = PORT_9000) {
      withSavedAdvert { (json, guid) =>
         guid must not empty
      }
    }

    "return not found for unknown advert" in new WithServer(port = PORT_9000) {

      val guid = java.util.UUID.randomUUID.toString

      val response = await(WS.url(APP_URL + s"/adverts/$guid").get)

      response.status must equalTo(NOT_FOUND)

    }


    "return stored advert by guid" in new WithServer(port = PORT_9000) {
      withSavedAdvert { (json, guid) =>

        val response = await(WS.url(APP_URL + s"/adverts/$guid").get)

        response.status must equalTo(OK).setMessage(response.body)

        val responseJson = Json.parse(response.body)

        (responseJson \ "title").as[String] must equalTo( (json \ "title").as[String] )
        (responseJson \ "fuel").as[String] must equalTo( (json \ "fuel").as[String] )
        (responseJson \ "price").as[Int] must equalTo( (json \ "price").as[Int])
     }
    }


    "update stored advert" in new WithServer(port = PORT_9000) {
      withSavedAdvert { (json, guid) =>

        val updateResponse = await(WS.url(APP_URL + s"/adverts/$guid")
                                     .put(Json.obj(
                                     "title" -> "Audi A5",
                                     "fuel" -> "gasoline",
                                     "price" -> 6000)))

        updateResponse.status must equalTo(OK).setMessage(updateResponse.body)

        val response = await(WS.url(APP_URL + s"/adverts/$guid").get)

        val json = Json.parse(response.body)

        (json \ "title").as[String] must equalTo("Audi A5")
        (json \ "fuel").as[String] must equalTo("gasoline")
        (json \ "price").as[Int] must equalTo(6000)

      }
    }

    "delete stored advert" in new WithServer(port = PORT_9000) {
      withSavedAdvert { (json, guid) =>

        val deleteResponse = await(WS.url(APP_URL + s"/adverts/$guid").delete)

        deleteResponse.status must equalTo(OK).setMessage(deleteResponse.body)

        val response = await(WS.url(APP_URL + s"/adverts/$guid").get)

        response.status must equalTo(NOT_FOUND)

      }
    }


    "get all stored adverts" in new WithServer(port = PORT_9000) {
      withSavedAdvert { (json, guid) =>

        val getAllResponse = await(WS.url(APP_URL + "/adverts").get)

        getAllResponse.status must equalTo(NOT_FOUND).setMessage(getAllResponse.body)

      }
    }

  }


  def withSavedAdvert(testFragment: (JsObject, String) => Any) {

    import play.api.Play.current

    val advert = Json.obj("title" -> "Audi A4",
                          "fuel" -> "diesel",
                          "price" -> 5000)

    val addResponse = await(WS.url(APP_URL + "/adverts").post(advert))

    addResponse.status must equalTo(OK)

    val guid = (Json.parse(addResponse.body) \ "guid").as[String]

    testFragment(advert, guid) // execute fragment in context of stored advert

  }


}