package io.kaitai.struct.exprlang

/**
  * Loosely based on /pythonparse/shared/src/main/scala/pythonparse/
  * from FastParse, Copyright (c) 2014 Li Haoyi (haoyi.sg@gmail.com)
  * http://www.lihaoyi.com/fastparse/
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
  * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
  * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
  * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
  * the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
  * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
  * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
  * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
  * IN THE SOFTWARE.
  */
object Ast {
  case class identifier(name: String)
  type bool = Boolean
  type int = Int

  // BoolOp() can use left & right?
  sealed trait expr
  object expr{
    case class BoolOp(op: boolop, values: Seq[expr]) extends expr
    case class BinOp(left: expr, op: operator, right: expr) extends expr
    case class UnaryOp(op: unaryop, operand: expr) extends expr
    case class Lambda(args: arguments, body: expr) extends expr
    case class IfExp(test: expr, body: expr, orelse: expr) extends expr
    case class Dict(keys: Seq[expr], values: Seq[expr]) extends expr
    case class Set(elts: Seq[expr]) extends expr
    case class Compare(left: expr, ops: cmpop, right: expr) extends expr
    case class Call(func: expr, args: Seq[expr]) extends expr
    case class Repr(value: expr) extends expr
    case class Num(n: Any) extends expr // a number as a PyObject.
    case class Str(s: String) extends expr

    // the following expression can appear in assignment context
    case class Attribute(value: expr, attr: identifier) extends expr
    case class Subscript(value: expr, idx: expr) extends expr
    case class Name(id: identifier) extends expr
    case class List(elts: Seq[expr], ctx: expr_context) extends expr
  }

  sealed trait expr_context
  object expr_context {
    case object Load extends expr_context
    case object Store extends expr_context
    case object Del extends expr_context
    case object AugLoad extends expr_context
    case object AugStore extends expr_context
    case object Param extends expr_context
  }

  sealed trait boolop
  object boolop{
    case object And extends boolop
    case object Or extends boolop
  }

  sealed trait operator
  case object operator {
    case object Add extends operator
    case object Sub  extends operator
    case object Mult  extends operator
    case object Div  extends operator
    case object Mod  extends operator
    case object Pow  extends operator
    case object LShift  extends operator
    case object RShift  extends operator
    case object BitOr  extends operator
    case object BitXor  extends operator
    case object BitAnd  extends operator
    case object FloorDiv extends operator
  }

  sealed trait unaryop
  object unaryop{

    case object Invert extends unaryop
    case object Not extends unaryop
    case object UAdd extends unaryop
    case object USubextends extends unaryop
  }
  sealed trait cmpop
  object cmpop{

    case object Eq extends cmpop
    case object NotEq extends cmpop
    case object Lt extends cmpop
    case object LtE extends cmpop
    case object Gt extends cmpop
    case object GtE extends cmpop
    case object Is extends cmpop
    case object IsNot extends cmpop
    case object In extends cmpop
    case object NotIn extends cmpop
  }

  case class comprehension(target: expr, iter: expr, ifs: Seq[expr])

  case class arguments(args: Seq[expr], vararg: Option[identifier], kwarg: Option[identifier], defaults: Seq[expr])

  // keyword arguments supplied to call
  case class keyword(arg: identifier, value: expr)

  // import name with optional 'as' alias.
  case class alias(name: identifier, asname: Option[identifier])
}