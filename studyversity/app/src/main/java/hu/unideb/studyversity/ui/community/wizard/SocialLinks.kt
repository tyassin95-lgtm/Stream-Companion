package hu.unideb.studyversity.ui.community.wizard

import hu.unideb.studyversity.data.model.CommunityMember

/** Turns what the student typed into a profile link (same rules as the original Utils.setSocialMediaLink). */
object SocialLinks {
    private val facebook = Regex("((http|https)://)?(www[.])?facebook.com/.+")
    private val linkedIn = Regex("((http|https)://)?(www[.])?linkedin.com/.+")
    private val instagram = Regex("((http|https)://)?(www[.])?instagram.com/.+")
    private val tiktok = Regex("((http|https)://)?(www[.])?tiktok.com/.+")

    private fun link(raw: String?, pattern: Regex, prefix: String): String {
        val s = raw.orEmpty().filterNot { it.isWhitespace() }
        return when {
            pattern.matches(s) -> s
            s.isNotBlank() -> prefix + s
            else -> ""
        }
    }

    fun normalise(m: CommunityMember) = m.copy(
        facebookLink = link(m.facebookLink, facebook, "https://www.facebook.com/"),
        linkedInLink = link(m.linkedInLink, linkedIn, "https://www.linkedin.com/in/"),
        instagramLink = link(m.instagramLink, instagram, "https://www.instagram.com/_u/"),
        tiktokLink = link(m.tiktokLink, tiktok, "https://www.tiktok.com/")
    )
}
