package fast

import fast.DetailedAppSpec._
import org.scalacheck._
import org.scalacheck.Prop.forAll
import play.api.libs.json.{JsObject, Json}
import play.api.test.{FakeHeaders, FakeRequest}

object DomainDataGen{

  val validTitleGen = for {
    length <- Gen.choose(2 ,32)
    str    <- Gen.listOfN(length, Gen.alphaChar).map(_.mkString)
  } yield str

  val inValidTitleGen = for {
    length <- Gen.oneOf(Gen.choose(0 ,1), Gen.choose(33, 128))
    str    <- Gen.listOfN(length, Gen.alphaChar).map(_.mkString)
  } yield str

  val validPriceGen = Gen.choose(0 , 5000000)

  val invalidPriceGen = Gen.oneOf(Gen.choose(-100 , -1), Gen.choose(5000000, 6000000))

  val validFuelGen = Gen.oneOf("diesel", "gasoline")

  val invalidFuelGen = Gen.alphaStr suchThat (str => !List("diesel, gasoline").contains(str))

  val validFirstRegGen = Gen.oneOf("1900-01-25 00:00", "2014-02-29 23:59", "2036-12-31 00:00")

  val validMileageGen = Gen.choose(1, 30000000)
}


object ControllerValidationSpec extends Properties("Controller validations") {

  object TestApplicationController extends controllers.Application

  import DomainDataGen._

  def anyExcept(exclude: String) = Gen.alphaStr suchThat(str => str.length > 0 && str != exclude)

  val jsonWithInvalidFieldNamesGen = for{
    guid  <- anyExcept("guid")
    title <- anyExcept("title")
    price <- anyExcept("price")
    fuel  <- anyExcept("fuel")
  }yield (Json.obj(guid -> "guid", title -> "title", fuel -> "fuel", "price" -> 1))


  val validJsonGen = for{
    guid  <- Gen.uuid.map(_.toString)
    title <- validTitleGen
    fuel  <- validFuelGen
    price <- validPriceGen
    usage <- Gen.option(validUsageJsonGen)
  }yield {val json = Json.obj("guid" -> guid, "title" -> title, "fuel" -> fuel, "price" -> price)
          usage.map(json++).getOrElse(json) }


  val invalidJsonGen = (for{
    (guid,g)  <- Gen.uuid.map(id => (id.toString, 1))
    (title,t) <- Gen.oneOf(validTitleGen.map((_,1)), inValidTitleGen.map((_,0)))
    (fuel,f)  <- Gen.oneOf(validFuelGen.map((_,1)), invalidFuelGen.map((_,0)))
    (price,p) <- Gen.oneOf(validPriceGen.map((_,1)), invalidPriceGen.map((_,0)) )
  }yield ((Json.obj("guid" -> guid, "title" -> title, "fuel" -> fuel, "price" -> price),
           g*t*f*p))) suchThat( _._2 != 1) map (_._1)

   val validUsageJsonGen: Gen[JsObject] = for{
    m   <- validMileageGen
    reg <- validFirstRegGen
  }yield (Json.obj("usage" -> Json.obj("mileage"->m, "firstReg" -> reg)))

  
  def genFakeRequest(json:JsObject) = new FakeRequest(
    method = "POST", uri = "/adverts",
    headers = FakeHeaders(Seq("Content-type"->"application/json")),
    body =  json
  ){override def toString = s"$method $uri body: $body"}


  property("return 400 if any of json fields has invalid name") =
    forAll(jsonWithInvalidFieldNamesGen.map(genFakeRequest)) { req =>
    val response = TestApplicationController.addAdvert.apply(req)

    status(response).equals(BAD_REQUEST) &&
      (Json.parse(contentAsString(response) ) \ "status").as[String] == "KO"
  }

  property("return 400 if some json field contains invalid value") =
  forAll(invalidJsonGen.map(genFakeRequest)) { req =>
    val response = TestApplicationController.addAdvert.apply(req)

    status(response).equals(BAD_REQUEST)  &&
      (Json.parse(contentAsString(response) ) \ "status").as[String] == "KO"
  }

  property("return 200 for valid request") =
    forAll(validJsonGen.map(genFakeRequest)) { req =>
    val response = TestApplicationController.addAdvert.apply(req)

    status(response).equals(OK)
  }

}
