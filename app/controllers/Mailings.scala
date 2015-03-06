package controllers

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Properties => Props }
import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import play.filters.csrf._
import jp.t2v.lab.play2.auth.LoginLogout
import com.sendgrid.SendGrid
import com.sendgrid.SendGrid._
import models._

object Mailings extends Controller with LoginLogout with AdminAuthConfig {
  val mailMsgForm = Form {
    mapping(
      "to" -> optional(email),
      "cc" -> optional(email),
      "bcc" -> optional(email),
      "from" -> optional(email),
      "replyTo" -> optional(email),
      "subject" -> optional(text),
      "body" -> optional(text)
    )(EmailMessage.apply)(EmailMessage.unapply)
  }

  def edit = CSRFAddToken {
    Action { implicit request =>
      Ok(views.html.editMail(mailMsgForm))
    }
  }

  def send = Action { implicit request =>
    mailMsgForm.bindFromRequest.fold(
      formWithErrors =>
        BadRequest(views.html.editMail(formWithErrors)),
      mail => {
        Ok(sendMessage(mail))
      }
    )
  }

  private def sendMessage(mail: EmailMessage) = {
    def sendgrid: Option[SendGrid] = {
      (for {
        userid <- Props.envOrNone("SENDGRID_USERNAME")
        passwd <- Props.envOrNone("SENDGRID_PASSWORD")
      } yield (new SendGrid(userid, passwd)))
      // proxy
    }

    def email: EmailMessage => Email = { msg =>
      val email = new Email()
      msg.to.foreach { email.addTo(_) }
      msg.cc.foreach { email.addCc(_) }
      msg.bcc.foreach { email.addBcc(_) }
      msg.from.foreach { email.setFrom(_) }
      msg.replyTo.foreach { email.setReplyTo(_) }
      msg.subject.foreach { email.setSubject(_) }
      msg.body.foreach { email.setText(_) }
      email
    }

    sendgrid.map { sender =>

      Logger.debug("about to send a message: $msg")

      sender.send(email(mail))
    }.map(_.toString).getOrElse("failed to read the system env")
  }
}
