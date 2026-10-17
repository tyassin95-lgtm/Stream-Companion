package hu.unideb.studyversity.utils.image

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import hu.unideb.studyversity.databinding.ActivityImageShowBinding
import java.io.File

/** Zoomable full screen picture. Extra: `imageResource` (url). */
class ImageShowActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra("imageResource")
        if (url == null) { finish(); return }
        val binding = ActivityImageShowBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.closeText.setOnClickListener { finish() }
        Glide.with(this).downloadOnly().load(url).into(object : CustomTarget<File>() {
            override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                binding.subsamplingScaleImageView.setImage(ImageSource.uri(Uri.fromFile(resource)))
            }
            override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
        })
    }

    override fun onPause() {
        overridePendingTransition(0, android.R.anim.fade_out)
        super.onPause()
    }
}
