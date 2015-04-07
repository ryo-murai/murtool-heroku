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

object Pocket {
  private val logger = Logger(this.getClass())
  private val consumerKey = Properties.envOrNone("POCKET_CONSUMER_KEY").get
  private val accessToken = Properties.envOrNone("POCKET_ACCESS_TOKEN").get

  private val requestForRetrieve = createRequest("https://getpocket.com/v3/get")
  private val requestForModify = createRequest("https://getpocket.com/v3/send")

  def retrieve(
    favorite: Option[Boolean] = None,
    tag: Option[String] = None,
    count: Option[Int] = None,
    since: Option[DateTime] = None): Future[WSResponse] = {

    val jsonBase = Json.obj(
      "consumer_key" -> consumerKey,
      "access_token" -> accessToken,
      "sort" -> "newest",
      "detailType" -> "complete")

    val reqJson = List(
      favorite.map(b => ("favorite", Json.toJson(if (b) 1 else 0))),
      tag.map(t => ("tag", Json.toJson(t))),
      count.map(c => ("count", Json.toJson(c))),
      since.map(d => ("since", Json.toJson(d.getMillis() / 1000))))
      .flatten
      .foldRight(jsonBase)((e, json) => json + e)

    trcjson(reqJson)

    requestForRetrieve.post(reqJson).flatMap(resultCheck)
  }

  def retrieveUrls(
    favorite: Option[Boolean] = None,
    tag: Option[String] = None,
    count: Option[Int] = None,
    since: Option[DateTime] = None): Future[Seq[(Int, String)]] =
    retrieve(favorite, tag, count, since).map(parseJson)

  def parseJson: WSResponse => Seq[(Int, String)] = { resp =>
    dbgheaders(resp)
    (trcjson(resp.json) \ "list") match {
      case list @ JsObject(_) => list.values.map {
        _ match {
          case elem @ JsObject(_) =>
            ((elem \ "item_id").as[String].toInt, (elem \ "resolved_url").as[String])
        }
      }.toSeq
    }
  }

  sealed abstract class ModifyAction(val label: String)
  case object Archive extends ModifyAction("archive")
  case object ReAdd extends ModifyAction("readd")
  case object Favorite extends ModifyAction("favorite")
  case object UnFavorite extends ModifyAction("unfavorite")
  case object Delete extends ModifyAction("delete")

  def modify(items: Seq[(ModifyAction, Int)]): Future[WSResponse] = {
    val reqJson = Json.obj(
      "consumer_key" -> consumerKey,
      "access_token" -> accessToken,
      "actions" -> JsArray(items.map { case ((act, id)) => Json.obj("action" -> act.label, "item_id" -> id) })
    )
    trcjson(reqJson)

    requestForModify.post(reqJson).flatMap(resultCheck)
  }

  def modifyUrls(items: Seq[(ModifyAction, Int)]): Future[Seq[Boolean]] = modify(items).map(parseResultJson)

  def parseResultJson(resp: WSResponse): Seq[Boolean] =
    (trcjson(dbgheaders(resp).json) \ "action_results").as[Seq[Boolean]]

  def resultCheck(resp: WSResponse): Future[WSResponse] = {
    resultCheckCond(resp)(_.status == 200)
  }

  def resultCheckCond(resp: WSResponse)(checker: WSResponse => Boolean): Future[WSResponse] = {
    if (checker(resp)) Future.successful(resp)
    else Future.failed(new IllegalStateException(s"${resp.status} / ${resp.body}"))
  }

  private def createRequest(url: String, timeout: Int = 10000) = {
    WS.url(url)
      .withHeaders(
        "X-Accept" -> "application/json",
        "Content-Type" -> "application/json; charset=UTF-8")
      .withRequestTimeout(timeout)
  }

  private def dbgheaders(resp: WSResponse): WSResponse = {
    logger.debug("print response headers")
    resp.allHeaders.foreach { h => logger.debug(s"${h._1}: ${h._2}") }
    resp
  }

  private def trcjson(js: JsValue): JsValue = {
    logger.trace("printing json ================================")
    logger.trace(Json.prettyPrint(js))
    js
  }
}
