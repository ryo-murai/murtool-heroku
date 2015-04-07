package services

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._
import play.api._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder
import play.api.Play.current
import models._
import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._

object Pinboard {
  private val userName = Properties.envOrNone("PINBOARD_USER").get
  private val apiToken = Properties.envOrNone("PINBOARD_TOKEN").get

  private val xmlprinter = new scala.xml.PrettyPrinter(80, 2)
  private val logger = Logger(this.getClass())

  private val requestForGetPosts = createRequest("https://api.pinboard.in/v1/posts/recent")

  def getPosts(
    tag: Option[Seq[String]] = None,
    results: Option[Int] = None,
    fromdt: Option[LocalDateTime] = None): Future[WSResponse] = {

    val request = List(
      tag.map(tx => ("tag", (tx.mkString(",")))),
      results.map(r => ("count", r.toString)))
      //results.map(r => ("results", r.toString)),
      //fromdt.map(d => ("fromdt", d.toString("yyyy-MM-dd'T'hh:mm:ss'Z'"))))
      .flatten
      .foldRight(requestForGetPosts)((p, req) => req.withQueryString(p))

    logger.debug(s"request query params: ${request.queryString}")

    //todo: error handling
    request.get()
  }

  def getPostUrls(
    tag: Option[Seq[String]] = None,
    results: Option[Int] = None,
    fromdt: Option[LocalDateTime] = None): Future[Seq[String]] =
    getPosts(tag, results, fromdt).map(parsePostsXml)

  def parsePostsXml(resp: WSResponse): Seq[String] =
    (trcxml(resp.xml) \\ "post").map(_ \ "@href").map(_.text)

  private def createRequest(url: String, timeout: Int = 10000) =
    WS.url(url)
      .withRequestTimeout(timeout)
      .withQueryString("auth_token" -> s"$userName:$apiToken")

  private def trcxml(xml: scala.xml.Elem): scala.xml.Elem = {
    logger.trace("print xml ======================================")
    logger.trace(xmlprinter.format(xml))
    xml
  }
}
