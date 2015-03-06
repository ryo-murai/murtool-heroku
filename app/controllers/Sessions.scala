package controllers

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import jp.t2v.lab.play2.auth.LoginLogout
import models._

object Sessions extends Controller with LoginLogout with AdminAuthConfig {
  val loginForm = Form {
    mapping("user" -> nonEmptyText, "password" -> text)(Admin.get.authenticate)(_.map(a => (a.user, "")))
      .verifying("Invalid user name or password", result => result.isDefined)
  }

  def login = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }

  def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(views.html.login(formWithErrors))),
      admin => gotoLoginSucceeded(admin.get.user)
    )
  }
}
