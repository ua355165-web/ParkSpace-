package com.example.core.config

/**
 * Explicit runtime boundary separating Demo Mode from future Production Mode.
 * Firebase and live payment/cloud services remain disconnected until Milestone 8.
 */
enum class AppMode(val displayLabel: String) {
  DEMO("Demo Sandbox • Local Testing Only (Not Production Auth)"),
  PRODUCTION_UNCONFIGURED("Production Mode (Not Yet Connected)"),
}

/**
 * Supported parking booking models for ParkSpace.
 */
enum class BookingType(val displayName: String) {
  HOURLY("Hourly"),
  DAILY("Daily"),
  MONTHLY("Monthly"),
}

/**
 * Strict launch city scope for ParkSpace Pakistan.
 */
enum class SupportedCity(
  val displayName: String,
  val provinceLabel: String,
  val centerLatitude: Double,
  val centerLongitude: Double,
  val defaultZoom: Double = 12.0,
) {
  LAHORE(
    displayName = "Lahore",
    provinceLabel = "Punjab",
    centerLatitude = 31.5204,
    centerLongitude = 74.3587,
  ),
  ISLAMABAD(
    displayName = "Islamabad",
    provinceLabel = "ICT",
    centerLatitude = 33.6844,
    centerLongitude = 73.0479,
  ),
  FAISALABAD(
    displayName = "Faisalabad",
    provinceLabel = "Punjab",
    centerLatitude = 31.4504,
    centerLongitude = 73.1350,
  ),
  KARACHI(
    displayName = "Karachi",
    provinceLabel = "Sindh",
    centerLatitude = 24.8607,
    centerLongitude = 67.0011,
  ),
}

/**
 * Platform user roles.
 * SUPER_ADMIN has no public self-registration path in production or demo.
 */
enum class UserRole(
  val displayName: String,
  val allowsPublicRegistration: Boolean,
) {
  CUSTOMER(displayName = "Customer", allowsPublicRegistration = true),
  HOST(displayName = "Host", allowsPublicRegistration = true),
  SUPER_ADMIN(displayName = "Private Super Admin", allowsPublicRegistration = false),
}

object AppEnvironment {
  val currentMode: AppMode = AppMode.DEMO
  const val CURRENCY_CODE: String = "PKR"
  const val CURRENCY_SYMBOL: String = "Rs."
  const val MAP_STYLE_URL: String = "https://tiles.openfreemap.org/styles/liberty"
  const val APPLICATION_ID: String = "pk.parkspace.marketplace"
  const val PRE_BOOKING_LOCATION_MASK_RADIUS_METERS: Int = 200
}
