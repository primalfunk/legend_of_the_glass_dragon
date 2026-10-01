package com.legendoftheglassdragon.navigation

enum class AppRoute {
    MainMenu,
    Settings,
    Credits,
    NewGameWarning,
    NameEntry,
    NameConfirmation,
    Town,
    GlassChronicle,
    DawnBrief,
    DragonWatch,
    DragonCombat,
    DragonVictoryNotice,
    FinalChronicle,
    SeasonRecord,
    Forest,
    Combat,
    Healer,
    WeaponShop,
    ArmorShop,
    Bank,
    BankAmount,
    Training,
    TrainingComplete,
    Rankings,
    NpcProfile,
    Inn,
    Gossip,
    RealmNews,
    WhoIsHere,
    ChallengeList,
    Challenge,
    Character,
    AreaPlaceholder,
    EndDay,
    SaveError,
    PlayerLayer,
    Welcome,
    Guide,
    TrophyCabinet,
}

object AppNavigation {
    fun initialRoute(): AppRoute = AppRoute.MainMenu

    /** Null delegates Back to Android, which closes the activity at the root screens. */
    fun previousRoute(route: AppRoute): AppRoute? = when (route) {
        AppRoute.MainMenu,
        AppRoute.Combat,
        AppRoute.Challenge,
        AppRoute.GlassChronicle,
        AppRoute.DawnBrief,
        AppRoute.DragonCombat,
        AppRoute.DragonVictoryNotice,
        AppRoute.FinalChronicle,
        AppRoute.PlayerLayer,
        -> null

        AppRoute.Settings,
        AppRoute.Credits,
        AppRoute.SeasonRecord,
        -> AppRoute.MainMenu

        AppRoute.NewGameWarning,
        AppRoute.NameEntry,
        AppRoute.Town,
        AppRoute.SaveError,
        -> AppRoute.MainMenu

        AppRoute.NameConfirmation -> AppRoute.NameEntry
        AppRoute.Forest,
        AppRoute.Healer,
        AppRoute.WeaponShop,
        AppRoute.ArmorShop,
        AppRoute.Bank,
        AppRoute.Training,
        AppRoute.TrainingComplete,
        AppRoute.Rankings,
        AppRoute.NpcProfile,
        AppRoute.Inn,
        AppRoute.Character,
        AppRoute.AreaPlaceholder,
        AppRoute.EndDay,
        AppRoute.DragonWatch,
        -> AppRoute.Town

        AppRoute.BankAmount -> AppRoute.Bank

        AppRoute.Guide,
        AppRoute.TrophyCabinet,
        -> AppRoute.PlayerLayer

        AppRoute.Welcome -> AppRoute.Town

        AppRoute.Gossip,
        AppRoute.RealmNews,
        AppRoute.WhoIsHere,
        AppRoute.ChallengeList,
        -> AppRoute.Inn
    }
}
