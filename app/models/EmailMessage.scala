package models

case class EmailMessage(
  to: Option[String],
  cc: Option[String],
  bcc: Option[String],
  from: Option[String],
  replyTo: Option[String],
  subject: Option[String],
  body: Option[String])
