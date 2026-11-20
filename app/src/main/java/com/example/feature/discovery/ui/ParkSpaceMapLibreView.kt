package com.example.feature.discovery.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.config.AppEnvironment
import com.example.core.config.SupportedCity
import com.example.core.designsystem.components.ParkSpaceLiquidGlassButton
import com.example.core.designsystem.components.ParkSpaceLiquidGlassSecondaryButton
import com.example.feature.discovery.domain.model.MapRenderStatus
import com.example.feature.discovery.domain.model.PublicParkingMarker
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

internal const val MARKER_SOURCE_ID = "parkspace-public-markers-source"
internal const val MASK_ZONE_LAYER_ID = "parkspace-mask-zone-layer"
internal const val MARKER_PIN_LAYER_ID = "parkspace-marker-pin-layer"
internal const val PIN_ICON_DEFAULT = "parkspace-pin-default"
internal const val PIN_ICON_SELECTED = "parkspace-pin-selected"

/**
 * Pure GeoJSON builder using strictly 200m-masked PublicParkingMarker coordinates.
 */
internal fun buildMarkerFeatureCollection(
  markers: List<PublicParkingMarker>,
): FeatureCollection {
  val features =
    markers.map { marker ->
      Feature.fromGeometry(Point.fromLngLat(marker.longitude, marker.latitude)).apply {
        addStringProperty("space_id", marker.spaceId)
        addStringProperty("price_label", marker.displayPriceLabel)
        addStringProperty("area_label", marker.areaLabel)
        addBooleanProperty("is_selected", marker.isSelected)
      }
    }
  return FeatureCollection.fromFeatures(features)
}

