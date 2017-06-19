package actors

import akka.actor.Props
import akka.actor.UntypedActor
import com.arjuna.ats.jta.TransactionManager
import entities.Post
import messages.*
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.Persistence

class StorageActor : UntypedActor() {

    val tm: javax.transaction.TransactionManager = TransactionManager.transactionManager()
    val emf: EntityManagerFactory = Persistence.createEntityManagerFactory("yolo-mongo")

    override fun onReceive(message: Any?) {
        when (message) {
            is GetAllPosts -> transact {
                val result = it.createQuery("from Post").resultList
                respond(result)
            }

            is GetPost -> transact {
                val result = it.find(Post::class.java, message.id)
                respond(result)
            }

            is AddPost -> transact {
                message.post.date = Date()
                it.persist(message.post)
                it.flush()
                respond("ok")
            }

            is DeletePost -> transact {
                val post = it.find(Post::class.java, message.id)
                it.remove(post)
                it.flush()
                respond("ok")
            }

            is UpdatePost -> transact {
                val post = message.post
                it.merge(post)
                it.flush()
                respond("ok")
            }
        }
    }

    override fun postStop() {
        emf.close()
    }

    private fun respond(message: Any) = sender.tell(message, self)

    private fun transact(transaction: (EntityManager) -> Unit) {
        tm.begin()
        transaction(emf.createEntityManager())
        tm.commit()
    }

    companion object {
        fun props(): Props = Props.create(StorageActor::class.java)
    }
}