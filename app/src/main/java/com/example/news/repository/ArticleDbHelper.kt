package com.example.news.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.news.model.Article

private const val DB_NAME = "articles.db"
private const val DB_VERSION = 2
private const val TABLE = "articles"

class ArticleDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val create = """
            CREATE TABLE $TABLE (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              title TEXT NOT NULL,
              description TEXT,
              url TEXT,
              publishedAt TEXT,
              imageUrl TEXT
            )
        """.trimIndent()
        db.execSQL(create)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Simple migration: add imageUrl column when upgrading from version 1 -> 2
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE ADD COLUMN imageUrl TEXT")
            } catch (e: Exception) {
                // fallback: recreate table
                db.execSQL("DROP TABLE IF EXISTS $TABLE")
                onCreate(db)
            }
        } else {
            db.execSQL("DROP TABLE IF EXISTS $TABLE")
            onCreate(db)
        }
    }

    fun insertArticle(article: Article): Long {
        val cv = ContentValues().apply {
            put("title", article.title)
            put("description", article.description)
            put("url", article.url)
            put("imageUrl", article.imageUrl)
            put("publishedAt", article.publishedAt)
        }
        return writableDatabase.insert(TABLE, null, cv)
    }

    fun updateArticle(article: Article): Int {
        if (article.id == null) return 0
        val cv = ContentValues().apply {
            put("title", article.title)
            put("description", article.description)
            put("url", article.url)
            put("imageUrl", article.imageUrl)
            put("publishedAt", article.publishedAt)
        }
        return writableDatabase.update(TABLE, cv, "id = ?", arrayOf(article.id.toString()))
    }

    fun deleteArticle(id: Long): Int {
        return writableDatabase.delete(TABLE, "id = ?", arrayOf(id.toString()))
    }

    fun getAllArticles(): List<Article> {
        val list = mutableListOf<Article>()
        val cur: Cursor = readableDatabase.query(TABLE, null, null, null, null, null, "id DESC")
        while (cur.moveToNext()) {
            list.add(cursorToArticle(cur))
        }
        cur.close()
        return list
    }

    fun getArticleById(id: Long): Article? {
        val cur = readableDatabase.query(TABLE, null, "id = ?", arrayOf(id.toString()), null, null, null)
        val found = if (cur.moveToFirst()) cursorToArticle(cur) else null
        cur.close()
        return found
    }

    fun getArticleByUrl(url: String): Article? {
        val cur = readableDatabase.query(TABLE, null, "url = ?", arrayOf(url), null, null, null)
        val found = if (cur.moveToFirst()) cursorToArticle(cur) else null
        cur.close()
        return found
    }

    private fun cursorToArticle(cur: Cursor): Article {
        val id = cur.getLong(cur.getColumnIndexOrThrow("id"))
        val title = cur.getString(cur.getColumnIndexOrThrow("title"))
        val description = cur.getString(cur.getColumnIndexOrThrow("description"))
        val url = cur.getString(cur.getColumnIndexOrThrow("url"))
        val publishedAt = cur.getString(cur.getColumnIndexOrThrow("publishedAt"))
        val imageUrl = try { cur.getString(cur.getColumnIndexOrThrow("imageUrl")) } catch (e: Exception) { null }
        return Article(id = id, title = title ?: "", description = description, url = url, publishedAt = publishedAt, imageUrl = imageUrl)
    }
}
