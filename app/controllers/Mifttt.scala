package controllers

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._
import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.ws._
import play.api.mvc._
import play.filters.csrf._
import jp.t2v.lab.play2.auth.LoginLogout
import models._
import services._
import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._

object MIfttt extends Controller {
  private val logger = Logger(this.getClass())

  def archvPind = Action.async { implicit req =>
    // todo: parameterize number of result to retrieve 
    val res = for {
      favs <- getPocketFavs
      ifpind <- getPinedbyIF(favs)
      result <- archvPocketIFPind(favs, ifpind)
    } yield (result)

    // todo: render the result in a html table
    res.map(xs => Ok(xs.map(_.toString).reduce(_ + "\n" + _)))
  }

  def getPocketFavs =
    Pocket.retrieveUrls(favorite = Some(true), count = Some(100))

  def getPinedbyIF(favs: Seq[(Int, String)]) =
    if (favs.isEmpty) Future.successful(Nil)
    else Pinboard.getPostUrls(tag = Some(Seq("IFTTT")), results = Some(100), fromdt = Some(LocalDateTime.now - 1.months))

  def archvPocketIFPind(favs: Seq[(Int, String)], ifpind: Seq[String]): Future[Seq[(Int, String, String)]] = {
    import Pocket._
    val (toArchv, notyet) = favs.partition(fav => ifpind.contains(fav._2))

    logger.debug(s"favs urls: ${favs.length}, ifpind: ${ifpind.length}")
    logger.debug(s"pined urls: ${toArchv.length}, not yet: ${notyet.length}")

    val fresult =
      if (toArchv.nonEmpty) {
        val req = toArchv.map(fav => Seq((UnFavorite, fav._1, fav._2), (Archive, fav._1, fav._2))).flatten
        val results = Pocket.modifyUrls(req.map(r => (r._1, r._2)))
        Pocket.modifyUrls(req.map(r => (r._1, r._2)))
          .map {
            _.zip(req).map { case ((b, (_, id, url))) => (id, url, if (b) { "done" } else { "error" }) }
          }
      } else {
        Future.successful(Nil)
      }

    fresult.map(_ ++ notyet.map(n => (n._1, n._2, "not pined")))
  }
}

