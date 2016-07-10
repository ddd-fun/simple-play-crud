package slow

import java.util
import java.util.UUID


import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClient}
import com.amazonaws.services.dynamodbv2.document.{Table, DynamoDB}
import com.amazonaws.services.dynamodbv2.model._
import com.typesafe.config.ConfigFactory
import fast.DomainDataGen
import model.{AdvertInfo, DynamoInterpreter}
import org.specs2.execute.{Result, AsResult}
import org.specs2.specification.{BeforeAll, Scope}
import org.specs2.mutable.Around
import play.api.test.PlaySpecification
import play.api.test._

object DynamoRepositorySpec extends PlaySpecification with DomainDataGen with BeforeAll {


  override def beforeAll() = {
    TestDatabase.deleteTable
    TestDatabase.createTable
  }

  object Sut extends DynamoInterpreter(TestDatabase)

  "DynamoInterpreter" should {

    "save advert and then fetch saved advert" in  {

      val advert = AdvertInfo(UUID.randomUUID, "Audi a4", "diesel", 500)

      val stored = Sut.saveOrUpdate(advert)

      stored must equalTo(Some(advert)).setMessage("advert was not stored")

      val fetch = Sut.get(advert.guid)

      fetch must equalTo( Some(advert)).setMessage("advert was not fetched")

    }


   }

}

import infrastructure._

object TestDatabase extends DynamoDb {

  val setUp = SetUp("http://localhost:8000", "dynamodb.secretAccessKeyId", "dynamodb.secretAccessKey")

  def createTable = {
    try {
      //System.out.println("Attempting to create table; please wait...");
      val table = dynamoDB.createTable(setUp.tbName,
        util.Arrays.asList(new KeySchemaElement("guid", KeyType.HASH)),
        util.Arrays.asList(new AttributeDefinition("guid", ScalarAttributeType.S)),
        new ProvisionedThroughput(10L, 10L));
      table.waitForActive();
      //System.out.println("Success.  Table status: " + table.getDescription().getTableStatus());
    } catch {
      case ex: Throwable => System.err.println("Unable to create table: " + ex.getMessage); throw ex;
    }
  }

  def deleteTable = {
    try {
      //System.out.println("Attempting to create table; please wait...");
       advertsTable.delete()
       advertsTable.waitForDelete()
      //System.out.println("Success.  Table status: " + table.getDescription().getTableStatus());
    } catch {
      case notFoundEx : ResourceNotFoundException => System.out.println("Unable to delete table: " + notFoundEx.getMessage);
      case ex: Throwable => System.err.println("Unable to create table: " + ex.getMessage); throw ex;
    }
  }

}

