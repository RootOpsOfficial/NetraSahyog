package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppLanguage
import com.example.model.ObstacleCategory
import com.example.model.ObstaclePriority
import com.example.model.SpatialZone
import com.example.navigation.OfflinePedestrianRouter
import com.example.navigation.PuneOsmDataset
import com.example.perception.ObstaclePriorityEngine
import com.example.perception.SpatialAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context matches NETRASAHYOG`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NETRASAHYOG", appName)
  }

  @Test
  fun `spatial analyzer calculates center zone correctly`() {
    val zone = SpatialAnalyzer.calculateSpatialZone(0.5f)
    assertEquals(SpatialZone.CENTER, zone)
  }

  @Test
  fun `offline pedestrian router generates route to apollo pharmacy`() {
    val router = OfflinePedestrianRouter()
    val destination = PuneOsmDataset.PUNE_POIS.first { it.id == "poi_apollo_pharmacy" }
    val route = router.calculateRoute(18.52043, 73.84365, destination)

    assertTrue(route.isNotEmpty())
    assertEquals(destination.name, route.last().streetOrFootpathName)
  }
}

