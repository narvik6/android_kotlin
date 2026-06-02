package com.example.module4_t3to14.task4

import com.example.module4_t3to14.R

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SocialFeedScreen()
            }
        }
    }
}

data class SocialPost(
    val id: Int,
    val userId: Int,
    val title: String,
    val body: String,
    val avatarUrl: String
)

data class Comment(
    val postId: Int,
    val id: Int,
    val name: String,
    val body: String
)

data class PostWithDetails(
    val post: SocialPost,
    val comments: List<Comment> = emptyList(),
    val isLoadingAvatar: Boolean = false,
    val isLoadingComments: Boolean = false,
    val isErrorAvatar: Boolean = false,
    val isErrorComments: Boolean = false
)

@Composable
fun SocialFeedScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var postsWithDetails by remember { mutableStateOf<List<PostWithDetails>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        loadJob = coroutineScope.launch {
            loadPosts(context) { posts ->
                postsWithDetails = posts
                isLoading = false
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Социальная лента",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = {
                    loadJob?.cancel()
                    isLoading = true
                    postsWithDetails = emptyList()

                    loadJob = coroutineScope.launch {
                        loadPosts(context) { posts ->
                            postsWithDetails = posts
                            isLoading = false
                        }
                    }
                }
            ) {
                Text("Обновить")
            }
        }

        if (isLoading && postsWithDetails.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(postsWithDetails) { postWithDetails ->
                    PostCard(postWithDetails = postWithDetails)
                }
            }
        }
    }
}

@Composable
fun PostCard(postWithDetails: PostWithDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    postWithDetails.isLoadingAvatar -> {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    postWithDetails.isErrorAvatar -> {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("❌", fontSize = 20.sp)
                        }
                    }
                    else -> {
                        AsyncImage(
                            model = postWithDetails.post.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Пользователь ${postWithDetails.post.userId}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = postWithDetails.post.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = postWithDetails.post.body,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Комментарии:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                postWithDetails.isLoadingComments -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Загрузка комментариев...", fontSize = 12.sp)
                    }
                }
                postWithDetails.isErrorComments -> {
                    Text(
                        text = "❌ Ошибка загрузки комментариев",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                postWithDetails.comments.isEmpty() -> {
                    Text(
                        text = "Нет комментариев",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                else -> {
                    postWithDetails.comments.take(3).forEach { comment ->
                        Text(
                            text = "• ${comment.name}: ${comment.body}",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    if (postWithDetails.comments.size > 3) {
                        Text(
                            text = "... и еще ${postWithDetails.comments.size - 3}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

suspend fun loadPosts(
    context: Context,
    onPostsLoaded: (List<PostWithDetails>) -> Unit
) {
    coroutineScope {
        val posts = loadPostsFromJson(context)

        val initialPosts = posts.map { post ->
            PostWithDetails(
                post = post,
                isLoadingAvatar = true,
                isLoadingComments = true
            )
        }
        onPostsLoaded(initialPosts)

        val jobs = initialPosts.mapIndexed { index, postWithDetails ->
            async {
                var avatarError = false
                try {
                    delay(1000 + Random.nextLong(500))
                    if (Random.nextInt(100) < 20) throw IOException("Ошибка аватарки")
                } catch (e: Exception) {
                    avatarError = true
                }

                var comments: List<Comment> = emptyList()
                var commentsError = false
                try {
                    comments = loadCommentsForPost(context, postWithDetails.post.id)
                } catch (e: Exception) {
                    commentsError = true
                }

                // Обновленный пост
                postWithDetails.copy(
                    isLoadingAvatar = false,
                    isErrorAvatar = avatarError,
                    comments = comments,
                    isLoadingComments = false,
                    isErrorComments = commentsError
                )
            }
        }

        // Ждем все результаты и отправляем финальный список
        val results = jobs.awaitAll()
        onPostsLoaded(results)
    }
}

suspend fun loadPostsFromJson(context: Context): List<SocialPost> = withContext(Dispatchers.IO) {
    val jsonString = context.resources.openRawResource(R.raw.social_posts)
        .bufferedReader().use { it.readText() }

    val jsonArray = JSONArray(jsonString)
    val posts = mutableListOf<SocialPost>()

    for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        val post = SocialPost(
            id = obj.getInt("id"),
            userId = obj.getInt("userId"),
            title = obj.getString("title"),
            body = obj.getString("body"),
            avatarUrl = obj.getString("avatarUrl")
        )
        posts.add(post)
    }

    posts
}

suspend fun loadCommentsForPost(context: Context, postId: Int): List<Comment> = withContext(Dispatchers.IO) {
    delay(1500 + Random.nextLong(500)) //

    val jsonString = context.resources.openRawResource(R.raw.comments)
        .bufferedReader().use { it.readText() }

    val jsonArray = JSONArray(jsonString)
    val allComments = mutableListOf<Comment>()

    for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        val comment = Comment(
            postId = obj.getInt("postId"),
            id = obj.getInt("id"),
            name = obj.getString("name"),
            body = obj.getString("body")
        )
        allComments.add(comment)
    }

    // 20% шанс ошибки
    if (Random.nextInt(100) < 20) {
        throw IOException("Ошибка загрузки комментариев")
    }

    allComments.filter { it.postId == postId }
}
