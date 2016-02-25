package io.kaitai.struct.languages

import io.kaitai.struct.Utils
import io.kaitai.struct.format.{ProcessXor, ProcessExpr, AttrSpec}

class JavaScriptCompiler(verbose: Boolean, outDir: String) extends LanguageCompiler(verbose, outDir) with UpperCamelCaseClasses with EveryReadIsExpression {
  override def outFileName(topClassName: String): String = s"${type2class(topClassName)}.js"
  override def indent: String = "  "

  override def fileHeader(sourceFileName: String, topClassName: String): Unit = {
    out.puts(s"// This file was generated from '${sourceFileName}' with kaitai-struct compiler")
    out.puts
  }

  override def fileFooter(name: String): Unit = {
    out.puts
    out.puts("// Export for amd environments")
    out.puts("if (typeof define === 'function' && define.amd) {")
    out.inc
    out.puts(s"define('${type2class(name)}', [], function() {")
    out.inc
    out.puts(s"return ${type2class(name)};")
    out.dec
    out.puts("});")
    out.dec
    out.puts("}")

    out.puts

    out.puts("// Export for CommonJS")
    out.puts("if (typeof module === 'object' && module && module.exports) {")
    out.inc
    out.puts(s"module.exports = ${type2class(name)};")
    out.dec
    out.puts("}")
  }

  override def classHeader(name: String): Unit = {
    out.puts(s"${type2class(name)} = (function() {")
    out.inc
  }

  override def classFooter(name: String): Unit = {
    out.puts
    out.puts(s"return ${type2class(name)};")
    out.dec
    out.puts("})();")
  }

  override def classConstructorHeader(name: String): Unit = {
    out.puts(s"function ${type2class(name)}(_io, _parent) {")
    out.inc
    out.puts("if (_parent == null)")
    out.inc
    out.puts("_parent = null;")
    out.dec
    out.puts("this._io = _io;")
    out.puts("this._parent = _parent;")
    out.puts
  }

  override def classConstructorFooter: Unit = {
    out.dec
    out.puts("}")
  }

  override def attributeDeclaration(attrName: String, attrType: String, isArray: Boolean): Unit = {}

  override def attributeReader(attrName: String, attrType: String, isArray: Boolean): Unit = {}

  override def attrFixedContentsParse(attrName: String, contents: Array[Byte]): Unit = {
    out.puts(s"this.${lowerCamelCase(attrName)} = _io.ensureFixedContents(${contents.length}, new byte[] { ${contents.mkString(", ")} });")
  }

  override def attrNoTypeWithSize(varName: String, size: String): Unit = {
    out.puts(s"this.${lowerCamelCase(varName)} = _io.readBytes(${expression2Java(size)});")
  }

  override def attrNoTypeWithSizeEos(varName: String): Unit = {
    out.puts(s"this.${lowerCamelCase(varName)} = _io.readBytesFull();")
  }

  override def attrUserTypeParse(id: String, attr: AttrSpec, io: String): Unit = {
    handleAssignment(id, attr, s"new ${type2class(attr.dataType)}(${io}, this)", io)
  }

  override def attrProcess(proc: ProcessExpr, varSrc: String, varDest: String): Unit = {
    proc match {
      case ProcessXor(xorValue) =>
        out.puts(s"this.$varDest = new byte[this.$varSrc.length];")
        out.puts(s"for (int i = 0; i < this.$varSrc.length; i++) {")
        out.inc
        out.puts(s"this.$varDest[i] = (byte) (this.$varSrc[i] ^ (${expression2Java(xorValue)}));")
        out.dec
        out.puts("}")
    }
  }

  override def normalIO: String = "_io"

  override def allocateIO(varName: String): String = {
    val ioName = s"_io_${lowerCamelCase(varName)}"
    out.puts(s"KaitaiStream ${ioName} = new KaitaiStream(${lowerCamelCase(varName)});")
    ioName
  }

  override def seek(io: String, pos: String): Unit = {
    out.puts(s"${io}.seek(${expression2Java(pos)});")
  }

