package model


trait AdvertService[Advert, Id] extends Repository[Advert, Id] {

  def store(id:Id, advert: Advert): Option[Advert] = for {
    _ <- invert(get(id))
    s <- saveOrUpdate(advert)
  }yield s

  def update(id:Id, advert: Advert) : Option[Advert] = for{
    _ <- get(id)
    u <- saveOrUpdate(advert)
  }yield u

  def delete(id: Id) : Option[Advert] = for{
    a <- get(id)
    d <- remove(a)
  }yield d

  def invert[A](o:Option[A]): Option[A]
}

trait Repository[Advert, Id] {

  def getAll : List[Advert]

  def saveOrUpdate(advert: Advert) : Option[Advert]

  def remove(advert: Advert) : Option[Advert]

  def get(id:Id) : Option[Advert]

}