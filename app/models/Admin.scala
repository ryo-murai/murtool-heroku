package models

import scalikejdbc._
import skinny.orm._

case class Admin(id: Long, user: String, password: String)

object Admin extends SkinnyCRUDMapper[Admin] {
  override val defaultAlias = createAlias("a")

  override def extract(rs: WrappedResultSet, n: ResultName[Admin]): Admin = Admin(
    id = rs.get(n.id),
    user = rs.get(n.user),
    password = rs.get(n.password))

  def get: Admin = findById(fixedId).getOrElse(defaultAdmin)

  def update(user: String, password: String) = {
    val res = updateById(fixedId).withAttributes('user -> user, 'password -> password)
    if (res == 0) createWithAttributes('id -> fixedId, 'user -> user, 'password -> password)
  }

  def authenticate(user: String, password: String): Option[Admin] = get match {
    case a @ Admin(_, u, pw) if u == user && pw == password => Some(a) // todo: hide plaintext pw
    case _ => None
  }

  private val fixedId = 0L
  private val defaultAdmin = Admin(fixedId, "admin", "admin")
}
