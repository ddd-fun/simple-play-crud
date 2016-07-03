package fast

import fast.DetailedAppSpec._
import org.scalacheck._
import org.scalacheck.Prop.forAll
import play.api.libs.json.{JsObject, Json}
import play.api.test.{FakeHeaders, FakeRequest}


object AppValidationSpec extends Properties("Application") with DomainDataGen {

  object TestApplicationController extends controllers.Application
   
  
  def fakePostAdvertRequest(json:JsObject) = new FakeRequest(
    method = "POST", uri = "/adverts",
    headers = FakeHeaders(Seq("Content-type"->"application/json")),
    body =  json
  ){override def toString = s"$method $uri body: $body"}


  property("return 400 if any of json fields has invalid name") =
    forAll(jsonWithInvalidFieldNamesGen.map(fakePostAdvertRequest)) { req =>
    val response = TestApplicationController.addAdvert.apply(req)

    status(response).equals(BAD_REQUEST) &&
      (Json.parse(contentAsString(response) ) \ "status").as[String] == "KO"
  }

  property("return 400 if some json field contains invalid value") =
  forAll(invalidJsonGen.map(fakePostAdvertRequest)) { req =>
    val response = TestApplicationController.addAdvert.apply(req)

    status(response).equals(BAD_REQUEST)  &&
      (Json.parse(contentAsString(response) ) \ "status").as[String] == "KO"
  }

  property("return 200 for valid request") =
    forAll(validJsonGen.map(fakePostAdvertRequest)) { req =>
    val response = TestApplicationController.addAdvert.apply(req)

    status(response).equals(OK)
  }

}
