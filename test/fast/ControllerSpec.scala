package fast

import fast.DetailedAppSpec._
import org.scalacheck._
import org.scalacheck.Prop.forAll
import play.api.libs.json.{JsObject, Json}
import play.api.test.{FakeHeaders, FakeRequest}

object DomainGen{

  val validTitleGen = for {
    length <- Gen.choose(2 ,32)
    str <- Gen.listOfN(length, Gen.alphaChar).map(_.mkString)
  } yield str

  val inValidTitleGen = for {
    length <- Gen.oneOf(Gen.choose(0 ,1), Gen.choose(33, 128))
    str <- Gen.listOfN(length, Gen.alphaChar).map(_.mkString)
  } yield str

  val validPriceGen = Gen.choose(0 , 5000000)

  val invalidPriceGen = Gen.oneOf(Gen.choose(-100 , -1), Gen.choose(5000000, 6000000))

  val validFuelGen = Gen.oneOf("diesel", "gasoline")

  val invalidFuelGen = Gen.alphaStr suchThat (str => !List("diesel, gasoline").contains(str))
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


  val seedOfInvalidJsonGen: Gen[(JsObject, Int)] = for{
    (guid, g) <- Gen.uuid.map(id => (id.toString, 1))
    (title, t) <- Gen.oneOf(validTitleGen.map((_,1)), inValidTitleGen.map((_,0)))
    (fuel, f) <- Gen.oneOf(validFuelGen.map((_,1)), invalidFuelGen.map((_,0)))
    (price, p) <- Gen.oneOf(validPriceGen.map((_,1)), invalidPriceGen.map((_,0)) )
  }yield ((Json.obj("guid" -> guid, "title" -> title, "fuel" -> fuel, "price" -> price),  g*t*f*p))

  val invalidJsonGen = seedOfInvalidJsonGen suchThat( _._2 != 1) map (_._1)
  
  
  def genFakeRequest(json:JsObject) = FakeRequest(
    method = "POST",
    uri = "/adverts",
    headers = FakeHeaders(Seq("Content-type"->"application/json")),
    body =  json
  )

  def callAddAdvert(req:FakeRequest[JsObject]) = TestApplicationController.addAdvert.apply(req)
  
  property("return 400 bad request if json contains fields with invalid name") =
    forAll(jsonWithInvalidFieldsNameGen.map(genFakeRequest).map(callAddAdvert)) { response =>

    status(response).equals(BAD_REQUEST) &&  (Json.parse(contentAsString(response) ) \ "status").as[String] == "KO"
  }

  property("return 200 for valid request") =
    forAll(advertValidJsonGen.map(genFakeRequest).map(callAddAdvert)) { response =>

    status(response).equals(OK)
  }

  property("return 400 bad request if some field contains invalid value") =
    forAll(invalidJsonGen.map(genFakeRequest).map(callAddAdvert)) { response =>

    status(response).equals(BAD_REQUEST)  && (Json.parse(contentAsString(response) ) \ "status").as[String] == "KO"
  }


}
