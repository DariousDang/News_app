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
        viewModel = ViewModelProvider(this, NewsViewModelFactory(repo)).get(NewsViewModel::class.java)

        // Read arguments passed from list
        val title = arguments?.getString("title") ?: ""
        val description = arguments?.getString("description") ?: ""
        val url = arguments?.getString("url") ?: ""
        val publishedAt = arguments?.getString("publishedAt") ?: ""
        val id = arguments?.getLong("id") ?: -1L

        binding.titleText.text = title
        binding.descriptionText.text = description
        binding.urlText.text = url
        binding.publishedAtText.text = publishedAt

        binding.deleteButton.setOnClickListener {
            if (id >= 0L) {
                viewModel.deleteById(id)
            }
            findNavController().popBackStack()
        }

        binding.editButton.setOnClickListener {
            // navigate to add/edit with same args
            findNavController().navigate(R.id.action_SecondFragment_to_addEditFragment, arguments)
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