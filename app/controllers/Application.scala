package controllers

import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.functional.syntax._

object Application extends Controller {

  implicit val advertReads: Reads[Advert] = (
                   (JsPath \ "guid").read[String] and
                   (JsPath \ "title").read[String] and
                   (JsPath \ "fuel").read[String]
  )(Advert.apply _)


  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }


  def addAdvert = Action(BodyParsers.parse.json){ request =>
    request.body.validate[Advert] match  {
      case JsSuccess(advert, _) =>  Ok(Json.obj("guid" -> advert.guid))
      case errors : JsError => BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))
    }
  }


}

case class Advert(guid:String, title:String, fuel:String)
