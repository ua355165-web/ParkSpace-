package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Brand Core — Deep Architectural Emerald & Signal Jade
val Emerald950 = Color(0xFF07231E)
val Emerald900 = Color(0xFF0D3C34)
val Emerald800 = Color(0xFF135449)
val Emerald700 = Color(0xFF176B5C)
val Emerald100 = Color(0xFFC8EFE0)
val Emerald50 = Color(0xFFE9F8F2)

val Jade500 = Color(0xFF19B87D)
val Jade400 = Color(0xFF2ED39A)
val Jade200 = Color(0xFF9DEFD0)
val Jade900 = Color(0xFF064430)

// Neutral Surfaces — Warm Limestone (Light) & Obsidian Asphalt (Dark)
val Limestone50 = Color(0xFFFAF9F6)
val Limestone100 = Color(0xFFF3F1EC)
val Limestone200 = Color(0xFFE7E4DC)
val Limestone300 = Color(0xFFD5D0C5)
val SlateInk900 = Color(0xFF121A17)
val SlateInk700 = Color(0xFF3E4A45)
val SlateInk500 = Color(0xFF64726C)

val Asphalt950 = Color(0xFF0B110F)
val Asphalt900 = Color(0xFF111A17)
val Asphalt850 = Color(0xFF17231F)
val Asphalt800 = Color(0xFF1F2F2A)
val Asphalt700 = Color(0xFF2E423B)
val Mist100 = Color(0xFFEBF1EE)
val Mist300 = Color(0xFFB5C4BD)

// Secondary Warm Brass / Transit Gold (accents, verified host badges)
val Brass600 = Color(0xFF9D6B14)
val Brass400 = Color(0xFFE0A63A)
val Brass100 = Color(0xFFFAEBD0)
val Brass900 = Color(0xFF3E2804)

// Semantic Status Tokens
val AvailableGreen = Color(0xFF15804B)
val AvailableGreenDark = Color(0xFF3AD28B)
val AvailableContainerLight = Color(0xFFDCF5E7)
val AvailableContainerDark = Color(0xFF113825)

val PendingAmber = Color(0xFFB86B0A)
val PendingAmberDark = Color(0xFFF5B547)
val PendingContainerLight = Color(0xFFFEF0D6)
val PendingContainerDark = Color(0xFF3B2809)

val ErrorCrimson = Color(0xFFBA2D2D)
val ErrorCrimsonDark = Color(0xFFF27373)
val ErrorContainerLight = Color(0xFFFDE3E3)
val ErrorContainerDark = Color(0xFF471515)

val InfoCobalt = Color(0xFF1D5FA8)
val InfoCobaltDark = Color(0xFF6EB0F7)
val InfoContainerLight = Color(0xFFE2EFFD)
val InfoContainerDark = Color(0xFF122C49)

@Immutable
data class ParkSpaceSemanticColors(
  val available: Color,
  val onAvailable: Color,
  val availableContainer: Color,
  val pending: Color,
  val onPending: Color,
  val pendingContainer: Color,
  val info: Color,
  val onInfo: Color,
  val infoContainer: Color,
  val demoBannerBg: Color,
  val demoBannerFg: Color,
  val glassSurface: Color,
  val glassSurfaceElevated: Color,
  val glassBorder: Color,
  val glassSpecularRim: Color,
  val customerGlow: Color,
  val hostGlow: Color,
)

val LightSemanticColors =
  ParkSpaceSemanticColors(
    available = AvailableGreen,
    onAvailable = Color.White,
    availableContainer = AvailableContainerLight,
    pending = PendingAmber,
    onPending = Color.White,
    pendingContainer = PendingContainerLight,
    info = InfoCobalt,
    onInfo = Color.White,
    infoContainer = InfoContainerLight,
    demoBannerBg = Brass100,
    demoBannerFg = Brass900,
    glassSurface = Color(0xE6FFFFFF),
    glassSurfaceElevated = Color(0xF5FFFFFF),
    glassBorder = Color(0x330D3C34),
    glassSpecularRim = Color(0xCCFFFFFF),
    customerGlow = Color(0x1F19B87D),
    hostGlow = Color(0x1F9D6B14),
  )

val DarkSemanticColors =
  ParkSpaceSemanticColors(
    available = AvailableGreenDark,
    onAvailable = Asphalt950,
    availableContainer = AvailableContainerDark,
    pending = PendingAmberDark,
    onPending = Asphalt950,
    pendingContainer = PendingContainerDark,
    info = InfoCobaltDark,
    onInfo = Asphalt950,
    infoContainer = InfoContainerDark,
    demoBannerBg = Brass900,
    demoBannerFg = Brass100,
    glassSurface = Color(0xCC15221E),
    glassSurfaceElevated = Color(0xE01C2D27),
    glassBorder = Color(0x472ED39A),
    glassSpecularRim = Color(0x3D9DEFD0),
    customerGlow = Color(0x332ED39A),
    hostGlow = Color(0x2BE0A63A),
  )
