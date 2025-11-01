package com.example.news.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    // Use activity-scoped ViewModel so insert/update events are observed by the list fragment
    viewModel = ViewModelProvider(requireActivity(), NewsViewModelFactory(repo)).get(NewsViewModel::class.java)

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

            if (editId >= 0L) {
                // normal update
                val article = Article(editId, title, desc, url, published, null)
                viewModel.updateArticle(article)
                findNavController().popBackStack()
            } else {
                // We're in create mode. Check if a saved article with the same URL exists.
                if (url.isNotBlank()) {
                    // perform DB lookup off the main thread
                    lifecycleScope.launch(Dispatchers.IO) {
                        val existing = repo.getByUrl(url)
                        if (existing != null) {
                            // convert to main thread update
                            val article = Article(existing.id, title, desc, url, published, null)
                            withContext(Dispatchers.Main) {
                                viewModel.updateArticle(article)
                                findNavController().popBackStack()
                            }
                        } else {
                            // safe insert
                            val article = Article(null, title, desc, url, published, null)
                            withContext(Dispatchers.Main) {
                                viewModel.insertArticle(article)
                                findNavController().popBackStack()
                            }
                        }
                    }
                } else {
                    // no URL to match, just insert
                    val article = Article(null, title, desc, url, published, null)
                    viewModel.insertArticle(article)
                    findNavController().popBackStack()
                }
            }
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
