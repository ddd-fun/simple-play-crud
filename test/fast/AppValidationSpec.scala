package fast

import fast.DetailedAppSpec._
import model.ServiceInterpreter
import org.scalacheck._
import org.scalacheck.Prop.{forAll, BooleanOperators}
import play.api.libs.json.{JsObject, Json}
import play.api.test.{FakeHeaders, FakeRequest}


object AppValidationSpec extends Properties("Application") with DomainDataGen {

  object TestApplicationController extends controllers.Application(ServiceInterpreter)

  property("return 400 if any of json fields has invalid name") =
  forAll(jsonWithInvalidFieldNamesGen.map(fakePostAdvertRequest)) { req =>
    examineBadRequestProperty(req)
  }

  property("return 400 if some json field contains invalid value") =
  forAll(invalidJsonGen.map(fakePostAdvertRequest)) { req =>
    examineBadRequestProperty(req)
  }

  property("return 200 for valid request") =
  forAll(validJsonGen.map(fakePostAdvertRequest)) { req =>
    val response = TestApplicationController.addAdvert.apply(req)
    val responseStatus = status(response)
    responseStatus.equals(OK) :| (s"expected http status 200, but actually: $responseStatus")
  }

  def fakePostAdvertRequest(json:JsObject) = new FakeRequest(
      method = "POST", uri = "/adverts",
      headers = FakeHeaders(Seq("Content-type"->"application/json")),
      body =  json
  ){override def toString = s"$method $uri body: $body"}


  def examineBadRequestProperty(fakeRequest: FakeRequest[JsObject]) = {
      val response = TestApplicationController.addAdvert.apply(fakeRequest)
      val stat = status(response)
      val content = contentAsString(response)
    (stat.equals(BAD_REQUEST)) :| (s"expected http status 400, but actually: $stat") &&
      ((Json.parse(content) \ "status").as[String] == "KO") :| (s"expected json filed status=KO, but actually: $content")
  }

}
