/*
 * Copyright (c) 2013 Scott Abernethy.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package vvv.docreg.model

import org.specs._
import java.util.Date
import vvv.docreg.db.TestDbVendor
import net.liftweb.http.js.JsCmds._Noop
import vvv.docreg.util.T
import org.squeryl.PrimitiveTypeMode._

class DocumentTest extends Specification {
  "Document Model" should {
    "next version is 1 if no revisions" >> {
      TestDbVendor.initAndClean()
      transaction{
      val (p,_,_) = TestDbVendor.createProjects
      val d = new Document
      d.number = ("336")
      d.projectId = (p.id)
      d.title = ("Foo bar")
      d.access = ("Everyone")
      Document.dbTable.insert(d)

      d.nextVersion must be_==(1)
      }
    }

    "create next version file name" in {
      TestDbVendor.initAndClean()
      transaction{
      val (u1, u2) = TestDbVendor.createUsers
      val (p,_,_) = TestDbVendor.createProjects
      val d = new Document
      d.number = ("234")
      d.projectId = (p.id)
      d.title = ("The Nameless City")
      d.access = ("Forbidden")
      Document.dbTable.insert(d)

      val r4 = new Revision
      r4.documentId = (d.id)
      r4.version = (4)
      r4.filename = ("foo.txt")
      r4.authorId = (u2.id)
      r4.date = (T.now)
      r4.comment = ("hmmm")
      Revision.dbTable.insert(r4)

      d.nextFileName("Rainbow Fish", "youyoui.odt") must be_==("0234-005-Rainbow Fish.odt")
      }
    }

    "check no file extension" in {
      TestDbVendor.initAndClean()
      transaction{
      val (u1, u2) = TestDbVendor.createUsers
      val (p,_,_) = TestDbVendor.createProjects
      val d = new Document
      d.number = ("234")
      d.projectId = (p.id)
      d.title = ("The Nameless City")
      d.access = ("Forbidden")
      Document.dbTable.insert(d)

      val r4 = new Revision
      r4.documentId = (d.id)
      r4.version = (4)
      r4.filename = ("foo.txt")
      r4.authorId = (u2.id)
      r4.date = (T.now)
      r4.comment = ("hmmm")
      Revision.dbTable.insert(r4)

      d.nextFileName("The Nameless City", "youyoui") must be_==("0234-005-The Nameless City")
      }
    }

    "check valid identifiers" >> {
      Document.ValidIdentifier.findFirstIn("") must beNone
      Document.ValidIdentifier.findFirstIn("index") must beNone
      Document.ValidIdentifier.findFirstIn("d/987") must beNone
      Document.ValidIdentifier.findFirstIn("user/1234") must beNone
      Document.ValidIdentifier.findFirstIn("user/1234/profile") must beNone

      def checkValidId(in: String, expectedKey: String, expectedVersion: String) = {
        in match {
          case Document.ValidIdentifier(key, version) => {
            key must be_==(expectedKey)
            if (expectedVersion == null) {
              version must beNull[String]
            }
            else {
              version must be_==(expectedVersion)
            }
          }
          case _ => {
            fail()
          }
        }
      }

      checkValidId("0", "0", null)
      checkValidId("1", "1", null)
      checkValidId("0002", "0002", null)
      checkValidId("987", "987", null)
      checkValidId("9999", "9999", null)
      checkValidId("12345", "12345", null)

      checkValidId("12-4", "12", "-4")
      checkValidId("9999-999", "9999", "-999")
    }

    "check valid document filename" >>
    {
      def checkValid(in: String, expectedKey: String, expectedVersion: String, expectedFileName: String)
      {
        in match {
          case Document.ValidDocumentFileName(key, version, fileName) =>
          {
            key must be_==(expectedKey)
            version must be_==(expectedVersion)
            expectedFileName must be_==(expectedFileName)
          }
          case _ =>
          {
            fail()
          }
        }
      }

      Document.ValidDocumentFileName.findFirstIn("6146-001") must beNone
      Document.ValidDocumentFileName.findFirstIn("6146-001-") must beNone
      Document.ValidDocumentFileName.findFirstIn("6146-New Document Test.txt") must beNone
      Document.ValidDocumentFileName.findFirstIn("New Document Test.txt") must beNone

      checkValid("6146-001-New Document Test 3.txt", "6146", "001", "New Document Test 3.txt")
    }

    "check valid identifier and ext" >>
    {
      def checkValid(in: String, expectedKey: String)
      {
        in match {
          case Document.IdentifierAndExtension(key) =>
          {
            key must be_==(expectedKey)
          }
          case _ =>
          {
            fail()
          }
        }
      }
      Document.IdentifierAndExtension.findFirstIn("6146.") must beNone
      Document.IdentifierAndExtension.findFirstIn(".txt") must beNone
      Document.IdentifierAndExtension.findFirstIn("my doco") must beNone
      Document.IdentifierAndExtension.findFirstIn("1234 txt") must beNone

      checkValid("987.foo", "987")
      checkValid("0987.foo", "0987")
      checkValid("1234.doc", "1234")
    }

    "validate user access" >>
    {
      TestDbVendor.initAndClean()
      inTransaction {
        val (p1, p2, p3) = TestDbVendor.createProjects
        val (u1, u2) = TestDbVendor.createUsers

        val x = new Document
        x.projectId = p3.id
        x.title = "Foo"
        x.number = "1444"
        x.access = "Public"
        x.allows(u1) must beTrue

        x.access = "Secure"
        x.allows(u1) must beFalse

        ProjectAuthorization.grant(u2, p3)
        x.allows(u1) must beFalse

        ProjectAuthorization.grant(u1, p3)
        x.allows(u1) must beTrue

        ProjectAuthorization.revoke(u1, p3)
        x.allows(u1) must beFalse
      }
    }
  }

  "DocumentRef extractor" should {
    import Document.DocumentRef

    "Fail to extract garbage" >> {
      DocumentRef.unapply("garbage") must beNone
    }

    "Extract 1234 or 0066 like" >> {
      DocumentRef.unapply("1234") must beSome(("1234", Long.MaxValue))
      DocumentRef.unapply("546") must beSome(("546", Long.MaxValue))
      DocumentRef.unapply("9") must beSome(("9", Long.MaxValue))
      DocumentRef.unapply("0066") must beSome(("0066", Long.MaxValue))
    }

    "Extract 2345-499 or 765-34 or 8-001 like" >> {
      DocumentRef.unapply("2345-499") must beSome(("2345", 499l))
      DocumentRef.unapply("765-34") must beSome(("765", 34l))
      DocumentRef.unapply("8-001") must beSome(("8", 1l))
      DocumentRef.unapply("7-2") must beSome(("7", 2l))
      DocumentRef.unapply("0066-002") must beSome(("0066", 2l))
    }

    "Extract 1234.txt or 1234-876.txt like" >> {
      DocumentRef.unapply("1234.txt") must beSome(("1234", Long.MaxValue))
      DocumentRef.unapply("1234-876.txt") must beSome(("1234", 876l))
    }

    "Extract 1234-My title.zip or 0666-666-Snap.jpg" >> {
      DocumentRef.unapply("1234-My title.zip") must beSome(("1234", Long.MaxValue))
      DocumentRef.unapply("0666-666-Snap.jpg") must beSome(("0666", 666l))
    }
  }

  "DocumentRevision extractor" should {
    import Document.DocumentRevision

    "Not extract garbage input" >> {
      DocumentRevision.unapply("garbage") must beNone
      DocumentRevision.unapply("123KKK-REV") must beNone
      DocumentRevision.unapply("X-Y") must beNone
      DocumentRevision.unapply("XXXX-YYY") must beNone
    }

    "Load correct document" >> {
      TestDbVendor.initAndClean()
      inTransaction{
        val (p1, p2, p3) = TestDbVendor.createProjects
        val (u1, u2) = TestDbVendor.createUsers

        val d = new Document
        d.number = ("0234")
        d.projectId = (p1.id)
        d.title = ("The Nameless City")
        d.access = ("Forbidden")
        Document.dbTable.insert(d)

        val r1 = new Revision
        r1.documentId = (d.id)
        r1.version = (1)
        r1.filename = ("foo.txt")
        r1.authorId = (u1.id)
        r1.date = (T.now)
        r1.comment = ("ok ok ok now")
        Revision.dbTable.insert(r1)

        val r4 = new Revision
        r4.documentId = (d.id)
        r4.version = (4)
        r4.filename = ("foo.txt")
        r4.authorId = (u2.id)
        r4.date = (T.now)
        r4.comment = ("hmmm")
        Revision.dbTable.insert(r4)

        DocumentRevision.unapply("0233") must beNone
        DocumentRevision.unapply("234") must beNone
        DocumentRevision.unapply("0234") must beSome((d, r4))
        DocumentRevision.unapply("0234-004") must beSome((d, r4))
        DocumentRevision.unapply("0234-4") must beSome((d, r4))
        DocumentRevision.unapply("0234-001") must beSome((d, r1))
        DocumentRevision.unapply("0234-001-Some garbage") must beSome((d, r1))
        DocumentRevision.unapply("0234-001-Some garbage.txt") must beSome((d, r1))
        DocumentRevision.unapply("0234.txt") must beSome((d, r4))
        DocumentRevision.unapply("0234txt") must beNone
        DocumentRevision.unapply("0234-1") must beSome((d, r1))
        DocumentRevision.unapply("0234-009") must beNone
        DocumentRevision.unapply("0234-9") must beNone
        DocumentRevision.unapply("0234-XYZ") must beSome(d, r4)
        DocumentRevision.unapply("0234-XYZ.jpg") must beSome(d, r4)
        DocumentRevision.unapply("0234-") must beNone
      }
    }
  }
}