  override def handleAssignment(id: String, attr: AttrSpec, expr: String, io: String): Unit = {
    if (attr.ifExpr.isDefined) {
      out.puts(s"if (${attr.ifExpr.get}) {")
      out.inc
    }

    attr.repeat match {
      case Some("eos") =>
        out.puts(s"${id} = [];")
        out.puts(s"while (!${io}.isEof()) {")
        out.inc
        out.puts(s"${id}.add(${expr});")
        out.dec
        out.puts("}")
      case Some("expr") =>
        attr.repeatExpr match {
          case Some(repeatExpr) =>
            out.puts(s"${id} = new Array(${expression2Java(repeatExpr)});")
            out.puts(s"for (int i = 0; i < ${expression2Java(repeatExpr)}; i++) {")
            out.inc
            out.puts(s"${id}.add(${expr});")
            out.dec
            out.puts("}")
          case None =>
            throw new RuntimeException("repeat: expr, but no repeat-expr value given")
        }
      case None =>
        out.puts(s"this.${lowerCamelCase(id)} = ${expr};")
    }

    if (attr.ifExpr.isDefined) {
      out.dec
      out.puts("}")
    }
  }

  override def stdTypeParseExpr(attr: AttrSpec, endian: Option[String]): String = {
    attr.dataType match {
      case "u1" =>
        s"_io.readUint8()"
      case "s1" | "u2le" | "u2be" | "u4le" | "u4be" | "u8le" | "u8be" | "s2le" | "s2be" | "s4le" | "s4be" | "s8le" | "s8be" =>
        s"_io.read${Utils.capitalize(attr.dataType)}()"
      case "u2" | "u4" | "u8" | "s2" | "s4" | "s8" =>
        endian match {
          case Some(e) => s"_io.read${Utils.capitalize(attr.dataType)}${e}()"
          case None => throw new RuntimeException(s"type ${attr.dataType}: unable to parse with no default endianess defined")
        }
      case null => throw new RuntimeException("should never happen")

      // Aw, crap, can't use interpolated strings here: https://issues.scala-lang.org/browse/SI-6476
      case "str" =>
        ((attr.byteSize, attr.sizeEos)) match {
          case (Some(bs: String), false) =>
            s"_io.readStrByteLimit(${bs}, " + '"' + attr.encoding.get + "\")"
          case (None, true) =>
            "_io.readStrEos(\"" + attr.encoding.get + "\")"
          case (None, false) =>
            throw new RuntimeException("type str: either \"byte_size\" or \"size_eos\" must be specified")
          case (Some(_), true) =>
            throw new RuntimeException("type str: only one of \"byte_size\" or \"size_eos\" must be specified")
        }
      case "strz" =>
        "_io.readStrz(\"" + attr.encoding.get + '"' + s", ${attr.terminator}, ${attr.include}, ${attr.consume}, ${attr.eosError})"
    }
  }

  override def instanceHeader(className: String, instName: String, dataType: String, isArray: Boolean): Unit = {
    out.puts(s"${type2class(className)}.prototype.someInstance = function() {")
    out.inc
  }

  override def instanceAttrName(instName: String): String = instName

  override def instanceFooter: Unit = classConstructorFooter

  override def instanceCheckCacheAndReturn(instName: String): Unit = {
    out.puts(s"if (${lowerCamelCase(instName)} != null)")
    out.inc
    instanceReturn(instName)
    out.dec
  }

  override def instanceReturn(instName: String): Unit = {
    out.puts(s"return ${lowerCamelCase(instName)};")
  }

  def lowerCamelCase(s: String): String = {
    if (s.startsWith("_raw_")) {
      return "_raw_" + Utils.lowerCamelCase(s.substring("_raw_".length))
    } else {
      Utils.lowerCamelCase(s)
    }
  }

  val ReInt = "^\\d+$".r
  val ReHexInt = "^0x[0-9a-fA-F]+$".r
  val ReLiteral = "^[A-Za-z][A-Za-z0-9_]*$".r

  def expression2Java(s: String): String = {
    s match {
      case ReInt() | ReHexInt() => s
      case ReLiteral() => lowerCamelCase(s)
    }
  }
}