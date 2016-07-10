package model

import java.util.UUID

import com.amazonaws.services.dynamodbv2.document.Item
import infrastructure.DynamoDb

import scala.collection.JavaConverters._


class DynamoInterpreter(db:DynamoDb) extends AdvertService[AdvertInfo, UUID]{

  override def getAll: List[AdvertInfo] = {
    try {
       val items = db.advertsTable.scan()
       items.asScala.map(toDomainAdvert).toList
    }catch {
      case error: Throwable => {error.printStackTrace(System.err); List.empty}
    }
  }

  def toDomainAdvert(item: Item): AdvertInfo ={
    AdvertInfo(UUID.fromString(item.getString("guid")),
      item.getString("title"),
      item.getString("fuel"),
      item.getInt("price"))
  }
  
  override def get(id: UUID): Option[AdvertInfo] = {
   try {
     val item = db.advertsTable.getItem("guid", id.toString)
     Some(toDomainAdvert(item))
   }catch {
     case error: Throwable => {error.printStackTrace(System.err); None}
   }
  }

  override def saveOrUpdate(advert: AdvertInfo): Option[AdvertInfo] = {
    try {
         val item = new Item
         db.advertsTable.putItem(item
           .withPrimaryKey("guid", advert.guid.toString)
           .withString("title", advert.title)
           .withString("fuel", advert.fuel)
           .withNumber("price", advert.price))
         Some(advert)
    }catch {
      case error : Throwable => {error.printStackTrace(System.err); None}
    }
  }

  override def remove(advert: AdvertInfo): Option[AdvertInfo] = {
    try {
        db.advertsTable.deleteItem("guid", advert.guid.toString)
        Some(advert)
    }catch {
      case error : Throwable => {error.printStackTrace(System.err); None}
    }
  }
}

