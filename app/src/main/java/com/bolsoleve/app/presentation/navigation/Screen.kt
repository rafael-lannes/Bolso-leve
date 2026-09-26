package com.bolsoleve.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Splash : Screen("splash", "Carregando")
    data object Onboarding : Screen("onboarding", "Boas-vindas")
    data object Dashboard : Screen("dashboard", "Início", Icons.Default.Home)
    data object Medicine : Screen("medicine", "Medicina", Icons.Default.Medication)
    data object Stats : Screen("stats", "Estatísticas", Icons.Default.Analytics)
    data object Options : Screen("options", "Opções", Icons.Default.Settings)

    companion object {
        val bottomNavItems = listOf(
            Dashboard,
            Medicine,
            Stats,
            Options
        )
    }
}
