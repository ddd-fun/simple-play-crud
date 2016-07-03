package model


object ServiceInterpreter extends AdvertService[AdvertInfo, String]{

  private val inMemoryDb = scala.collection.mutable.Map.empty[String, AdvertInfo]

  def get(id: String): Option[AdvertInfo] = inMemoryDb.get(id)

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
