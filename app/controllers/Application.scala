package controllers

import java.util.UUID

import infrastructure.{SetUp, DynamoDb}
import model._
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scalaz.{-\/, \/-, \/}

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

  def getAdvert(guid: UUID) = Action { unliftResult(
      for{
         adv <- service.get(guid).leftMap(toHttpError)
      }yield(Ok(Json.toJson[AdvertInfo](adv)))
    )
  }

  def addAdvert = Action(BodyParsers.parse.json){ req => unliftResult(
      for{
        adv <- req.body.readAdvert
          _ <- service.store(adv.guid, adv).leftMap(toHttpError)
      } yield Ok(Json.obj("guid" -> adv.guid))
    )
  }

  def updateAdvert(guid: UUID) = Action(BodyParsers.parse.json){ req => unliftResult(
     for{
       adv <- req.body.readAdvert
         _ <- service.update(guid, adv).leftMap(toHttpError)
    } yield Ok )
  }

  def deleteAdvert(guid: UUID) = Action {unliftResult(
      for{
         _ <- service.delete(guid).leftMap(toHttpError)
      }yield(Ok)
    )
  }

  def getAllAdverts = Action { unliftResult(
      for{
         advs <- service.getAll.leftMap(toHttpError)
      }yield(Ok(Json.obj("adverts" -> advs)))
    )
  }

  implicit class JsValueOp(v: JsValue) {
     def readAdvert : \/[Result, AdvertInfo] = {
       v.validate[AdvertInfo] match {
         case JsSuccess(a, _) =>  \/-(a)
         case errors: JsError => -\/(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors))))
       }
     }
  }

  def unliftResult[A](e: \/[A,A]) : A = e match {
    case \/-(r) => r
    case -\/(l) => l
  }


  def toHttpError(e:Error): Result ={
   e match {
       case AdvertNotFound(n) => NotFound(Json.obj("status" -> "KO", "message" -> s"advert by guid=${n} is not found"))
       case AdvertAlreadyExist(e) => BadRequest(Json.obj("status" -> "KO", "message" -> s"advert with guid=${e} already exist"))
       case DbAccessError(m) => InternalServerError(Json.obj("status" -> "KO", "message" -> s"ad access error: ${m}"))
       case err@ _ => InternalServerError(Json.obj("status" -> "KO", "message" -> s"ups! we experienced some error, let us know about this: ${err}"))
    }
  }

}

object Application extends Application(ServiceInterpreter)

object ServiceInterpreter extends DynamoInterpreter(DB)

object DB extends DynamoDb {
  def setUp = {
    val cfg = play.api.Play.current.configuration
    val setUp = for{  u <- cfg.getString("aws.dynamo.url")
                      i <- cfg.getString("aws.dynamo.accessKeyId")
                      k <- cfg.getString("aws.dynamo.accessKey")
                  } yield SetUp(u, i, k)
   setUp.getOrElse(throw new RuntimeException("could not read dynamo config"))
  }
}