internal fun isNetworkAvailable(context: Context): Boolean {
  val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
  val network = cm.activeNetwork ?: return false
  val caps = cm.getNetworkCapabilities(network) ?: return false
  return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
fun ParkSpaceMapLibreView(
  city: SupportedCity,
  markers: List<PublicParkingMarker>,
  mapRenderStatus: MapRenderStatus,
  cameraTriggerVersion: Int,
  onMarkerSelected: (String) -> Unit,
  onStyleLoaded: () -> Unit,
  onMapLoadFailed: (String) -> Unit,
  onRetryMap: () -> Unit,
  onRecenterCity: () -> Unit,
  onSwitchToList: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val currentMarkers by rememberUpdatedState(markers)
  val currentOnMarkerSelected by rememberUpdatedState(onMarkerSelected)
  val currentOnStyleLoaded by rememberUpdatedState(onStyleLoaded)
  val currentOnMapLoadFailed by rememberUpdatedState(onMapLoadFailed)

  var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
  var loadedStyle by remember { mutableStateOf<Style?>(null) }

  val mapView = remember {
    MapLibre.getInstance(context.applicationContext)
    MapView(context).apply {
      onCreate(null)
    }
  }

  // Forward Compose lifecycle cleanly to MapView
  DisposableEffect(lifecycleOwner, mapView) {
    val failListener =
      MapView.OnDidFailLoadingMapListener { errorMessage ->
        currentOnMapLoadFailed(
          "OpenFreeMap Liberty tiles could not load ($errorMessage). Switch to List view or retry."
        )
      }
    mapView.addOnDidFailLoadingMapListener(failListener)

    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_START -> mapView.onStart()
        Lifecycle.Event.ON_RESUME -> mapView.onResume()
        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
        Lifecycle.Event.ON_STOP -> mapView.onStop()
        Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose {
      mapView.removeOnDidFailLoadingMapListener(failListener)
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  fun loadLibertyStyle(map: MapLibreMap) {
    if (!isNetworkAvailable(context)) {
      currentOnMapLoadFailed(
        "No internet connection detected for OpenFreeMap Liberty tiles. You can browse all listings in List view."
      )
      return
    }
    map.setStyle(Style.Builder().fromUri(AppEnvironment.MAP_STYLE_URL)) { style ->
      ensureParkingLayers(style, currentMarkers)
      loadedStyle = style
      currentOnStyleLoaded()
    }
  }

  LaunchedEffect(mapView) {
    mapView.getMapAsync { map ->
      mapLibreMap = map
      map.uiSettings.isAttributionEnabled = true
      map.uiSettings.isLogoEnabled = false
      map.cameraPosition =
        CameraPosition.Builder()
          .target(LatLng(city.centerLatitude, city.centerLongitude))
          .zoom(city.defaultZoom)
          .build()

      map.addOnMapClickListener { latLng ->
        val screenPoint = map.projection.toScreenLocation(latLng)
        val hitFeatures =
          map.queryRenderedFeatures(screenPoint, MARKER_PIN_LAYER_ID, MASK_ZONE_LAYER_ID)
        val spaceId = hitFeatures.firstOrNull()?.getStringProperty("space_id")
        if (!spaceId.isNullOrBlank()) {
          currentOnMarkerSelected(spaceId)
          true
        } else {
          false
        }
      }

      loadLibertyStyle(map)
    }
  }

  // Re-load style if user taps Retry
  LaunchedEffect(mapRenderStatus) {
    if (mapRenderStatus is MapRenderStatus.LoadingStyle) {
      mapLibreMap?.let { loadLibertyStyle(it) }
    }
  }

  // Move camera smoothly on city change or recenter trigger
  LaunchedEffect(city, cameraTriggerVersion, mapLibreMap) {
    mapLibreMap?.animateCamera(
      CameraUpdateFactory.newLatLngZoom(
        LatLng(city.centerLatitude, city.centerLongitude),
        city.defaultZoom,
      ),
      450,
    )
  }

  // Push updated GeoJSON markers whenever filters or selection change
  LaunchedEffect(markers, loadedStyle) {
    loadedStyle?.let { style ->
      val source = style.getSourceAs<GeoJsonSource>(MARKER_SOURCE_ID)
      source?.setGeoJson(buildMarkerFeatureCollection(markers))
    }
  }

  Box(modifier = modifier.fillMaxSize().testTag("maplibre_container")) {
    AndroidView(
      factory = { mapView },
      modifier = Modifier.fillMaxSize(),
    )

    // Accessible Zoom & Recenter Controls
    Column(
      modifier =
        Modifier.align(Alignment.TopEnd)
          .padding(top = ParkSpaceSpacing.sm, end = ParkSpaceSpacing.sm),
      verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
    ) {
      FilledIconButton(
        onClick = { mapLibreMap?.animateCamera(CameraUpdateFactory.zoomIn(), 250) },
        modifier = Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("map_zoom_in_button"),
        colors =
          IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface,
          ),
      ) {
        Icon(imageVector = Icons.Outlined.Add, contentDescription = "Zoom in map")
      }

      FilledIconButton(
        onClick = { mapLibreMap?.animateCamera(CameraUpdateFactory.zoomOut(), 250) },
        modifier = Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("map_zoom_out_button"),
        colors =
          IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface,
          ),
      ) {
        Icon(imageVector = Icons.Outlined.Remove, contentDescription = "Zoom out map")
      }

      FilledIconButton(
        onClick = onRecenterCity,
        modifier = Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("map_recenter_button"),
        colors =
          IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
      ) {
        Icon(
          imageVector = Icons.Outlined.MyLocation,
          contentDescription = "Re-center map on ${city.displayName}",
        )
      }
    }

    val semantic = ParkSpaceThemeTokens.semantic

    // Loading Style Overlay
    if (mapRenderStatus is MapRenderStatus.LoadingStyle) {
      Surface(
        modifier =
          Modifier.align(Alignment.TopStart)
            .padding(ParkSpaceSpacing.sm)
            .testTag("map_loading_badge"),
        shape = CircleShape,
        color = semantic.glassSurfaceElevated,
        border = BorderStroke(1.dp, semantic.glassBorder),
        tonalElevation = 2.dp,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = ParkSpaceSpacing.sm, vertical = ParkSpaceSpacing.xs),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
          Text(
            text = "Loading OpenFreeMap Liberty...",
            style = MaterialTheme.typography.labelMedium,
          )
        }
      }
    }

    // Graceful Offline / Tile Error Overlay
    if (mapRenderStatus is MapRenderStatus.OfflineOrError) {
      Box(
        modifier = Modifier.fillMaxSize().padding(ParkSpaceSpacing.lg),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().testTag("map_error_overlay"),
          shape = RoundedCornerShape(22.dp),
          color = semantic.glassSurfaceElevated,
          border = BorderStroke(1.dp, semantic.glassBorder),
          tonalElevation = 3.dp,
        ) {
          Column(
            modifier = Modifier.padding(ParkSpaceSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
          ) {
            Icon(
              imageVector = Icons.Outlined.CloudOff,
              contentDescription = "Map tiles unavailable",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(36.dp),
            )
            Text(
              text = "Map Tiles Unavailable",
              style = MaterialTheme.typography.titleMedium,
              textAlign = TextAlign.Center,
            )
            Text(
              text = mapRenderStatus.message,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              ParkSpaceLiquidGlassSecondaryButton(
                text = "Retry",
                leadingIcon = Icons.Outlined.Refresh,
                onClick = onRetryMap,
                modifier = Modifier.weight(1f).height(ParkSpaceSpacing.minTouchTarget),
              )
              ParkSpaceLiquidGlassButton(
                text = "List View",
                onClick = onSwitchToList,
                leadingIcon = Icons.Outlined.ViewList,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    }
  }
}

private fun ensureParkingLayers(
  style: Style,
  markers: List<PublicParkingMarker>,
) {
  if (style.getImage(PIN_ICON_DEFAULT) == null) {
    style.addImage(
      PIN_ICON_DEFAULT,
      createPinMarkerBitmap(
        fillColor = Color(0xFF0D3C34).toArgb(),
        borderColor = Color(0xFFFAF9F6).toArgb(),
        dotColor = Color(0xFF2ED39A).toArgb(),
      ),
    )
  }
  if (style.getImage(PIN_ICON_SELECTED) == null) {
    style.addImage(
      PIN_ICON_SELECTED,
      createPinMarkerBitmap(
        fillColor = Color(0xFF9D6B14).toArgb(),
        borderColor = Color(0xFFFAF9F6).toArgb(),
        dotColor = Color(0xFFFAF9F6).toArgb(),
      ),
    )
  }

  if (style.getSource(MARKER_SOURCE_ID) == null) {
    style.addSource(GeoJsonSource(MARKER_SOURCE_ID, buildMarkerFeatureCollection(markers)))
  }

  if (style.getLayer(MASK_ZONE_LAYER_ID) == null) {
    val maskZoneLayer =
      CircleLayer(MASK_ZONE_LAYER_ID, MARKER_SOURCE_ID).withProperties(
        PropertyFactory.circleRadius(26f),
        PropertyFactory.circleColor("#19B87D"),
        PropertyFactory.circleOpacity(0.18f),
        PropertyFactory.circleStrokeColor("#0D3C34"),
        PropertyFactory.circleStrokeWidth(1.5f),
      )
    style.addLayer(maskZoneLayer)
  }

  if (style.getLayer(MARKER_PIN_LAYER_ID) == null) {
    val pinLayer =
      SymbolLayer(MARKER_PIN_LAYER_ID, MARKER_SOURCE_ID).withProperties(
        PropertyFactory.iconImage(
          Expression.switchCase(
            Expression.eq(Expression.get("is_selected"), Expression.literal(true)),
            Expression.literal(PIN_ICON_SELECTED),
            Expression.literal(PIN_ICON_DEFAULT),
          )
        ),
        PropertyFactory.iconSize(
          Expression.switchCase(
            Expression.eq(Expression.get("is_selected"), Expression.literal(true)),
            Expression.literal(1.2f),
            Expression.literal(1.0f),
          )
        ),
        PropertyFactory.iconAllowOverlap(true),
        PropertyFactory.iconIgnorePlacement(true),
      )
    style.addLayer(pinLayer)
  }
}

private fun createPinMarkerBitmap(
  fillColor: Int,
  borderColor: Int,
  dotColor: Int,
): Bitmap {
  val size = 72
  val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)

  val outerPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = borderColor
      style = Paint.Style.FILL
    }
  val innerPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = fillColor
      style = Paint.Style.FILL
    }
  val dotPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = dotColor
      style = Paint.Style.FILL
    }

  val cx = size / 2f
  val cy = size / 2f
  canvas.drawCircle(cx, cy, 26f, outerPaint)
  canvas.drawCircle(cx, cy, 22f, innerPaint)
  val innerBadge = RectF(cx - 8f, cy - 8f, cx + 8f, cy + 8f)
  canvas.drawRoundRect(innerBadge, 6f, 6f, dotPaint)

  return bitmap
}
