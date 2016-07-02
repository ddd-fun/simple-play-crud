package fast

import play.api.libs.json.Json
import play.api.mvc._
import play.api.test._

object DetailedAppSpec extends PlaySpecification with Results  {

  class TestApplicationController() extends controllers.Application


  "Application" should {

    "send 404 bad request on invalid json" in {

      val json = Json.obj("invalidFieldName" -> "bla-bla",
                          "title" -> "Audi A5",
                          "fuel" -> "gasoline",
                          "price" -> 6000)

      val req = FakeRequest(
        method = "POST",
        uri = "/adverts",
        headers = FakeHeaders(Seq("Content-type"->"application/json")),
        body =  json
      )

      val controller = new TestApplicationController

      val result = controller.addAdvert.apply(req)

      val bodyText: String = contentAsString(result) //must contain()

      println(bodyText)

      status(result) mustEqual(BAD_REQUEST)

      //{"status":"KO","message":{"obj.guid":[{"msg":["error.path.missing"],"args":[]}]}}

      (Json.parse(bodyText) \ "status").as[String] must equalTo("KO")

    }


  }
}

