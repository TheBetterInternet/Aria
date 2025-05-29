package org.thebetterinternet.aria

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class TabPagerAdapter(
    private var tabs: List<BrowserTab>,
    private val fragmentManager: FragmentManager,
    private val lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment {
        return TabFragment.newInstance(position)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateTabs(newTabs: List<BrowserTab>) {
        tabs = newTabs
        notifyDataSetChanged()
    }

    fun getTab(position: Int): BrowserTab? = tabs.getOrNull(position)
}