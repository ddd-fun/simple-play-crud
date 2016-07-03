package fast

import model.ServiceInterpreter
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test._

/**
 * Keeping this class only as show case and justification of usage scala check for validation.
 * It's really boring to write test coverage for invalid json scenarios in a way like code below.
 * Wouldn't be  better if we described declaratively what is valid/invalid json is, and let the
 * framework to generate hundreds of possible test cases? Please, see ControllerSpec.
 */
object DetailedAppSpec extends PlaySpecification with Results  {

  class TestApplicationController() extends controllers.Application(ServiceInterpreter)


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

      status(result) mustEqual(BAD_REQUEST)

      (Json.parse(bodyText) \ "status").as[String] must equalTo("KO")

    }


  }
}

