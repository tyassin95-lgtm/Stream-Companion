package hu.unideb.studyversity.utils.image

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import hu.unideb.studyversity.R
import hu.unideb.studyversity.databinding.ActivityImageSlideShowBinding
import java.io.File

/** Swipe through a set of pictures. Extras: `imageUrls` (String[]), `position`. */
class ImageSlideShowActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityImageSlideShowBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val urls = intent.getStringArrayExtra("imageUrls")?.toList().orEmpty()
        binding.imageSliderVp.adapter = Adapter(urls)
        binding.imageSliderVp.setPageTransformer(MarginPageTransformer(100))
        binding.imageSliderVp.setCurrentItem(intent.getIntExtra("position", 0), false)
        binding.closeText.visibility = View.VISIBLE
        binding.closeText.setOnClickListener { finish() }
    }

    override fun onPause() {
        overridePendingTransition(0, android.R.anim.fade_out)
        super.onPause()
    }

    private class Adapter(private val urls: List<String>) : RecyclerView.Adapter<Adapter.VH>() {
        class VH(val view: View) : RecyclerView.ViewHolder(view)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(android.view.LayoutInflater.from(parent.context).inflate(R.layout.image_slideshow_item, parent, false))
        override fun getItemCount() = urls.size
        override fun onBindViewHolder(holder: VH, position: Int) {
            val image = holder.view.findViewById<SubsamplingScaleImageView>(R.id.subsamplingScaleImageView)
            Glide.with(holder.view).downloadOnly().load(urls[position]).into(object : CustomTarget<File>() {
                override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                    image.setImage(ImageSource.uri(Uri.fromFile(resource)))
                }
                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
            })
        }
    }
}
