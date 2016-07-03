package controllers

import model.{ServiceInterpreter, AdvertService, CarUsage, AdvertInfo}
import play.api._
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

class Application(service:AdvertService[AdvertInfo, String]) extends Controller{

  implicit val usageReads: Reads[CarUsage] = (
      (JsPath \ "mileage").read[Int](min(1) keepAnd max(30000000)) and
      (JsPath \ "firstReg").read[String](dateValidator)
    )(CarUsage.apply _)

  implicit val advertReads: Reads[AdvertInfo] = (
      (JsPath \ "guid").read[String] and
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
      (JsPath \ "guid").write[String] and
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

  def advertNotFoundJson(guid:String) = Json.obj("status" -> "KO", "message" -> s"advert by guid=$guid is not found")

  def addAdvert = Action(BodyParsers.parse.json){ request =>
    request.body.validate[AdvertInfo] match  {
      case JsSuccess(advert, _) =>  { 
        val guid = advert.guid
        service.store(guid, advert)
          .map(_=> Ok(Json.obj("guid" -> advert.guid)))
          .getOrElse(BadRequest(Json.obj("status" -> "KO", "message" -> s"advert with guid=$guid already exist"))) 
      }
      case errors : JsError => BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))
    }
  }

  def getAdvert(guid: String) = Action {
    service.get(guid)
      .map(adv => Ok(Json.toJson[AdvertInfo](adv)))
      .getOrElse(NotFound(advertNotFoundJson(guid))) 
  }


  def updateAdvert(guid: String) = Action(BodyParsers.parse.json) { request =>
    request.body.validate[AdvertInfo] match {
      case JsSuccess(advert, _) => service.update(guid, advert)
                                    .map(_=>Ok)
                                    .getOrElse(NotFound(advertNotFoundJson(guid)))
      case errors: JsError => BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))
     }
  }

  def deleteAdvert(guid: String) = Action {
   service.delete(guid)
     .map(_=>Ok)
     .getOrElse(NotFound(advertNotFoundJson(guid))) 
  }

  def getAllAdverts = Action {
    Ok(Json.obj("adverts" -> service.getAll))
  }
  

}

object Application extends Application(ServiceInterpreter)