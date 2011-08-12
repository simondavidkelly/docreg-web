package vvv.docreg.model

import org.specs._
import org.specs.runner.JUnit4
import org.specs.runner.ConsoleRunner
import org.specs.matcher._
import org.specs.specification._
import java.util.Date
import java.io.File
import com.sun.corba.se.spi.orbutil.fsm.Input
import vvv.docreg.db.TestDbVendor

class DocumentTestSpecsAsTest extends JUnit4(DocumentTestSpecs)
object DocumentTestSpecsRunner extends ConsoleRunner(DocumentTestSpecs)

object DocumentTestSpecs extends Specification {
  "Document Model" should {
    "next version is 1 if no revisions" >> {
      TestDbVendor.initAndClean()
      val p = Project.create.name("p")
      p.save
      val d = Document.create.key("999").project(p).title("hellow world").editor("me").access("all")
      d.save

      d.nextVersion must be_==(1)
    }
    "create next version file name" in {
      TestDbVendor.initAndClean()
      val (u1, u2) = TestDbVendor.createUsers
      val p = Project.create.name("Cthulhu")
      p.save
      val d: Document = Document.create.key("234").project(p).title("The Nameless City").editor("H P Lovecraft").access("Forbidden")
      d.save
      val r4 = Revision.create.document(d).version(4).filename("foo.txt").author(u2).date(new Date()).comment("hmmm")
      r4.save

      d.nextFileName("youyoui.odt") must be_==("0234-005-The Nameless City.odt")
    }
  }
}