package controllers

import play.api._
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

class Application extends Controller{

  private val inMemoryDb = scala.collection.mutable.Map.empty[String, AdvertInfo]


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


  def dateValidator = Reads.pattern("((19|20)\\d\\d)-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01]) ?([0-1][0-9]|2[0-3]):([0-5][0-9])".r, "error.date:yyyy/mm/dd HH:MM")

  def fuelValidator(allowed:List[String]) =
    Reads.of[String].filter(ValidationError("allowed only "+allowed))(allowed.contains)

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }


  def addAdvert = Action(BodyParsers.parse.json){ request =>
    request.body.validate[AdvertInfo] match  {
      case JsSuccess(advert, _) =>  { save(advert) match {
          case Right(guid) => Ok(Json.obj("guid" -> guid))
          case Left(msg) => BadRequest(Json.obj("status" -> "KO", "message" -> msg))
        }
      }
      case errors : JsError => BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))
    }
  }

  def getAdvert(guid: String) = Action {
    get(guid) match {
      case Right(adv) => Ok(Json.toJson[AdvertInfo](adv))
      case _=> NotFound(Json.obj("status" -> "KO", "message" -> s"advert by guid=$guid is not found"))
    }
  }


  def editAdvert(guid: String) = Action(BodyParsers.parse.json) { request =>
    request.body.validate[AdvertInfo] match {
      case JsSuccess(advert, _) =>
          update(guid, advert) match {
            case Right(_) => Ok
            case Left(msg) => NotFound(Json.obj("status" -> "KO", "message" -> msg))
          }
      case errors: JsError => BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))
     }
  }

  def deleteAdvert(guid: String) = Action {
    delete(guid) match {
       case Right(_) => Ok
       case Left(msg) => NotFound(Json.obj("status" -> "KO", "message" -> msg))
    }
  }

  def getAllAdverts = Action {
    Ok(Json.obj("adverts" -> getAll))
  }


  def getAll: List[AdvertInfo] =
    inMemoryDb.values.foldLeft(List[AdvertInfo]())((l,info) => info :: l)


  def get(guid:String): Either[String, AdvertInfo] = {
    inMemoryDb.get(guid) match {
      case Some(adv) =>  Right(adv)
      case _ =>  Left(s"advert by guid=$guid is not found")
    }
  }

  def save(advert: AdvertInfo) : Either[String, String] = {
    inMemoryDb.get(advert.guid) match {
      case None => inMemoryDb += (advert.guid -> advert); Right(advert.guid)
      case Some(a) => val guid = advert.guid; Left(s"advert with guid=$guid already exist")
    }
  }

  def update(guid:String, advert: AdvertInfo): Either[String, AdvertInfo] = {
    inMemoryDb.get(guid) match {
      case Some(_) =>  inMemoryDb += (guid -> advert); Right(advert)
      case _ =>  Left(s"advert by guid=$guid is not found")
    }
  }

  def delete(guid:String): Either[String, AdvertInfo] = {
    inMemoryDb.get(guid) match {
       case Some(adv) => inMemoryDb.remove(guid); Right(adv)
       case _ =>  Left(s"advert by guid=$guid is not found")
    }
  }


}

case class AdvertInfo(guid:String, title:String, fuel:String, price:Int, usage: Option[CarUsage])
case class CarUsage(mileage:Int, firstReg:String)

object Application extends Application