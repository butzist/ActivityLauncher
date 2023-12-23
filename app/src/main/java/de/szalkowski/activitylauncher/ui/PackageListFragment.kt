package de.szalkowski.activitylauncher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.databinding.FragmentPackageListBinding
import javax.inject.Inject

@AndroidEntryPoint
class PackageListFragment : Fragment() {
    @Inject
    internal lateinit var packageListAdapter: PackageListAdapter

    private var _binding: FragmentPackageListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPackageListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        packageListAdapter.onItemClick = {
            val action = PackageListFragmentDirections.actionSelectPackage(it.packageName)
            findNavController().navigate(action)
        }
        binding.rvPackages.adapter = packageListAdapter
        binding.rvPackages.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
