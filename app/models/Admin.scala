package models

import org.mindrot.jbcrypt.BCrypt
import scalikejdbc._
import skinny.orm._

case class Admin(id: Long, user: String, password: String) {
  def authenticate(user: String, password: String): Option[Admin] = Admin.get match {
    case a @ Admin(_, u, pw) if u == user && BCrypt.checkpw(password, pw) => Some(this)
    case _ => None
  }
}

object DefaultAdmin extends Admin(Admin.fixedId, "admin", "") {
  override def authenticate(user: String, pw: String): Option[Admin] = (user, pw) match {
    case ("admin", "admin") => Some(this)
    case _ => None
  }
}

object Admin extends SkinnyCRUDMapper[Admin] {
  val fixedId = 0L

  override def useAutoIncrementPrimaryKey = false
  override val defaultAlias = createAlias("a")

  override def extract(rs: WrappedResultSet, n: ResultName[Admin]): Admin = Admin(
    id = rs.get(n.id),
    user = rs.get(n.user),
    password = rs.get(n.password))

  def get: Admin = findById(fixedId).getOrElse(DefaultAdmin)

  def update(user: String, password: String) = {
    val hspass = BCrypt.hashpw(password, BCrypt.gensalt())
    val res = updateById(fixedId).withAttributes('user -> user, 'password -> hspass)
    if (res == 0) createWithAttributes('id -> fixedId, 'user -> user, 'password -> hspass)
  }
}
