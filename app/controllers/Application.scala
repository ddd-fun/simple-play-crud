package controllers

import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.functional.syntax._

object Application extends Controller {

  private val inMemoryDb = scala.collection.mutable.Map.empty[String, AdvertInfo]

  implicit val advertReads: Reads[AdvertInfo] = (
                   (JsPath \ "title").read[String] and
                   (JsPath \ "fuel").read[String] and
                   (JsPath \ "price").read[Int]
  )(AdvertInfo.apply _)


  implicit val advertWrites: Writes[AdvertInfo] = (
     (JsPath \ "title").write[String] and
     (JsPath \ "fuel").write[String] and
     (JsPath \ "price").write[Int]
  )(unlift(AdvertInfo.unapply))


  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }


  def addAdvert = Action(BodyParsers.parse.json){ request =>
    request.body.validate[AdvertInfo] match  {
      case JsSuccess(advert, _) =>  {  val guid = save(advert); Ok(Json.obj("guid" -> guid)) }
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


  def get(guid:String): Either[String, AdvertInfo] = {
    inMemoryDb.get(guid) match {
      case Some(adv) =>  Right(adv)
      case _ =>  Left(s"advert by guid=$guid is not found")
    }
  }

  def save(advert: AdvertInfo) : String = {
    val guid = genGUID;
    inMemoryDb += (guid -> advert)
    guid
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

  def genGUID = java.util.UUID.randomUUID.toString

}

case class AdvertInfo(title:String, fuel:String, price:Int)
