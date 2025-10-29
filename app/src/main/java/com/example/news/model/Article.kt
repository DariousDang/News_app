package com.example.news.model

data class Article(
    val id: Long? = null,
    val title: String,
    val description: String? = null,
    val url: String? = null,
    val publishedAt: String? = null
)
