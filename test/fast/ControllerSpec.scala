package fast

import fast.DetailedAppSpec._
import org.scalacheck._
import org.scalacheck.Prop.forAll
import play.api.libs.json.{JsObject, Json}
import play.api.test.{FakeHeaders, FakeRequest}

object DomainGen{

  val validTitleGen = Gen.alphaStr suchThat( str => str.length > 2 && str.length < 32)
  val inValidTitleGen = Gen.alphaStr suchThat( str => str.length < 2 && str.length > 32)



}


object ControllerSpec extends Properties("Controller") {

  object TestApplicationController extends controllers.Application


  import DomainGen._

  def notEmptyAndNotEqualTo(exclude: String)  = Gen.alphaStr suchThat(str => str.length > 0 && str != exclude)

  val jsonWithInvalidFieldsNameGen = for{
    guid <- notEmptyAndNotEqualTo("guid")
    title <- notEmptyAndNotEqualTo("title")
    price <- notEmptyAndNotEqualTo("price")
    fuel <- notEmptyAndNotEqualTo("fuel")
  }yield (Json.obj(guid -> "guid", title -> "title", fuel -> "fuel", "price" -> 1))


  val advertValidJsonGen = for{
    guid <- Gen.uuid.map(_.toString)
    title <- validTitleGen
    fuel <- Gen.oneOf("diesel", "gasoline")
    price <- Gen.choose(0, 5000000)
  }yield (Json.obj("guid" -> guid, "title" -> title, "fuel" -> fuel, "price" -> price))



  def genFakeRequest(json:JsObject) = FakeRequest(
    method = "POST",
    uri = "/adverts",
    headers = FakeHeaders(Seq("Content-type"->"application/json")),
    body =  json
  )

  property("return bad request if invalid field name") = forAll(jsonWithInvalidFieldsNameGen.map(genFakeRequest)) { req =>

    val result = TestApplicationController.addAdvert.apply(req)

    status(result).equals(BAD_REQUEST) &&  (Json.parse(contentAsString(result) ) \ "status").as[String] == "KO"
  }

  property("return 200 for valid advert") = forAll(advertValidJsonGen.map(genFakeRequest)) { req =>

    val result = TestApplicationController.addAdvert.apply(req)

    status(result).equals(OK)
  }



}
