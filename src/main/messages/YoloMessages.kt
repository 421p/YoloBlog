package messages

import entities.Post
import java.util.*

class GetAllPosts()
class GetPost(val id: UUID)
class AddPost(val post: Post)
class UpdatePost(val post: Post)
class DeletePost(val id: UUID)


