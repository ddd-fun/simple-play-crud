package controllers

import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.functional.syntax._

object Application extends Controller {

  private val inMemoryDb = scala.collection.mutable.Map.empty[String, AdvertInfo]

  implicit val advertReads: Reads[AdvertInfo] = (
                   (JsPath \ "guid").read[String] and
                   (JsPath \ "title").read[String] and
                   (JsPath \ "fuel").read[String] and
                   (JsPath \ "price").read[Int]
  )(AdvertInfo.apply _)


  implicit val advertWrites: Writes[AdvertInfo] = (
     (JsPath \ "guid").write[String] and
     (JsPath \ "title").write[String] and
     (JsPath \ "fuel").write[String] and
     (JsPath \ "price").write[Int]
  )(unlift(AdvertInfo.unapply))


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

case class AdvertInfo(guid:String, title:String, fuel:String, price:Int)
