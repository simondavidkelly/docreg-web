package vvv.docreg.backend

import com.hstx.docregsx._
import scala.actors._
import scala.actors.Actor._
import scala.collection.JavaConversions._
import vvv.docreg.model._

import _root_.net.liftweb.mapper._
import _root_.net.liftweb.util._
import _root_.net.liftweb.common._

case class Connect()

class Backend extends Actor {
  var agent: Agent = _
  def act() {
    loop {
      react {
        case Connect() => 
          val server = "shelob.GNET.global.vpn"
          agent = new Agent("0.1", server, "docreg-web")
          val library = new FileList(server, agent)
          library.addUpdateListener(new UpdateListener() {
            def updated(ds: java.util.List[Doc]) = ds.foreach(Backend.this ! _)
            def updated(d: Doc) = Backend.this ! d 
          })
        case d: Doc => 
          val document = Document.forName(d.getKey)
          if (document == null) {
            createDocument(d)
          } else {
            updateDocument(document, d)
          }
      }
    }
  }
  
  private def projectWithName(name: String) = {
    val existing = Project.forName(name) 
    if (existing == null) {
      val project = Project.create
      project.name(name)
      project.save
      project
      // TODO notify new project? Or do it as notification on save.
    } else {
      existing
    }
  }

  private def createDocument(d: Doc) {
    try {
      val document = Document.create
      assignDocument(document, d)
      document.save

      agent.loadRevisions(d).foreach{createRevision(document, _)}

      DocumentServer ! DocumentAdded(document)
    } catch {
      case e: java.lang.NullPointerException => println("Exception " + e + " with " + d.getKey)
    }
  }

  private def createRevision(document: Document, r: Rev) {
    val revision = Revision.create
    revision.document(document)
    assignRevision(revision, r)
    revision.save
  }
  
  private def updateDocument(document: Document, d: Doc) {
    // todo check project from latest revision !!!!!
    //println(document.name + " check latest " + document.latest + " against " + d.getVersion.toLong)
    if (document.latest_?(d.getVersion.toLong)) {
      //println(document.name + " document update, only needs reconcile")
      Reconciler ! PriorityReconcile(document)
    } else {
      println(document.name + " new revisions detected")
      updateRevisions(document)
    }
    
    assignDocument(document, d)
    if (document.dirty_?) { 
      document.save
      DocumentServer ! DocumentChanged(document)
    }
  }

  private def assignDocument(document: Document, d: Doc) {
    document.name(d.getKey)
    document.project(projectWithName(d.getProject))
    document.title(d.getTitle)
    document.editor(d.getEditor)
  }

  private def assignRevision(revision: Revision, r: Rev) {
    revision.version(r.getVersion)
    revision.filename(r.getFilename)
    revision.author(r.getAuthor)
    revision.date(r.getDate)
    revision.comment(r.getComment)
  }

  private def updateRevisions(document: Document) {
    agent.loadRevisions(document.name).foreach { r =>
      val revision = document.revision(r.getVersion)
      if (revision == null) {
        createRevision(document, r)
        DocumentServer ! DocumentRevised(document)
      } else {
        assignRevision(revision, r)
        if (revision.dirty_?) {
          revision.save
          DocumentServer ! DocumentChanged(document)
        }
      }
    }
  }
}
