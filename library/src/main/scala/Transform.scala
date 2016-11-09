package sbt.datatype

import ast._
import AstUtil._

object Transform {
  val emptyVersion = VersionNumber.empty
  def propateNamespace(doc: Document): Document =
    {
      val pkg =
        doc.packageDecl map { case PackageDecl(nameSegments, _, _, _) =>
          nameSegments.mkString(".")
        }
      val target =
        doc.packageDecl flatMap { case PackageDecl(_, dirs, _, _) =>
          toTarget(dirs)
        }
      val defns =
        doc.definitions map {
          toDefinitions(_, pkg, target)
        }
      doc.copy(definitions = defns)
    }

  def toDefinitions(d: Definition, ns0: Option[String], packageTarget: Option[String]): TypeDefinition =
    d match {
      case e: EnumTypeDefinition =>
        e.copy(namespace = e.namespace orElse ns0)
      case o: ObjectTypeDefinition =>
        o.copy(namespace = o.namespace orElse ns0)
      case i:InterfaceTypeDefinition =>
        i.copy(namespace = i.namespace orElse ns0)
    }
}
