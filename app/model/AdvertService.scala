package model


trait AdvertService[Advert, Id] extends Repository[Advert, Id] {

  def store(id:Id, advert: Advert): Option[Advert] = for {
    _ <- get(id).invert
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


  implicit class OptionOperations(opt: Option[_]){
    def invert : Option[Unit] = {
      opt match {
        case Some(_) => None
        case None => Some(Unit)
      }
    }
  }
}

trait Repository[Advert, Id] {

  def getAll : List[Advert]

  def saveOrUpdate(advert: Advert) : Option[Advert]

  def remove(advert: Advert) : Option[Advert]

  def get(id:Id) : Option[Advert]

}
