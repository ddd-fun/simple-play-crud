package controllers

import java.util.UUID

import infrastructure.{SetUp, DynamoDb}
import model._
import play.api._
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

class Application(service:AdvertService[AdvertInfo, UUID]) extends Controller{

  implicit val usageReads: Reads[CarUsage] = (
      (JsPath \ "mileage").read[Int](min(1) keepAnd max(30000000)) and
      (JsPath \ "firstReg").read[String](dateValidator)
    )(CarUsage.apply _)

  implicit val advertReads: Reads[AdvertInfo] = (
      (JsPath \ "guid").read[UUID] and
      (JsPath \ "title").read[String](minLength[String](2) keepAnd maxLength[String](32)) and
      (JsPath \ "fuel").read[String](fuelValidator(List("diesel", "gasoline"))) and
      (JsPath \ "price").read[Int](min(0) keepAnd max(5000000)) and
      (JsPath \ "usage").readNullable[CarUsage]
  )(AdvertInfo.apply _)

  implicit val usageWrites: Writes[CarUsage] = (
      (JsPath \ "mileage").write[Int] and
      (JsPath \ "firstReg").write[String]
    )(unlift(CarUsage.unapply))

  implicit val advertWrites: Writes[AdvertInfo] = (
      (JsPath \ "guid").write[UUID] and
      (JsPath \ "title").write[String] and
      (JsPath \ "fuel").write[String] and
      (JsPath \ "price").write[Int] and
      (JsPath \ "usage").writeNullable[CarUsage]
  )(unlift(AdvertInfo.unapply))


  def dateValidator = Reads.pattern("((19|20)\\d\\d)-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01]) ?([0-1][0-9]|2[0-3]):([0-5][0-9])".r,
    "error.date:yyyy/mm/dd HH:MM")

  def fuelValidator(allowed:List[String]) =
    Reads.of[String].filter(ValidationError("allowed only "+allowed))(allowed.contains)

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def advertNotFoundJson(guid:UUID) = Json.obj("status" -> "KO", "message" -> s"advert by guid=$guid is not found")

  def getAdvert(guid: UUID) = Action {
    service.get(guid).map(adv => Ok(Json.toJson[AdvertInfo](adv)))
       .getOrElse(NotFound(advertNotFoundJson(guid)))
  }

  def addAdvert = Action(BodyParsers.parse.json){ request =>
    onValidAdvert(request.body)(advert => {
      val guid = advert.guid
      service.store(guid, advert).map(_=> Ok(Json.obj("guid" -> advert.guid)))
        .getOrElse(BadRequest(Json.obj("status" -> "KO", "message" -> s"advert with guid=$guid already exist"))) })
  }

  def updateAdvert(guid: UUID) = Action(BodyParsers.parse.json) { request =>
   onValidAdvert(request.body)(advert =>
     service.update(guid, advert).map(_=> Ok)
       .getOrElse(NotFound(advertNotFoundJson(guid))))
  }

  def deleteAdvert(guid: UUID) = Action {
    service.delete(guid).map( _=> Ok)
        .getOrElse(NotFound(advertNotFoundJson(guid)))
  }

  def getAllAdverts = Action {
    Ok(Json.obj("adverts" -> service.getAll))
  }

  private def onValidAdvert(json: JsValue)(invokeDomain: AdvertInfo => Result) = {
    json.validate[AdvertInfo] match {
      case JsSuccess(advert, _) =>  invokeDomain(advert)
      case errors: JsError => BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))
    }
  }
}

object Application extends Application(DynamoServiceInterpreter)

object DynamoServiceInterpreter extends DynamoInterpreter(DynamoDb)

object DynamoDb extends DynamoDb {
  def setUp = {
    val cfg = play.api.Play.current.configuration
    val setUp = for{url <- cfg.getString("aws.dynamo.url")
                    keyId <- cfg.getString("aws.dynamo.accessKeyId")
                    key <- cfg.getString("aws.dynamo.accessKey")
                  }yield SetUp(url, keyId, key)
   setUp.getOrElse(throw new RuntimeException("could not read dynamo config"))
  }
}

