import java.util.UUID

import scalaz._

package object model {

  type AdvertAction[+A] = \/[Error, A]

  implicit class ResultOperations(opt: AdvertAction[_]){
    def invert : AdvertAction[Unit] = {
      opt match {
        case \/-(a:AdvertInfo) => -\/(AdvertAlreadyExist(a.guid))
        case -\/(AdvertNotFound(_)) => \/-(():Unit)
        case e@ -\/(_)=> e
      }
    }
  }

  trait Error
  case class AdvertNotFound(guid:UUID) extends Error
  case class AdvertAlreadyExist(guid:UUID) extends Error
  case class DbAccessError(msg:String) extends Error

}
