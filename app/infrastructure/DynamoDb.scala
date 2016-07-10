package infrastructure

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBClient, AmazonDynamoDB}


trait DynamoDb {

  def setUp:SetUp

  lazy val client:AmazonDynamoDB = {
    val c = new AmazonDynamoDBClient(new BasicAWSCredentials(setUp.accKeyId, setUp.accKey))
        c.withEndpoint(setUp.url)
  }
  lazy val dynamoDB = new DynamoDB(client);
  lazy val advertsTable = dynamoDB.getTable(setUp.tbName);
}

case class SetUp(url:String, accKeyId:String, accKey:String, tbName:String="Adverts")