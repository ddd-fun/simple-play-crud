package slow

import play.api.libs.json._
import play.api.libs.ws._
import play.api.test._


object AppSkeletonSpec extends PlaySpecification {

  val PORT_9000 = 9000
  val APP_URL = "http://localhost:" + PORT_9000

  "Application" should {

    "add advert and return guid" in new WithServer(port = PORT_9000) {
      withSavedAdvert { (json, guid) =>
         guid must not be empty
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
                                     "guid" -> guid,
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

    "return all stored adverts" in new WithServer(port = PORT_9000) {
      withSavedAdvert { (json, guid) =>

        val getAllResponse = await(WS.url(APP_URL + "/adverts").get)

        getAllResponse.status must equalTo(OK).setMessage(getAllResponse.body)

        val json = Json.parse(getAllResponse.body)

        (json  \ "adverts" ).toEither match {
          case Right(JsArray(seq)) => seq.size must be greaterThan 0
          case fail@ _ => failure("failed to parse replay: " +fail)
        }

      }
    }

  }

  def withSavedAdvert(testFragment: (JsObject, String) => Any) {

    import play.api.Play.current

    val guid = java.util.UUID.randomUUID.toString

    val advert = Json.obj("guid" -> guid,
                          "title" -> "Audi A4",
                          "fuel" -> "diesel",
                          "price" -> 5000)

    val addResponse = await(WS.url(APP_URL + "/adverts").post(advert))

    addResponse.status must equalTo(OK).setMessage(addResponse.body)

    (Json.parse(addResponse.body) \ "guid").as[String] must equalTo(guid)

    testFragment(advert, guid) // execute fragment in context of stored advert

  }


}
