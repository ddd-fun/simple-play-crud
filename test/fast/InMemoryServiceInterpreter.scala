package fast

import java.util.UUID

import model.{AdvertAction, AdvertNotFound, AdvertInfo, AdvertService}

import scalaz.{-\/, \/-}


object InMemoryServiceInterpreter extends AdvertService[AdvertInfo, UUID]{

  private val inMemoryDb = scala.collection.mutable.Map.empty[UUID, AdvertInfo]

  def get(id: UUID): AdvertAction[AdvertInfo] = {
    inMemoryDb.get(id).map(a => \/-(a)).getOrElse(-\/(AdvertNotFound))
  }

  def saveOrUpdate(advert: AdvertInfo): AdvertAction[AdvertInfo] = {
    inMemoryDb += (advert.guid -> advert)
    \/-(advert)
  }

  def remove(advert: AdvertInfo): AdvertAction[AdvertInfo] = {
    inMemoryDb -= advert.guid
    \/-(advert)
  }

  def getAll: AdvertAction[List[AdvertInfo]] ={
   val result = inMemoryDb.values.foldLeft(List[AdvertInfo]())((l,info) => info :: l)
   \/-(result)
  }
}
