package controllers

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import play.filters.csrf._
import jp.t2v.lab.play2.auth.LoginLogout
import models._

object Admins extends Controller {
  val loginForm = Form {
    mapping("user" -> nonEmptyText, "password" -> text)(Admin.get.authenticate)(_.map(a => (a.user, "")))
      .verifying("Invalid user name or password", result => result.isDefined)
  }

  val editAdminForm = Form {
    mapping(
      "id" -> ignored(Admin.fixedId),
      "user" -> nonEmptyText,
      "password" -> text
    )(Admin.apply)(Admin.unapply)
  }

  def login = CSRFAddToken {
    Action { implicit request =>
      Ok(views.html.login(loginForm, routes.Admins.authenticate))
    }
  }

  def promptLogin = Action {
    Redirect(routes.Admins.login)
  }

  def updateAdmin = CSRFCheck {
    Action { implicit request =>
      val adminForm = editAdminForm.bindFromRequest()

      adminForm.fold(
        hasErrors = { form =>
          BadRequest(fillEditAdminForm)
            .flashing("error" -> "validation error")
        },
        success = { admin =>
          if (!admin.password.isEmpty) {
            Admin.update(admin.user, admin.password)
          }

          Ok(fillEditAdminForm)
            .flashing("success" -> "successfully updated") // cannot use flash without redirect
        }
      )
    }
  }

  def authenticate = CSRFCheck {
    Action { implicit request =>
      loginForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(views.html.login(formWithErrors, routes.Admins.authenticate)),
        admin => Ok(fillEditAdminForm)
      )
    }
  }

  private def fillEditAdminForm(implicit req: RequestHeader) =
    views.html.editAdmin(editAdminForm.fill(Admin.get))
}
