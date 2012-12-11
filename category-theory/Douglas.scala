// Semigroup

trait Semigroup[A] {
  def append(a: A): A
}

class ListSemigroup[A](as: List[A]) extends Semigroup[List[A]] {
  override def append(as2: List[A]) = as ++ as2
}

object Semigroup {
  implicit def listSemigroup[A](as: List[A]) = new ListSemigroup(as)
}

// Functor

trait Functor[A, F[_]] {
  def map[B](f: A => B): F[B]
}

object Functor {
  implicit def fn1Functor[A, B](g: A => B) = new Fn1Functor(g)
}

class Fn1Functor[A, B](g: A => B) extends Functor[B, ({type λ[α] = A => α})#λ] {
  override def map[C](f: B => C): (A => C) = a => f(g(a))
}

trait Fn1FunctorDemo {
  import Functor.fn1Functor
  val fn1FunctorDemo: Int => Int =
    { x: Int => x + 1 } map { x: Int => x * 2 } map { x: Int => x - 3 }
  println("fn1FunctorDemo(5) = " + fn1FunctorDemo(5)) // ((5 + 1) * 2) - 3 = 9
}

// Applicative Functor

trait Applicative[A, F[_]] extends Functor[A, F] {
  def ap[B](f: F[A => B]): F[B]
}

class Fn1Applicative[A, B](g: A => B) extends Fn1Functor[A, B](g)
                                         with Applicative[B, ({type λ[α] = A => α})#λ] {
  override def ap[C](f: A => B => C): (A => C) = a => f(a)(g(a))
}

class OptionApplicative[A](a: A) extends Applicative[A, Option] {
  override def map[B](f: A => B): Option[B] = Option(f(a))
  override def ap[B](f: Option[A => B]): Option[B] = f.map(_(a))
}

