package vvv.docreg.snippet

import _root_.scala.xml._
import _root_.net.liftweb._
import http._
import S._
import SHtml._
import common._
import util._
import Helpers._
import js._
import JsCmds._
import _root_.net.liftweb.http.js.jquery.JqJsCmds._
import _root_.net.liftweb.http.js.JE.JsRaw
import vvv.docreg.model.Document

class Search extends Logger {
  private val resultPart: NodeSeq = 
    <tr>
      <td class="nowrap"><doc:project/></td>
      <td class="nowrap"><doc:key_link/></td>
      <td><doc:title/></td>
      <td class="nowrap"><doc:author/></td>
      <td class="nowrap"><doc:date/></td>
    </tr>
  object search extends RequestVar("")
  def go(xhtml: NodeSeq): NodeSeq = {
    bind("search", xhtml, 
      "form" -> form _,
      "results" -> <div/> % ("id" -> "results"))
  }
  def form(xhtml: NodeSeq): NodeSeq = {
    ajaxForm (
      bind("search", xhtml,
      "text" -> text(search.is, s => search(s)) ,
      "submit" -> submit("Search", processSearch _)) ++ hidden(processSearch _)
    )
  }
  def processSearch(): JsCmd = {
    debug("Search for '" + search.is + "'")
    if (search.is.size == 0) {
      Hide("results", 0) & Show("dashboard", 0)
    } else {
      val ds = Document.search(search.is) 
      val x = ds.flatMap(d => bind("doc", resultPart,
        "project" -> d.projectName,
        "author" -> d.latest.author,
        "key_link" -> <a href={d.latest.link}>{d.key}</a>,
        "date" -> d.latest.date,
        "title" -> <a href={d.infoLink}>{d.title}</a>))

      val out = 
        <table>
          <tr>
            <th>Project</th>
            <th>Doc</th>
            <th>Title</th>
            <th>Author</th>
            <th>Date</th>
          </tr>
          {x}
        </table>

      //debug("Resulted in " + out)
      SetHtml("results", out) & Show("results", 0) & Hide("dashboard", 0)
    }
  }
}
