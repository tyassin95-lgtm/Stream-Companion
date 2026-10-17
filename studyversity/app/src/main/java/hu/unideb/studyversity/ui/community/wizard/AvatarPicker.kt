package hu.unideb.studyversity.ui.community.wizard

import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hu.unideb.studyversity.R
import hu.unideb.studyversity.ui.base.singleLayoutAdapter
import hu.unideb.studyversity.utils.ImageLoader

/** The horizontal strip of bundled default avatars shown on the profile picture page and in the profile editor. */
object AvatarPicker {
    fun attach(list: RecyclerView, avatars: List<String>, onPick: (String) -> Unit) {
        list.layoutManager = LinearLayoutManager(list.context, LinearLayoutManager.HORIZONTAL, false)
        val adapter = singleLayoutAdapter<String>(R.layout.avatar_image_slider_item) { v, url, _ ->
            val image = v.findViewById<ImageView>(R.id.avatarImage)
            ImageLoader.load(image, url)
            v.setOnClickListener { onPick(url) }
        }
        list.adapter = adapter
        adapter.submit(avatars)
    }
}
