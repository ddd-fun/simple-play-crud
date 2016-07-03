package model


case class AdvertInfo(guid:String, title:String, fuel:String, price:Int, usage: Option[CarUsage])
case class CarUsage(mileage:Int, firstReg:String)
