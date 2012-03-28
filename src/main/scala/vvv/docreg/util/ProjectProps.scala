package vvv.docreg.util

import net.liftweb._
import http._
import actor._
import common._
import mapper._
import util.Helpers._
import java.util.Properties
import scala.collection.JavaConversions._

object ProjectProps {
  def get(key: String): Box[String] = Box.legacyNullTest(props.getProperty(key))
  lazy val props: Properties = {
    val p = new Properties
    p.setProperty("project.name", "DocReg+Web")
    p.setProperty("project.version", "0.7.1")
    p
  }
}
