package com.kodraliu.localrock.ui.navigation

import kotlinx.serialization.Serializable

/** A screen the app can open on once the splash is done, see [firstScreen]. */
sealed interface FirstScreen

// Top-level destinations
@Serializable data object Splash
@Serializable data object Welcome : FirstScreen
@Serializable data object ServerSetup : FirstScreen
@Serializable data object Login : FirstScreen
@Serializable data object DeviceList : FirstScreen
@Serializable data object AppSettings
@Serializable data class AddVacuum(val duid: String? = null)

@Serializable data class VacuumGraph(val duid: String)
@Serializable data object VacuumMain
@Serializable data object VacuumSettings
@Serializable data object VacuumZoneClean
@Serializable data object VacuumMapEdit
@Serializable data object VacuumSchedule
@Serializable data object VacuumCamera
@Serializable data object VacuumRemote
@Serializable data object VacuumPinGo
@Serializable data object VacuumHistory
