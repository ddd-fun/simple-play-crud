package fast


import org.scalacheck.{Prop, Gen}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.commands.Commands
import play.api.libs.json.JsObject
import play.api.test.{FakeHeaders, FakeRequest}

import scala.concurrent.Future
import scala.util.{Try, Success}
import scala.collection.immutable.Map
import play.api.test.Helpers._

object AppStateProperties extends org.scalacheck.Properties("Advert app state") {

  property("state property") = AppStateSpec.property()

}

object AppStateSpec extends Commands{

  object TestApplication extends controllers.Application

  case class State(map:Set[String])

  type Sut = controllers.Application

  def canCreateNewSut(newState: State,
                      initSuts: Traversable[State],
                      runningSuts: Traversable[Sut]): Boolean = initSuts.isEmpty && runningSuts.isEmpty

  def destroySut(sut: Sut): Unit = ()

  def initialPreCondition(state: State): Boolean = state.map.isEmpty

  def genInitialState: Gen[State] = State(Set.empty[String])

  def newSut(state: State): Sut = TestApplication

  import ControllerValidationSpec._
  import play.api.libs.json._
  import DomainDataGen._

  def genCommand(state: State): Gen[AppStateSpec.Command] = {
    Gen.frequency((10, genStoreAdvert),
                  (10, genGetStoredAdvert(state)),
                  (1,  genGetNotExistedAdvert(state)),
                  (5,  genDeleteStoredAdvert(state)),
                  (10, genDeleteNotExistedAdvert(state)),
                  (5,  genUpdateStoredAdvert(state)) )
  }

  def genStoreAdvert =
    validJsonGen.map(json => AddAdvert((json \ "guid").as[String], json))

  def genGetStoredAdvert(state:State) =
    Gen.oneOf(state.map.toSeq).map(GetStoredAdvert)

  def genDeleteStoredAdvert(state:State) =
    Gen.oneOf(state.map.toSeq).map(DeleteStoredAdvert)

  def genDeleteNotExistedAdvert(state:State) =
    validIdGen.map(GetNotExistedAdvert)

  def genGetNotExistedAdvert(state:State) =
    validIdGen.suchThat(!state.map.contains(_)).map(GetNotExistedAdvert)

  def genUpdateStoredAdvert(state:State) =
    Gen.oneOf(state.map.toSeq).flatMap(id => validJsonGen.map(UpdateStoredAdvert(id, _)) )


  case class AddAdvert(id: String, jsObject: JsObject) extends Command {

    type Result = Future[play.api.mvc.Result]

    def run(sut: Sut) = sut.addAdvert.apply(genFakeRequest(jsObject))

    def preCondition(state: State) = !state.map.contains(id)

    def nextState(state: State) = state.copy(map = state.map + id)

    def postCondition(state: State, result: Try[Result]) =
      Prop(result.map(response => status(response).equals(OK) ).getOrElse(false))

  }

  case class GetStoredAdvert(id: String) extends Command {

    type Result = Future[play.api.mvc.Result]

    def run(sut: Sut) = sut.getAdvert(id).apply(FakeRequest("GET", s"/adverts/$id"))

    def preCondition(state: State) = state.map.contains(id)

    def nextState(state: State) = state

    def postCondition(state: State, result: Try[Result]) =
      Prop(result.map(response => status(response).equals(OK) ).getOrElse(false))

  }

  case class GetNotExistedAdvert(id:String)  extends Command {

    type Result = Future[play.api.mvc.Result]

    def run(sut: Sut) = sut.getAdvert(id).apply(FakeRequest("GET", s"/adverts/$id"))

    def preCondition(state: State) = !state.map.contains(id)

    def nextState(state: State) = state

    def postCondition(state: State, result: Try[Result]) =
      Prop(result.map(response => status(response).equals(NOT_FOUND) ).getOrElse(false))

  }


  case class DeleteStoredAdvert(id:String)  extends Command {

    type Result = Future[play.api.mvc.Result]

    def run(sut: Sut) = sut.deleteAdvert(id).apply(FakeRequest("DELETE", s"/adverts/$id"))

    def preCondition(state: State) = state.map.contains(id)

    def nextState(state: State) = state.copy(state.map - id)

    def postCondition(state: State, result: Try[Result]) =
     Prop(result.map(response => status(response).equals(OK) ).getOrElse(false))

  }

  case class DeleteNotExistedAdvert(id: String) extends Command {

    type Result = Future[play.api.mvc.Result]

    def run(sut: Sut) = sut.deleteAdvert(id).apply(FakeRequest("DELETE", s"/adverts/$id"))

    def preCondition(state: State) = !state.map.contains(id)

    def nextState(state: State) = state

    def postCondition(state: State, result: Try[Result]) =
     Prop(result.map(response => status(response).equals(NOT_FOUND) ).getOrElse(false))
  }


  case class UpdateStoredAdvert(id:String, jsObject: JsObject)  extends Command {

    type Result = Future[play.api.mvc.Result]

    def run(sut: Sut) = sut.editAdvert(id).apply(genFakeRequest(jsObject))

    def preCondition(state: State) = state.map.contains(id)

    def nextState(state: State) = state

    def postCondition(state: State, result: Try[Result]) =
      Prop(result.map(response => status(response).equals(OK) ).getOrElse(false))

  }


}
