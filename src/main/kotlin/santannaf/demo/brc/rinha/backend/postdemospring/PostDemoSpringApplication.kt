package santannaf.demo.brc.rinha.backend.postdemospring

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.core.ParameterizedTypeReference
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient

@SpringBootApplication
class PostDemoSpringApplication

fun main(args: Array<String>) {
    runApplication<PostDemoSpringApplication>(*args)
}

@RestController
@RequestMapping(path = ["/posts"])
class PostsController(
    private val service: PostsService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun getPosts(): List<Post> {
        log.info("get all posts")
        val posts = service.getPosts()
        log.info("fetch posts successfully: ${posts.size}")
        return posts
    }
}

interface PostsService {
    fun getPosts(): List<Post>
}

@Service
class PostsServiceImpl(
    restClient: RestClient.Builder,
    val jdbcClient: JdbcClient,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) : PostsService {
    private val customHttp = restClient.baseUrl("https://jsonplaceholder.typicode.com").build()

    private val insertSql = """
        INSERT INTO POSTS (ID, TITLE, USER_ID, BODY)
        VALUES (:id, :title, :userId, :body)
    """.trimIndent()

    private fun insert(post: Post): Int {
        return jdbcClient.sql(insertSql)
            .param("id", post.id)
            .param("title", post.title)
            .param("userId", post.userId)
            .param("body", post.body)
            .update()
    }

    fun insertOneByOne(posts: List<Post>) {
        posts.forEach { post ->
            jdbcClient.sql(insertSql)
                .param("id", post.id)
                .param("title", post.title)
                .param("userId", post.userId)
                .param("body", post.body)
                .update()
        }
    }

    private fun insertBatch(posts: List<Post>): IntArray {
        if (posts.isEmpty()) return intArrayOf()
//        if (posts.isEmpty()) return 0

//        val paramList: List<Map<String, Any>> = posts.map { post ->
//            mapOf(
//                "id" to post.id,
//                "title" to post.title,
//                "userId" to post.userId,
//                "body" to post.body
//            )
//        }

        val batchValues: Array<Map<String, Any>> = posts.map { post ->
            mapOf(
                "id" to post.id,
                "title" to post.title,
                "userId" to post.userId,
                "body" to post.body
            )
        }.toTypedArray()

        return namedParameterJdbcTemplate.batchUpdate(insertSql, batchValues)

//        return jdbcClient.sql(insertSql)
//            .params(paramList)   // lista de maps para batch
//            .update()            // executa como batch
    }

    override fun getPosts(): List<Post> {
        val posts = customHttp.get()
            .uri("/posts")
            .retrieve()
            .body(object : ParameterizedTypeReference<List<Post>>() {}) ?: emptyList()

//        insertOneByOne(posts)
        insertBatch(posts)

        return posts
    }
}

data class Post(val id: Long, val title: String, val userId: Long, val body: String)