sealed trait Validity[A, B] extends Applicative[A, ({type λ[α] = Validity[α, B]})#λ]
case class Valid[A, B](a: A)(implicit bs: B => Semigroup[B]) extends Validity[A, B] {
  override def map[C](f: A => C): Validity[C, B] = Valid[C, B](f(a))
  override def ap[C](f: Validity[A => C, B]): Validity[C, B] = f match {
    case Valid(ac)  => Valid(ac(a))
    case Invalid(b) => Invalid(b)
  }
}
case class Invalid[A, B](b: B)(implicit bs: B => Semigroup[B]) extends Validity[A, B] {
  override def map[C](f: A => C): Validity[C, B] = Invalid(b)
  override def ap[C](f: Validity[A => C, B]): Validity[C, B] = f match {
    case Valid(ac)   => Invalid(b)
    case Invalid(b2) => Invalid(bs(b2) append b)
  }
}

object Applicative {
  implicit def fn1Applicative[A, B](g: A => B) = new Fn1Applicative(g)
  implicit def optionApplicative[A](a: A) = new OptionApplicative(a)
}

trait Fn1ApplicativeDemo {
  import Applicative.fn1Applicative
  val fn1ApplicativeDemo: Int => Int =
    { x: Int => x + 1 } ap { x: Int => y: Int => x * (y + 3) }
  println("fn1ApplicativeDemo(5) = " + fn1ApplicativeDemo(5))
}

trait OptionApplicativeDemo {
  import Applicative.optionApplicative
  val add3: Int => Int => Int => Int = x => y => z => x + y + z

  val optionApplicativeDemo1 = 1 ap (2 ap (3 map add3))
  println("optionApplicativeDemo1 = " + optionApplicativeDemo1)

  val nope: Int = null.asInstanceOf[Int]
  val optionApplicativeDemo2 = 1 ap (nope ap (3 map add3))
  println("optionApplicativeDemo2 = " + optionApplicativeDemo2)
}

trait ValidityApplicativeDemo {
  import Semigroup.listSemigroup
  val add4: Int => Int => Int => Int => Int = w => x => y => z => w + x + y + z

  def parse(x: String): Validity[Int, List[String]] = try {
    Valid[Int, List[String]](x.toInt)
  } catch {
    case _ => Invalid[Int, List[String]](List("'" + x + "' is not an integer"))
  }

  val validApplicativeDemo = parse("1") ap (parse("2") ap (parse("3") ap (parse("4") map add4)))
  println("validApplicativeDemo = " + validApplicativeDemo)

  val invalidApplicativeDemo = parse("1") ap (parse("nooo") ap (parse("3") ap (parse("fourve") map add4)))
  println("invalidApplicativeDemo = " + invalidApplicativeDemo)
}

// Monad

trait Monad[A, F[_]] extends Applicative[A, F] {
  def flatMap[B](f: A => F[B]): F[B]
}

class Fn1Monad[A, B](g: A => B) extends Fn1Applicative[A, B](g)
                                   with Monad[B, ({type λ[α] = A => α})#λ] {
  override def flatMap[C](f: B => (A => C)): (A => C) = a => f(g(a))(a)
}

class StateMonad[S, A](g: S => (A, S)) extends Monad[A, ({type λ[α] = S => (α, S)})#λ] {
  override def map[B](f: A => B): (S => (B, S)) =
    { state =>
      val (a, state1) = g(state)
      (f(a), state1)
    }
  def ap[B](f: S => (A => B, S)): (S => (B, S)) =
    { state =>
      val (a, state1) = g(state)
      val (atob, state2) = f(state1)
      (atob(a), state2)
    }
  override def flatMap[B](f: A => (S => (B, S))): (S => (B, S)) =
    { state =>
      val (a, state1) = g(state)
      f(a)(state1)
    }
}

object Monad {
  type Reader[A, B] = Fn1Monad[A, B]
  implicit def fn1Monad[A, B](g: A => B) = new Fn1Monad(g)
  implicit def stateMonad[S, A](g: S => (A, S)) = new StateMonad(g)
}

trait Fn1MonadDemo1 {
  import Monad.fn1Monad
  val fn1MonadDemo1: Int => Int =
    { x: Int => x + 1 } flatMap { x: Int => y: Int => x * (y + 3) }
  println("fn1MonadDemo1(5) = " + fn1MonadDemo1(5)) // (5 + 1) * (5 + 3) = 48
}

trait Fn1MonadDemo2 {
  import Monad.fn1Monad
  val fn1MonadDemo2: Int => Int =
    for {
      a <- { x: Int => x + 1 }
      b <- { x: Int => y: Int => x * (y + 3) } apply a
    } yield b
  println("fn1MonadDemo2(5) = " + fn1MonadDemo2(5)) // (5 + 1) * (5 + 3) = 48
}

trait StateMonadDemo1 {
  import Monad.stateMonad

  val f1: List[String] => (Int, List[String]) = log => (1, "f1" :: log)
  val f2: Int => List[String] => (Int, List[String]) = x => log => (x + 1, "f2" :: log)

  val f3: List[String] => (Int, List[String]) = for {
    a <- f1
    b <- f2(a)
  } yield b

  val (x, log) = f3(Nil)
  println("StateMonadDemo1: (x, log) = (" + x + ", " + log + ")")
}

trait StateMonadDemo2 {
  import Monad.stateMonad

  trait Log { def append(level: String, message: String): Log }
  class StdoutLog extends Log {
    def append(level: String, message: String) = {
      println("[" + level + "] " + message)
      this
    }
  }

  val f1: Log => (Int, Log) = log => (1, log.append("info", "f1"))
  val f2: Int => Log => (Int, Log) = x => log => (x + 1, log.append("info", "f2"))

  val f3: Log => (Int, Log) = for {
    a <- f1
    b <- f2(a)
  } yield b

  val (x, log) = f3(new StdoutLog)
  println("StateMonadDemo1: x = " + x)
}

// Demo

object Demo extends App
               with Fn1FunctorDemo
               with Fn1ApplicativeDemo
               with OptionApplicativeDemo
               with ValidityApplicativeDemo
               with Fn1MonadDemo1
               with Fn1MonadDemo2
               with StateMonadDemo2
