package com.legendoftheglassdragon.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppNavigationTest {
    @Test
    fun everyLaunchStartsAtProductMainMenu() {
        assertEquals(AppRoute.MainMenu, AppNavigation.initialRoute())
    }

    @Test
    fun gameplayDestinationsBackReturnToTownOrInnLawfully() {
        assertEquals(AppRoute.Town, AppNavigation.previousRoute(AppRoute.AreaPlaceholder))
        assertEquals(AppRoute.Town, AppNavigation.previousRoute(AppRoute.Character))
        assertEquals(AppRoute.Town, AppNavigation.previousRoute(AppRoute.EndDay))
        assertEquals(AppRoute.Town, AppNavigation.previousRoute(AppRoute.Forest))
        assertEquals(AppRoute.Town, AppNavigation.previousRoute(AppRoute.Healer))
        assertEquals(AppRoute.Town, AppNavigation.previousRoute(AppRoute.WeaponShop))
        assertEquals(AppRoute.Town, AppNavigation.previousRoute(AppRoute.ArmorShop))
        assertEquals(AppRoute.Town, AppNavigation.previousRoute(AppRoute.Bank))
        assertEquals(AppRoute.Bank, AppNavigation.previousRoute(AppRoute.BankAmount))
        assertEquals(AppRoute.Town, AppNavigation.previousRoute(AppRoute.Training))
        assertEquals(AppRoute.Town, AppNavigation.previousRoute(AppRoute.Rankings))
        assertEquals(AppRoute.Town, AppNavigation.previousRoute(AppRoute.Inn))
        assertEquals(AppRoute.Inn, AppNavigation.previousRoute(AppRoute.Gossip))
        assertEquals(AppRoute.Inn, AppNavigation.previousRoute(AppRoute.RealmNews))
        assertEquals(AppRoute.Inn, AppNavigation.previousRoute(AppRoute.WhoIsHere))
    }

    @Test
    fun wrapperScreensReturnToExpectedSafeLocation() {
        assertEquals(AppRoute.MainMenu, AppNavigation.previousRoute(AppRoute.Settings))
        assertEquals(AppRoute.MainMenu, AppNavigation.previousRoute(AppRoute.Credits))
        assertEquals(AppRoute.MainMenu, AppNavigation.previousRoute(AppRoute.SeasonRecord))
        assertEquals(AppRoute.MainMenu, AppNavigation.previousRoute(AppRoute.NewGameWarning))
        assertEquals(AppRoute.MainMenu, AppNavigation.previousRoute(AppRoute.NameEntry))
        assertEquals(AppRoute.NameEntry, AppNavigation.previousRoute(AppRoute.NameConfirmation))
    }

    @Test
    fun mainMenuBackIsDelegatedToAndroidAndAtomicActivityScreensTrapBack() {
        assertNull(AppNavigation.previousRoute(AppRoute.MainMenu))
        assertNull(AppNavigation.previousRoute(AppRoute.Combat))
        assertNull(AppNavigation.previousRoute(AppRoute.Challenge))
        assertNull(AppNavigation.previousRoute(AppRoute.GlassChronicle))
        assertNull(AppNavigation.previousRoute(AppRoute.DawnBrief))
        assertNull(AppNavigation.previousRoute(AppRoute.DragonCombat))
    }

    @Test
    fun productionRoutesContainNoStartupOrDeveloperDestinations() {
        val names = AppRoute.entries.map { it.name.lowercase() }
        assertEquals(false, names.any { "debug" in it || "seed" in it || "startup" in it })
    }
}
