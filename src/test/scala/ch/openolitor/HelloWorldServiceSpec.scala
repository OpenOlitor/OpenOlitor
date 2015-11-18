package ch.openolitor

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http._
import StatusCodes._
import spray.json._
import OpenOlitorJsonProtocol._

class HelloWorldServiceSpec extends Specification with Specs2RouteTest with HelloWorldService {
  def actorRefFactory = system

  "HelloWorldService" should {

    "return a greeting for GET requests to the root path as xml" in {
      Get("/hello/xml") ~> myRoute ~> check {
        responseAs[String] must contain("<h1>Hello World</h1>")
      }

      "return a greeting for GET requests to the root path as json" in {
        Get("/hello/json") ~> myRoute ~> check {
          responseAs[String].parseJson.convertTo[HelloWorld] must beEqualTo(HelloWorld("Hello World!"))
        }
      }
    }

    "leave GET requests to other paths unhandled" in {
      Get("/kermit") ~> myRoute ~> check {
        handled must beFalse
      }
    }

    "return a MethodNotAllowed error for PUT requests to the root path" in {
      Put("/hello/xml") ~> sealRoute(myRoute) ~> check {
        status === MethodNotAllowed
        responseAs[String] === "HTTP method not allowed, supported methods: GET"
      }
    }
  }
}
