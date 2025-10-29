package com.example.news.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.news.R
import com.example.news.model.Article

/**
 * Adapter that holds a combined view of remote headlines and saved articles.
 * For simplicity we maintain two lists; remote are shown first then saved.
 */
class ArticleAdapter(
    private val onItemClick: (Article) -> Unit,
    private val onSaveClick: (Article) -> Unit
) : RecyclerView.Adapter<ArticleAdapter.ViewHolder>() {

    private val remote = mutableListOf<Article>()
    private val saved = mutableListOf<Article>()

    fun submitRemote(list: List<Article>) {
        remote.clear()
        remote.addAll(list)
        notifyDataSetChanged()
    }

    fun submitSaved(list: List<Article>) {
        saved.clear()
        saved.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = remote.size + saved.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_article, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = if (position < remote.size) remote[position] else saved[position - remote.size]
        holder.bind(article, position < remote.size)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.item_icon)
        private val title: TextView = view.findViewById(R.id.item_title)
        private val subtitle: TextView = view.findViewById(R.id.item_subtitle)
        private val saveIcon: ImageView = view.findViewById(R.id.item_save)

        fun bind(a: Article, isRemote: Boolean) {
            title.text = a.title
            subtitle.text = a.description ?: ""
            val ctx = itemView.context
            icon.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_launcher_foreground))
            // color accent for remote vs saved
            val color = if (isRemote) R.color.teal_200 else R.color.purple_200
            itemView.setBackgroundColor(ContextCompat.getColor(ctx, color))

            saveIcon.setOnClickListener {
                onSaveClick(a)
            }

            itemView.setOnClickListener {
                onItemClick(a)
            }
        }
    }
}
