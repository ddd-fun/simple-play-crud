import org.specs2.runner._
import org.junit.runner._

import play.api.libs.ws._
import play.api.test._



@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends PlaySpecification {

  var PORT_9000 = 9000
  val LOCAL_HOST = "http://localhost:"
  val APP_URL = LOCAL_HOST + PORT_9000

  "Application" should {

    "render the index page" in new WithServer(port=PORT_9000) {

      val response = await(WS.url(APP_URL).get())

      response.status must equalTo(OK)
      response.body must contain("Your new application is ready.")
    }

    "advert app is not available yet" in new WithServer(port=PORT_9000){

      val response = await(WS.url(APP_URL+"/advert").get())

      response.status must equalTo(NOT_FOUND)
    }
  }
}
