package fast

import model.ServiceInterpreter
import org.scalacheck.{Prop, Gen}
import org.scalacheck.commands.Commands
import play.api.test.{FakeHeaders, FakeRequest}
import scala.concurrent.Future
import scala.util.{Try}
import play.api.test.Helpers._

object AppStateSpec extends org.scalacheck.Properties("Application") {

  property("state transitions") = AppStateTransitions.property()

}

object AppStateTransitions extends Commands with DomainDataGen{

  object TestApplication extends controllers.Application(ServiceInterpreter)

  case class State(map:Set[String])

  type Sut = controllers.Application

  def canCreateNewSut(newState: State,
                      initSuts: Traversable[State],
                      runningSuts: Traversable[Sut]): Boolean = initSuts.isEmpty && runningSuts.isEmpty

  def destroySut(sut: Sut): Unit = ()

  def initialPreCondition(state: State): Boolean = state.map.isEmpty

  def genInitialState: Gen[State] = State(Set.empty[String])

  def newSut(state: State): Sut = TestApplication

  import play.api.libs.json._

  def genCommand(state: State): Gen[AppStateTransitions.Command] = {
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


  def fakeRequest(method:String, json:JsObject) = new FakeRequest(
    method = method, uri = "/adverts",
    headers = FakeHeaders(Seq("Content-type"->"application/json")),
    body =  json
  ){override def toString = s"$method $uri body: $body"}



  case class AddAdvert(id: String, jsObject: JsObject) extends Command {

    type Result = Future[play.api.mvc.Result]

    def run(sut: Sut) = sut.addAdvert.apply(fakeRequest("POST", jsObject))

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

    def run(sut: Sut) = sut.updateAdvert(id).apply(fakeRequest("PUT", jsObject))

    def preCondition(state: State) = state.map.contains(id)

    def nextState(state: State) = state

    def postCondition(state: State, result: Try[Result]) =
      Prop(result.map(response => status(response).equals(OK) ).getOrElse(false))

  }


}
