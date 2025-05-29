package org.thebetterinternet.aria

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.mozilla.geckoview.GeckoView

class TabFragment : Fragment() {
    private var tabPosition: Int = -1
    private var geckoView: GeckoView? = null

    companion object {
        private const val ARG_TAB_POSITION = "tab_position"
        private var tabsReference: List<BrowserTab> = emptyList()

        fun newInstance(position: Int): TabFragment {
            return TabFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TAB_POSITION, position)
                }
            }
        }

        fun setTabsReference(tabs: List<BrowserTab>) {
            tabsReference = tabs
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabPosition = arguments?.getInt(ARG_TAB_POSITION) ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return GeckoView(requireContext()).also { geckoView ->
            this.geckoView = geckoView
            geckoView.isNestedScrollingEnabled = true
            geckoView.isVerticalScrollBarEnabled = true
            geckoView.autofillEnabled = true
            val tab = tabsReference.getOrNull(tabPosition)
            tab?.geckoSession?.let { session ->
                geckoView.setSession(session)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        geckoView = null
    }
}