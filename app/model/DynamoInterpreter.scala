package model

import java.util.UUID

import com.amazonaws.services.dynamodbv2.document.Item
import infrastructure.DynamoDb
import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

class DynamoInterpreter(db:DynamoDb) extends AdvertService[AdvertInfo, UUID] with Mapper {

  override def getAll: AdvertAction[List[AdvertInfo]] = {
    try {
      val items = db.advertsTable.scan()
      items.asScala.map(toDomainAdvert).toList.point[AdvertAction]
    } catch {
      case error: Throwable => {
        error.printStackTrace(System.err);
        -\/(DbAccessError(error.getMessage))
      }
    }
  }

  override def get(id: UUID): AdvertAction[AdvertInfo] = {
    try {
      val item = db.advertsTable.getItem("guid", id.toString)
      Option(item).map(i => \/-(toDomainAdvert(i))).getOrElse(-\/(AdvertNotFound(id)))
    } catch {
      case error: Throwable => {
        error.printStackTrace(System.err);
        -\/(DbAccessError(error.getMessage))
      }
    }
  }

  override def saveOrUpdate(advert: AdvertInfo): AdvertAction[AdvertInfo] = {
    try {
      db.advertsTable.putItem(toDynamoItem(advert))
      advert.point[AdvertAction]
    } catch {
      case error: Throwable => {
        error.printStackTrace(System.err);
        -\/(DbAccessError(error.getMessage))
      }
    }
  }

  override def remove(advert: AdvertInfo): AdvertAction[AdvertInfo] = {
    try {
      db.advertsTable.deleteItem("guid", advert.guid.toString)
      advert.point[AdvertAction]
    } catch {
      case error: Throwable => {
        error.printStackTrace(System.err);
        -\/(DbAccessError(error.getMessage))
      }
    }

  }
}
trait Mapper{

  def toDomainAdvert(item: Item): AdvertInfo ={
    val advert = AdvertInfo(UUID.fromString(
      item.getString("guid")),
      item.getString("title"),
      item.getString("fuel"),
      item.getInt("price"));
    if(item.hasAttribute("mileage") && item.hasAttribute("firstReg")){
      advert.copy(usage = Some(CarUsage(item.getInt("mileage"), item.getString("firstReg"))))
    } else advert
  }

  def toDynamoItem(advert: AdvertInfo): Item ={
    val item = new Item
    item.withPrimaryKey("guid", advert.guid.toString)
      .withString("title", advert.title)
      .withString("fuel", advert.fuel)
      .withNumber("price", advert.price)
    advert.usage match {
      case Some(u) => {item.withInt("mileage", u.mileage)
        .withString("firstReg", u.firstReg) }
      case _=> item
    }
  }

}
