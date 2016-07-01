package controllers

import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.functional.syntax._

object Application extends Controller {

  private val inMemoryDb = scala.collection.mutable.Map.empty[String, AddAdvertRequest]

  implicit val advertReads: Reads[AddAdvertRequest] = (
                   (JsPath \ "title").read[String] and
                   (JsPath \ "fuel").read[String] and
                   (JsPath \ "price").read[Int]
  )(AddAdvertRequest.apply _)


  implicit val advertWrites: Writes[AddAdvertRequest] = (
     (JsPath \ "title").write[String] and
     (JsPath \ "fuel").write[String] and
     (JsPath \ "price").write[Int]
  )(unlift(AddAdvertRequest.unapply))


  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }


  def addAdvert = Action(BodyParsers.parse.json){ request =>
    request.body.validate[AddAdvertRequest] match  {
      case JsSuccess(advert, _) =>  {  val guid = store(advert); Ok(Json.obj("guid" -> guid)) }
      case errors : JsError => BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))
    }
  }

  def getAdvert(guid: String) = Action {
    inMemoryDb.get(guid) match {
      case Some(adv) => Ok(Json.toJson[AddAdvertRequest](adv))
      case _=> NotFound(Json.obj("status" -> "KO", "message" -> s"advert by guid=$guid is not found"))
    }
  }



  def store(advert: AddAdvertRequest) : String = {
    val guid = genGUID;
    inMemoryDb += (guid -> advert)
    guid
  }

  def genGUID = java.util.UUID.randomUUID.toString

}

case class AddAdvertRequest(title:String, fuel:String, price:Int)
