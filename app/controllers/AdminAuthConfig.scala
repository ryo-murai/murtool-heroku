package controllers

import scala.concurrent._
import scala.reflect._
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import jp.t2v.lab.play2.auth._
import models.Admin

trait AdminAuthConfig extends AuthConfig {
  type Id = String
  type User = Admin
  type Authority = Admin

  val idTag: ClassTag[Id] = classTag[Id]

  def sessionTimeoutInSeconds = 3600

  def resolveUser(id: Id)(implicit context: ExecutionContext) = Future { Some(Admin.get) }

  def loginSucceeded(request: RequestHeader)(implicit context: ExecutionContext) =
    Future.successful(Redirect(routes.Application.index))

  def logoutSucceeded(request: RequestHeader)(implicit context: ExecutionContext) =
    Future.successful(Redirect(routes.Sessions.login))

  def authenticationFailed(request: RequestHeader)(implicit context: ExecutionContext) =
    Future.successful(Redirect(routes.Sessions.login))

  def authorizationFailed(request: RequestHeader)(implicit context: ExecutionContext) =
    Future.successful(Forbidden("no permission"))

  def authorize(user: User, authority: Authority)(implicit context: ExecutionContext) =
    Future.successful(true)
}
