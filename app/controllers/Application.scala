package controllers

import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.functional.syntax._

object Application extends Controller {

  private val inMemoryDb = scala.collection.mutable.Map.empty[String, AddAdvertRequest]

  implicit val advertReads: Reads[AddAdvertRequest] = (
                   (JsPath \ "title").read[String] and
                   (JsPath \ "fuel").read[String]
  )(AddAdvertRequest.apply _)


  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }


  def addAdvert = Action(BodyParsers.parse.json){ request =>
    request.body.validate[AddAdvertRequest] match  {
      case JsSuccess(advert, _) =>  {  val guid = store(advert); Ok(Json.obj("guid" -> guid)) }
      case errors : JsError => BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))
    }
  }

  def store(advert: AddAdvertRequest) : String = {
    val guid = genGUID;
    inMemoryDb += (guid -> advert)
    guid
  }

  def genGUID = java.util.UUID.randomUUID.toString

}

case class AddAdvertRequest(title:String, fuel:String)
