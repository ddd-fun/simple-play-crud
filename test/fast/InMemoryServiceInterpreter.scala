package fast

import java.util.UUID

import model.{AdvertInfo, AdvertService}


object InMemoryServiceInterpreter extends AdvertService[AdvertInfo, UUID]{

  private val inMemoryDb = scala.collection.mutable.Map.empty[UUID, AdvertInfo]

  def get(id: UUID): Option[AdvertInfo] = inMemoryDb.get(id)

  def saveOrUpdate(advert: AdvertInfo): Option[AdvertInfo] = {
    inMemoryDb += (advert.guid -> advert)
    Some(advert)
  }

  def remove(advert: AdvertInfo): Option[AdvertInfo] = {
    inMemoryDb -= advert.guid
    Some(advert)
  }

  def getAll: List[AdvertInfo] =
    inMemoryDb.values.foldLeft(List[AdvertInfo]())((l,info) => info :: l)

}
