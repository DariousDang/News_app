package com.example.news.ui

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
    private val bookmarkIcon: ImageView = view.findViewById(R.id.item_bookmark)
    private val shareIcon: ImageView = view.findViewById(R.id.item_share)

        fun bind(a: Article, isRemote: Boolean) {
            title.text = a.title
            subtitle.text = a.description ?: ""
            val ctx = itemView.context
            // Load thumbnail with Glide only for valid http(s) URLs; fallback to app icon
            val img = a.imageUrl?.trim()
            val validUrl = if (!img.isNullOrBlank() && (img.startsWith("http://") || img.startsWith("https://"))) img else null
            if (validUrl != null) {
                try {
                    Glide.with(ctx)
                        .load(validUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(icon)
                } catch (e: Exception) {
                    // swallow Glide errors and show placeholder
                    icon.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_launcher_foreground))
                }
            } else {
                icon.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_launcher_foreground))
            }
            // Monochrome backgrounds: remote items slightly gray, saved items white
            val color = if (isRemote) R.color.light_gray else R.color.white
            itemView.setBackgroundColor(ContextCompat.getColor(ctx, color))

            // enforce text/icon colors to monochrome palette
            title.setTextColor(ContextCompat.getColor(ctx, R.color.black))
            subtitle.setTextColor(ContextCompat.getColor(ctx, R.color.mid_gray))
            bookmarkIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.dark_gray))
            // bookmark state reflects whether article has an id (saved locally)
            val isSaved = a.id != null
            val bmDrawable = if (isSaved) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border
            bookmarkIcon.setImageDrawable(ContextCompat.getDrawable(ctx, bmDrawable))
            bookmarkIcon.setOnClickListener {
                onSaveClick(a)
            }

            shareIcon.setOnClickListener {
                // open system share sheet with article URL (fallback to title)
                val shareText = a.url ?: a.title
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                val chooser = Intent.createChooser(intent, ctx.getString(R.string.share_article))
                ctx.startActivity(chooser)
            }

            itemView.setOnClickListener {
                onItemClick(a)
            }
        }
    }
}
