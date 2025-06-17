package de.szalkowski.activitylauncher.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.databinding.FragmentFavoritesBinding
import de.szalkowski.activitylauncher.databinding.FragmentPackageListBinding
import de.szalkowski.activitylauncher.services.ViewIntentParserService
import javax.inject.Inject

@AndroidEntryPoint
class FavoritesFragment : Fragment() {
    private var _binding: FragmentFavoritesBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
