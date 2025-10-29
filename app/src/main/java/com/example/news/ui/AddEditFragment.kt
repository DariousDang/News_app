package com.example.news.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.news.databinding.FragmentAddEditBinding
import com.example.news.model.Article
import com.example.news.repository.ArticleRepository

/**
 * Simple add / edit fragment for local articles.
 */
class AddEditFragment : Fragment() {

    private var _binding: FragmentAddEditBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NewsViewModel

    private var editId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repo = ArticleRepository(requireContext())
        viewModel = ViewModelProvider(this, NewsViewModelFactory(repo)).get(NewsViewModel::class.java)

        // If args present, populate fields
        val args = arguments
        args?.let {
            editId = it.getLong("id", -1L)
            binding.titleEdit.setText(it.getString("title", ""))
            binding.descriptionEdit.setText(it.getString("description", ""))
            binding.urlEdit.setText(it.getString("url", ""))
            binding.publishedAtEdit.setText(it.getString("publishedAt", ""))
        }

        binding.saveButton.setOnClickListener {
            val title = binding.titleEdit.text.toString().ifBlank { "(no title)" }
            val desc = binding.descriptionEdit.text.toString()
            val url = binding.urlEdit.text.toString()
            val published = binding.publishedAtEdit.text.toString()
            val article = if (editId >= 0L) Article(editId, title, desc, url, published) else Article(null, title, desc, url, published)

            if (editId >= 0L) viewModel.updateArticle(article) else viewModel.insertArticle(article)
            findNavController().popBackStack()
        }

        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
