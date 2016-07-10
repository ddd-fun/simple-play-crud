package model


trait AdvertService[Advert, Id] extends Repository[Advert, Id] {

  def store(id:Id, advert: Advert): AdvertAction[Advert] = for {
    _ <- get(id).invert
    s <- saveOrUpdate(advert)
  }yield s

  def update(id:Id, advert: Advert): AdvertAction[Advert] = for{
    _ <- get(id)
    u <- saveOrUpdate(advert)
  }yield u

  def delete(id: Id) : AdvertAction[Advert] = for{
    a <- get(id)
    d <- remove(a)
  }yield d

}

trait Repository[Advert, Id] {

  def getAll : AdvertAction[List[Advert]]

  def saveOrUpdate(advert: Advert) : AdvertAction[Advert]

  def remove(advert: Advert) : AdvertAction[Advert]

  def get(id:Id) : AdvertAction[Advert]

}
