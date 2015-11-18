package ch.openolitor

import akka.actor.Actor
import spray.routing._
import spray.http._
import spray.http.MediaTypes._
import spray.httpx.marshalling.ToResponseMarshallable._
import spray.httpx.SprayJsonSupport._
import spray.routing.Directive.pimpApply
import spray.json._
import DefaultJsonProtocol._ // if you don't supply your own Protocol (see below)

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class HelloWorldServiceActor extends Actor with HelloWorldService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)
}

case class HelloWorld(message: String)

object OpenOlitorJsonProtocol extends DefaultJsonProtocol {
  implicit val helloWorldFormat = jsonFormat1(HelloWorld)
}

// this trait defines our service behavior independently from the service actor
trait HelloWorldService extends HttpService {

  import OpenOlitorJsonProtocol._

  val myRoute =
    path("hello" / "xml") {
      get {
        respondWithMediaType(`text/xml`) { // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            <html>
              <body>
                <h1>Hello World</h1>
              </body>
            </html>
          }
        }
      }
    } ~
      path("hello" / "json") {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              HelloWorld("Hello World!")
            }
          }
        }
      }
}