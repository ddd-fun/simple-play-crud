import scalaz._

package object model {

  type AdvertAction[+A] = \/[Error, A]

  implicit class ResultOperations(opt: AdvertAction[_]){
    def invert : AdvertAction[Unit] = {
      opt match {
        case \/-(_) => -\/(AdvertAlreadyExist)
        case -\/(AdvertNotFound) => \/-()
        case e@ -\/(_)=> e
      }
    }
  }

  trait Error
  case object AdvertNotFound extends Error
  case object AdvertAlreadyExist extends Error
  case class DbAccessError(msg:String) extends Error

}
