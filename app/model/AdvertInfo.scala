package model

import java.util.UUID

case class AdvertInfo(guid:UUID, title:String, fuel:String, price:Int, usage: Option[CarUsage])
case class CarUsage(mileage:Int, firstReg:String)
