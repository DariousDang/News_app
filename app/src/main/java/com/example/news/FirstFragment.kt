package com.example.news

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.news.databinding.FragmentFirstBinding
import com.example.news.model.Article
import com.example.news.repository.ArticleRepository
import com.example.news.ui.ArticleAdapter
import com.example.news.ui.NewsViewModel
import com.example.news.BuildConfig
import com.google.android.material.snackbar.Snackbar
import com.example.news.ui.NewsViewModelFactory

/**
 * News list fragment (replaces original FirstFragment behavior).
 * Shows a RecyclerView of articles (remote headlines + saved local items) and allows add/save.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ArticleAdapter
    private lateinit var viewModel: NewsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup ViewModel with repository that needs a Context
        val repo = ArticleRepository(requireContext())
        viewModel = ViewModelProvider(this, NewsViewModelFactory(repo)).get(NewsViewModel::class.java)

        // Warn if API key is missing in BuildConfig (local.properties -> NEWS_API_KEY)
        if (BuildConfig.NEWS_API_KEY.isBlank()) {
            Snackbar.make(binding.root, "API key missing — add NEWS_API_KEY to local.properties and rebuild.", Snackbar.LENGTH_INDEFINITE)
                .setAction("OK") { /* dismiss */ }
                .show()
        }

        // RecyclerView + adapter
        adapter = ArticleAdapter({ article ->
            // on click -> navigate to detail
            val bundle = Bundle().apply {
                putLong("id", article.id ?: -1L)
                putString("title", article.title)
                putString("description", article.description)
                putString("url", article.url)
                putString("publishedAt", article.publishedAt)
            }
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment, bundle)
        }, { article ->
            // on save click -> insert locally
            viewModel.insertArticle(article)
        })

        val rv = binding.recyclerView
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        rv.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        // Observe saved articles
        viewModel.savedArticles.observe(viewLifecycleOwner) { list ->
            adapter.submitSaved(list)
            // show/hide empty state
            binding.emptyView.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
        }

        // Observe errors from fetch
        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let {
                Snackbar.make(binding.root, "Error: $it", Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // FAB navigates to add screen
        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_addEditFragment)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchTopHeadlines { _ ->
                binding.swipeRefresh.isRefreshing = false
            }
        }

        // Observe remote headlines
        viewModel.headlines.observe(viewLifecycleOwner) { list ->
            adapter.submitRemote(list)
            // show/hide empty state
            binding.emptyView.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
        }

        // initial load
        viewModel.loadSavedArticles()
        viewModel.fetchTopHeadlines()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}