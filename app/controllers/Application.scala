package controllers

import play.api._
import play.api.mvc._
import models._

object Application extends Controller {
  def index = Action {
    val admin = Admin.get
    Ok(s"hello ${admin.username}")
  }
}
