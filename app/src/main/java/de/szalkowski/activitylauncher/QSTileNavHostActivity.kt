package de.szalkowski.activitylauncher

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QSTileNavHostActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TileDialogFragment().show(supportFragmentManager, "")
    }
}

class TileDialogFragment : BottomSheetDialogFragment() {
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        requireActivity().finish()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val navHostId = R.id.nav_host_fragment_content_main

        return FragmentContainerView(requireContext()).apply {
            id = navHostId
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )

            post {
                if (childFragmentManager.findFragmentById(navHostId) == null) {
                    val navHost = NavHostFragment.create(R.navigation.nav_graph)
                    childFragmentManager.beginTransaction().replace(navHostId, navHost)
                        .setPrimaryNavigationFragment(navHost).commitNow()

                    navHost.navController.apply {
                        graph = navInflater.inflate(R.navigation.nav_graph).apply {
                            setStartDestination(R.id.FavoritesFragment)
                        }
                    }
                }
            }
        }
    }
}
