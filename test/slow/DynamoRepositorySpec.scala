package slow

import java.util
import java.util.UUID

import com.amazonaws.services.dynamodbv2.model._
import fast.DomainDataGen
import model.{AdvertAction, CarUsage, AdvertInfo, DynamoInterpreter}
import org.specs2.specification.BeforeAll
import play.api.test.PlaySpecification
import scalaz._
import Scalaz._


object DynamoRepositorySpec extends PlaySpecification with DomainDataGen with BeforeAll {


  override def beforeAll() = {
    TestDatabase.deleteTable
    TestDatabase.createTable
  }

  object Sut extends DynamoInterpreter(TestDatabase)

  "DynamoInterpreter" should {

    "save advert and then get it" in  {

      val advert = AdvertInfo(UUID.randomUUID, "Audi a4", "diesel", 500, CarUsage(15000, "2014-02-29 23:59").toOption)

      val stored = Sut.saveOrUpdate(advert)

      stored must equalTo(\/-(advert)).setMessage("advert was not stored")

      val fetch = Sut.get(advert.guid)

      fetch must equalTo(\/-(advert)).setMessage("advert was not fetched")

    }

    "update stored advert and then get it" in {

      val advert = AdvertInfo(UUID.randomUUID, "Audi a4", "diesel", 500)

      val stored = Sut.saveOrUpdate(advert)

      stored must equalTo(\/-(advert)).setMessage("advert was not stored")

      val updateAdv = advert.copy(title = "Audi A5", fuel ="gasoline", price = 600)

      val updated = Sut.saveOrUpdate(advert.copy(title = "Audi A5", fuel ="gasoline", price = 600))

      updated must equalTo(\/-(updateAdv)).setMessage("advert was not updated")

      val fetched = Sut.get(advert.guid)

      fetched must equalTo(\/-(updateAdv)).setMessage("fetch updated advert")

    }


    "get all returns stored adverts" in {

      val advert = AdvertInfo(UUID.randomUUID, "Audi a4", "diesel", 500)

      val stored = Sut.saveOrUpdate(advert)

      stored must equalTo(\/-(advert)).setMessage("advert was not stored")

      val list = Sut.getAll

      list.getOrElse(List.empty[AdvertInfo]).find(_ == advert) must equalTo(Some(advert)).setMessage("advert must be in returned list")

     }


   }

}

import infrastructure._

object TestDatabase extends DynamoDb {

  val setUp = SetUp("http://localhost:8000", "dynamodb.secretAccessKeyId", "dynamodb.secretAccessKey")

  def createTable = {
    try {
      val table = dynamoDB.createTable(setUp.tbName,
        util.Arrays.asList(new KeySchemaElement("guid", KeyType.HASH)),
        util.Arrays.asList(new AttributeDefinition("guid", ScalarAttributeType.S)),
        new ProvisionedThroughput(10L, 10L));
      table.waitForActive();
    } catch {
      case ex: Throwable => System.err.println("Unable to create table: " + ex.getMessage); throw ex;
    }
  }

  def deleteTable = {
    try {
         advertsTable.delete()
         advertsTable.waitForDelete()
    } catch {
      case notFoundEx : ResourceNotFoundException => System.out.println("Unable to delete table: " + notFoundEx.getMessage);
      case ex: Throwable => System.err.println("Unable to delete table: " + ex.getMessage); throw ex;
    }
  }

}

