package com.example.news.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.news.model.Article
import com.example.news.repository.ArticleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log
import com.example.news.BuildConfig

/**
 * Simple ViewModel that exposes saved articles and remote headlines.
 * It uses the ArticleRepository for local CRUD and a tiny HTTP fetch for headlines.
 */
class NewsViewModel(private val repo: ArticleRepository) : ViewModel() {

    private val _savedArticles = MutableLiveData<List<Article>>(emptyList())
    val savedArticles: LiveData<List<Article>> = _savedArticles

    private val _headlines = MutableLiveData<List<Article>>(emptyList())
    val headlines: LiveData<List<Article>> = _headlines

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun loadSavedArticles() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repo.getAll()
            _savedArticles.postValue(list)
        }
    }

    fun insertArticle(article: Article) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repo.insert(article)
            // reload saved list
            loadSavedArticles()
            // if this article came from headlines (matched by URL), update headlines to reflect saved id
            val current = _headlines.value ?: emptyList()
            if (!article.url.isNullOrBlank()) {
                val updated = current.map { h ->
                    if (h.url == article.url && h.id == null) {
                        h.copy(id = id)
                    } else h
                }
                _headlines.postValue(updated)
            }
        }
    }

    /**
     * Insert and return the new row id via callback on the Main thread.
     * Useful when a fragment needs the id immediately (e.g. save-then-edit flow).
     */
    fun insertArticleWithCallback(article: Article, callback: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repo.insert(article)
            // reload saved list
            loadSavedArticles()
            // update headlines if matching by URL
            val current = _headlines.value ?: emptyList()
            if (!article.url.isNullOrBlank()) {
                val updated = current.map { h ->
                    if (h.url == article.url && h.id == null) {
                        h.copy(id = id)
                    } else h
                }
                _headlines.postValue(updated)
            }
            // invoke callback on main thread
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    callback(id)
                }
            } catch (e: Exception) {
                callback(id)
            }
        }
    }

    fun updateArticle(article: Article) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.update(article)
            // reload saved list
            loadSavedArticles()
            // update headlines entries matching this article's URL so edits reflect in remote list too
            val current = _headlines.value ?: emptyList()
            if (!article.url.isNullOrBlank()) {
                val updated = current.map { h ->
                    if (h.url == article.url) {
                        // keep thumbnail from headline if present, but prefer updated title/desc
                        h.copy(id = article.id ?: h.id, title = article.title, description = article.description, publishedAt = article.publishedAt)
                    } else h
                }
                _headlines.postValue(updated)
            }
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            // get the article being deleted (to match by URL in headlines)
            val toDelete = repo.getById(id)
            repo.delete(id)
            // reload saved list
            loadSavedArticles()
            // clear saved state from headlines that matched the deleted article's URL
            if (toDelete != null && !toDelete.url.isNullOrBlank()) {
                val current = _headlines.value ?: emptyList()
                val updated = current.map { h -> if (h.url == toDelete.url) h.copy(id = null) else h }
                _headlines.postValue(updated)
            }
        }
    }

    /**
     * Fetch headlines from NewsAPI.org. To use the official API, set a key and update the URL.
     * For this simple example we'll call the sample endpoint that may not need a key.
     */
    fun fetchTopHeadlines(callback: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // The user should add their API key in strings.xml or build configs; for now try a public sample endpoint.
                // Read API key from BuildConfig (set via local.properties -> BuildConfig.NEWS_API_KEY).
                // If you prefer the key directly in the URL, the following fallback will use a hard-coded key.
                // WARNING: hard-coding keys in source is insecure for production. This is provided per your request.
                val apiKey = if (BuildConfig.NEWS_API_KEY.isNotBlank()) BuildConfig.NEWS_API_KEY
                else "30dd8a4defc844bea6e6bd57ad33ed25"
                // Log masked API key info for debugging (do not log full key)
                if (apiKey.isBlank()) {
                    Log.w("NewsViewModel", "No API key provided in BuildConfig.NEWS_API_KEY")
                } else {
                    val masked = if (apiKey.length > 8) apiKey.substring(0,4) + "..." + apiKey.takeLast(4) else "(set)"
                    Log.d("NewsViewModel", "Using API key: $masked")
                }
                // Construct URL with apiKey as query param (NewsAPI expects apiKey or X-Api-Key header).
                val urlString = "https://newsapi.org/v2/top-headlines?country=us&apiKey=$apiKey"
                val url = URL(urlString)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    // Set common headers which may help bypass simple blocks (User-Agent) and request JSON
                    setRequestProperty("User-Agent", "NewsApp/1.0")
                    setRequestProperty("Accept", "application/json")
                    // Prefer the X-Api-Key header over query param for clarity
                    if (apiKey.isNotBlank()) setRequestProperty("X-Api-Key", apiKey)
                }
                conn.connect()
                val code = conn.responseCode
                if (code == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = mutableListOf<Article>()
                    val root = JSONObject(text)
                    val articles = root.optJSONArray("articles")
                    if (articles != null) {
                        for (i in 0 until articles.length()) {
                            val o = articles.getJSONObject(i)
                            val title = o.optString("title", "(no title)")
                            val desc = o.optString("description", "")
                            val urlStr = o.optString("url", "")
                            val publishedAt = o.optString("publishedAt", "")
                            var imageUrl: String? = null
                            if (!o.isNull("urlToImage")) {
                                val raw = o.optString("urlToImage", null)
                                if (!raw.isNullOrBlank() && (raw.startsWith("http://") || raw.startsWith("https://"))) {
                                    imageUrl = raw
                                }
                            }
                            arr.add(Article(null, title, desc, urlStr, publishedAt, imageUrl))
                        }
                    }
                    // Merge with local saved articles so edits/bookmarks survive subsequent fetches
                    val merged = arr.map { remoteA ->
                        if (!remoteA.url.isNullOrBlank()) {
                            val saved = try { repo.getByUrl(remoteA.url!!) } catch (e: Exception) { null }
                            if (saved != null) {
                                // prefer locally saved title/description/publishedAt, but keep remote image if saved lacks one
                                saved.copy(imageUrl = saved.imageUrl ?: remoteA.imageUrl)
                            } else remoteA
                        } else remoteA
                    }
                    _headlines.postValue(merged)
                    callback?.invoke(true)
                } else {
                    // try to read error body (helps diagnose 401/403)
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    val msg = "HTTP $code" + (if (!err.isNullOrBlank()) ": $err" else "")
                    Log.w("NewsViewModel", "fetchTopHeadlines failed: $msg")
                    _error.postValue(msg)
                    callback?.invoke(false)
                }
            } catch (e: Exception) {
                Log.e("NewsViewModel", "fetchTopHeadlines exception", e)
                _error.postValue(e.message ?: "Unknown network error")
                callback?.invoke(false)
            }
        }
    }

    fun clearError() {
        _error.postValue(null)
    }
}

class NewsViewModelFactory(private val repo: ArticleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NewsViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
