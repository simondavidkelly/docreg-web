package vvv.docreg.backend

import akka.actor.Actor
import akka.util.Duration
import vvv.docreg.util.Environment
import net.liftweb.common.{Empty, Full}
import vvv.docreg.model.{ProjectAuthorization, Project, User, UserLookup}
import org.squeryl.PrimitiveTypeMode._

class UserStorage extends Actor {
  override def preStart() {
    context.system.scheduler.schedule(Duration("10 sec"), Duration("24 hour"), self, 'Check)
    super.preStart()
  }

  protected def receive = {
    case 'Check => {
      val directory = Environment.env.directory

      directory.groupMembers(DirectoryConfig.userGroup()) match {
        case Full(dns) => {
          for (dn <- dns; attributes <- directory.findAttributes(dn)) {
            // Cheap and nasty lookup that should set things right for most users.
            transaction( UserLookup.fromAttributes(attributes, true) )
          }
        }
        case _ => {
          // Ignore
        }
      }

//      for (p <- Project.findAll()) {
//        directory.groupMembers(DirectoryConfig.projectAuthorizationGroup(p.name)) match {
//          case Full(dns) => {
//            val authorized = ProjectAuthorization.authorizedUsersFor(p.id)
//            val users = for {
//              dn <- dns
//              attributes <- directory.findAttributes(dn)
//              user <- transaction( UserLookup.fromAttributes(attributes, true) )
//            } yield user
//            println("Authorized for " + p.name + " = " + users.map(_.displayName))
//          }
//          case Empty => {
//            // Remove all authorizations
//          }
//          case _ => {
//            // Ignore
//          }
//        }
//      }

    }
    case other => {
      unhandled(other)
    }
  }
}