import java.util.UUID

import scalaz._

package object model {

  type AdvertAction[+A] = \/[Error, A]

  trait Error
  case class AdvertNotFound(guid:UUID) extends Error
  case class AdvertAlreadyExist(guid:UUID) extends Error
  case class DbAccessError(msg:String) extends Error

}
