package hu.unideb.studyversity.ui.community.memberlist

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import hu.unideb.studyversity.R
import hu.unideb.studyversity.data.model.CommunityMember
import hu.unideb.studyversity.databinding.FragmentMemberListBinding
import hu.unideb.studyversity.databinding.MemberListItemBinding
import hu.unideb.studyversity.databinding.MemberListItemHeaederBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.base.ListItemAdapter
import hu.unideb.studyversity.ui.community.CommunityViewModel
import hu.unideb.studyversity.ui.community.MemberListMode
import hu.unideb.studyversity.ui.community.member_detail.MemberDetailActivity
import hu.unideb.studyversity.utils.ImageLoader

/** Two tabs: students I marked as interesting, and suggestions with shared interests. Search filters by name. */
class MemberListFragment : BaseFragment() {
    private var _binding: FragmentMemberListBinding? = null
    private val binding get() = _binding!!
    private val communityVm: CommunityViewModel by activityViewModels()
    private val header = CommunityMember(id = -1)
    private var members: List<CommunityMember> = emptyList()

    private val adapter = ListItemAdapter<CommunityMember>(
        layoutFor = { item, _ -> if (item.id == -1L) R.layout.member_list_item_heaeder else R.layout.member_list_item }
    ) { layout, view, item, _ ->
        if (layout == R.layout.member_list_item_heaeder) bindHeader(MemberListItemHeaederBinding.bind(view))
        else bindMember(MemberListItemBinding.bind(view), item)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMemberListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbarHost()?.updateSubTitle(getString(R.string.ud_community_title))
        binding.memberList.layoutManager = LinearLayoutManager(requireContext())
        binding.memberList.adapter = adapter
        binding.tabLayout.getTabAt(if (communityVm.mode == MemberListMode.MY_INTERESTS) 0 else 1)?.select()
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                communityVm.mode = if (tab.position == 0) MemberListMode.MY_INTERESTS else MemberListMode.SUGGESTIONS
                refresh()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        adapter.submit(listOf(header))
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() = load(showProgress = false, retry = ::refresh) {
        members = if (communityVm.mode == MemberListMode.MY_INTERESTS) backend.searchMyInterests() else backend.searchSuggestions()
        applyFilter()
    }

    private fun applyFilter() {
        val q = communityVm.search.trim()
        val shown = if (q.isEmpty()) members else members.filter { it.fullName.contains(q, true) || (it.modules ?: "").contains(q, true) }
        adapter.submit(listOf(header) + shown)
    }

    private fun bindHeader(b: MemberListItemHeaederBinding) {
        b.searchTextInputEditText.setText(communityVm.search)
        b.searchTextInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b4: Int, c: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (s.toString() != communityVm.search) { communityVm.search = s.toString(); applyFilter() }
            }
        })
    }

    private fun bindMember(b: MemberListItemBinding, m: CommunityMember) {
        ImageLoader.load(b.profileImage, m.photoUrl)
        b.nameText.text = m.fullName
        b.modulesText.text = m.modules
        b.interestsChipGroup.removeAllViews()
        m.interests.forEach { cat ->
            val chip = LayoutInflater.from(b.root.context).inflate(
                if (cat.isSignificant) R.layout.chip_significant_item else R.layout.chip_item, b.interestsChipGroup, false) as Chip
            chip.text = cat.name
            b.interestsChipGroup.addView(chip)
        }
        b.root.setOnClickListener {
            startActivity(Intent(requireContext(), MemberDetailActivity::class.java).putExtra("memberId", m.id ?: return@setOnClickListener))
        }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
