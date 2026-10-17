package hu.unideb.studyversity.ui.community.wizard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.model.CategoryItem
import hu.unideb.studyversity.data.model.CommunityMember
import hu.unideb.studyversity.ui.community.ChipSelectionType
import hu.unideb.studyversity.utils.ImageLoader
import kotlinx.coroutines.launch
import java.util.Locale

/** The profile being built (registration wizard) or edited (profile screen). Shared by all its fragments. */
class WizardViewModel : ViewModel() {
    private val backend get() = ServiceLocator.backend

    private val _member = MutableLiveData(CommunityMember())
    val member: LiveData<CommunityMember> get() = _member
    var current: CommunityMember
        get() = _member.value!!
        set(v) { _member.value = v }

    var privacyAccepted = false
    var termsAccepted = false

    val avatars: List<String> get() = ImageLoader.defaultAvatars(ServiceLocator.appContext)

    /** All categories of one group, cached because the chip selector reopens often. */
    private val categoryCache = HashMap<ChipSelectionType, List<CategoryItem>>()

    suspend fun categories(type: ChipSelectionType): List<CategoryItem> =
        categoryCache.getOrPut(type) { backend.categories(listOf(type.code)) }

    fun selection(type: ChipSelectionType): List<CategoryItem> = when (type) {
        ChipSelectionType.INTERESTS -> current.interests
        ChipSelectionType.FACULTY_NAMES -> current.faculties
        ChipSelectionType.LANGUAGES -> current.preferredLanguages
    }

    fun setSelection(type: ChipSelectionType, items: List<CategoryItem>) {
        current = when (type) {
            ChipSelectionType.INTERESTS -> current.copy(interests = items)
            ChipSelectionType.FACULTY_NAMES -> current.copy(faculties = items)
            ChipSelectionType.LANGUAGES -> current.copy(preferredLanguages = items)
        }
    }

    fun setPhoto(url: String?) { current = current.copy(photoUrl = url) }
    fun setDescription(text: String) { current = current.copy(description = text) }

    fun setLinks(facebook: String, linkedIn: String, instagram: String, tiktok: String) {
        current = SocialLinks.normalise(current.copy(facebookLink = facebook, linkedInLink = linkedIn, instagramLink = instagram, tiktokLink = tiktok))
    }

    fun load(member: CommunityMember) { current = member }

    fun register(onDone: () -> Unit, onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try { backend.registerCommunityMember(current); onDone() } catch (e: Exception) { onError(e) }
        }
    }

    fun save(onDone: () -> Unit, onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try { _member.value = backend.updateCommunityMember(current); onDone() } catch (e: Exception) { onError(e) }
        }
    }

    fun uploadPhoto(bytes: ByteArray, onDone: (String) -> Unit, onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try { val url = backend.uploadProfileImage(bytes); setPhoto(url); onDone(url) } catch (e: Exception) { onError(e) }
        }
    }

    /** Selected chips first, then the rest alphabetically (locale aware) – the order of the chip selector. */
    fun sortedForSelector(all: List<CategoryItem>, selected: Set<Long>): List<CategoryItem> {
        val collator = java.text.Collator.getInstance(Locale.getDefault())
        val (sel, rest) = all.partition { it.id in selected }
        return sel + rest.sortedWith { a, b -> collator.compare(a.name ?: "", b.name ?: "") }
    }
}
