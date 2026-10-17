package hu.unideb.studyversity

import android.app.Application
import hu.unideb.studyversity.core.ServiceLocator
import org.osmdroid.config.Configuration

class StudyversityApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        // OpenStreetMap tiles are used for the campus map (the original used Google Maps).
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = cacheDir
            osmdroidTileCache = cacheDir.resolve("osm-tiles")
        }
    }
}
