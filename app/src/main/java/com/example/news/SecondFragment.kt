package com.example.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.news.databinding.FragmentSecondBinding
import com.example.news.model.Article
import com.example.news.repository.ArticleRepository
import com.example.news.ui.NewsViewModel
import com.example.news.ui.NewsViewModelFactory

/**
 * Article detail view (replaces original SecondFragment behavior).
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: NewsViewModel
    private var currentId: Long = -1L
    private var currentUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    val repo = ArticleRepository(requireContext())
    // Share the ViewModel with the activity so delete/edit are reflected in the list
    viewModel = ViewModelProvider(requireActivity(), NewsViewModelFactory(repo)).get(NewsViewModel::class.java)

        // Read arguments passed from list
        var title = arguments?.getString("title") ?: ""
        var description = arguments?.getString("description") ?: ""
        var url = arguments?.getString("url") ?: ""
        var publishedAt = arguments?.getString("publishedAt") ?: ""
        val id = arguments?.getLong("id") ?: -1L
        currentId = id
        currentUrl = if (url.isBlank()) null else url

        binding.titleText.text = title
        binding.descriptionText.text = description
        binding.urlText.text = url
        binding.publishedAtText.text = publishedAt

        // Observe saved articles so the detail view refreshes immediately after edits
        viewModel.savedArticles.observe(viewLifecycleOwner) { list ->
            if (currentId >= 0L) {
                val found = list.find { it.id == currentId }
                if (found != null) {
                    binding.titleText.text = found.title
                    binding.descriptionText.text = found.description
                    binding.urlText.text = found.url
                    binding.publishedAtText.text = found.publishedAt
                }
            } else if (!currentUrl.isNullOrBlank()) {
                // if this detail was opened for a remote item and it was saved, update fields to match saved row
                val found = list.find { !it.url.isNullOrBlank() && it.url == currentUrl }
                if (found != null) {
                    currentId = found.id ?: currentId
                    binding.titleText.text = found.title
                    binding.descriptionText.text = found.description
                    binding.urlText.text = found.url
                    binding.publishedAtText.text = found.publishedAt
                }
            }
        }

        if (id >= 0L) {
            binding.deleteButton.setOnClickListener {
                viewModel.deleteById(id)
                findNavController().popBackStack()
            }
            binding.editButton.setOnClickListener {
                // navigate to add/edit with same args
                findNavController().navigate(R.id.action_SecondFragment_to_addEditFragment, arguments)
            }
        } else {
            // remote article: cannot delete (not saved). Make Edit save then open editor.
            binding.deleteButton.isEnabled = false
            binding.deleteButton.alpha = 0.6f
            binding.editButton.setOnClickListener {
                val toSave = Article(null, title, description, url, publishedAt, null)
                viewModel.insertArticleWithCallback(toSave) { newId ->
                    val bundle = Bundle().apply {
                        putLong("id", newId)
                        putString("title", title)
                        putString("description", description)
                        putString("url", url)
                        putString("publishedAt", publishedAt)
                    }
                    requireActivity().runOnUiThread {
                        val nav = findNavController()
                        // Only navigate if we're not already on the Add/Edit destination (prevents IllegalArgumentException)
                        if (nav.currentDestination?.id != R.id.addEditFragment) {
                            // navigate directly to the destination id (safe even if current destination changed)
                            nav.navigate(R.id.addEditFragment, bundle)
                        }
                    }
                }
            }
        }

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}