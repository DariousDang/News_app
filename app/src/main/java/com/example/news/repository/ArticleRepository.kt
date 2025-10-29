package com.example.news.repository

import android.content.Context
import com.example.news.model.Article

class ArticleRepository(context: Context) {
    private val db = ArticleDbHelper(context)

    fun insert(article: Article): Long {
        return db.insertArticle(article)
    }

    fun update(article: Article): Int {
        return db.updateArticle(article)
    }

    fun delete(id: Long): Int {
        return db.deleteArticle(id)
    }

    fun getAll(): List<Article> {
        return db.getAllArticles()
    }

    fun getById(id: Long): Article? {
        return db.getArticleById(id)
    }
}
