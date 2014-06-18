package data

case class Nonce(value: String)
case class Session(id: String, expiration: Long)
case class Email(value: String)

object DB {

  import java.util.Date
  import java.util.UUID

  private var nonces: Map[Nonce,Email] = Map.empty
  private var sessions: Map[String,Session] = Map.empty
  private var emails: Map[Session,Email] = Map.empty

  def addNonce(e: Email): Nonce = {
    val n = Nonce(UUID.randomUUID.toString)
    nonces = nonces + (n -> e)
    n
  }

  def findNonce(n: Nonce): Option[Email] =
    for {
      e <- nonces.get(n)
      _  = nonces = nonces - n
    } yield e

  def addSession(e: Email): Session = {
    val thirtyDays = 30L * 24L * 60L * 60L * 1000L
    val id = UUID.randomUUID.toString
    val expiration = (new Date).getTime + thirtyDays
    val s = Session(id, expiration)
    sessions = sessions + (s.id -> s)
    emails = emails + (s -> e)
    s
  }

  def findSession(id: String): Option[Session] =
    for {
      s <- sessions.get(id)
      if s.expiration > (new Date).getTime
    } yield s

  def findEmail(s: Session): Option[Email] =
    emails.get(s)

}
