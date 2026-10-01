package com.legendoftheglassdragon.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay
import com.legendoftheglassdragon.audio.AudioControls
import com.legendoftheglassdragon.data.AppPreferences
import com.legendoftheglassdragon.data.GameSaveRepository
import com.legendoftheglassdragon.data.SaveLoadResult
import com.legendoftheglassdragon.data.SaveWriteResult
import com.legendoftheglassdragon.data.TrophyRepository
import com.legendoftheglassdragon.data.TrophyStoreState
import com.legendoftheglassdragon.game.CharacterNameValidator
import com.legendoftheglassdragon.game.CommunityActions
import com.legendoftheglassdragon.game.CommunityRules
import com.legendoftheglassdragon.game.ChallengeEngine
import com.legendoftheglassdragon.game.ChallengeStatus
import com.legendoftheglassdragon.game.EconomyEngine
import com.legendoftheglassdragon.game.EconomyRules
import com.legendoftheglassdragon.game.EquipmentCatalog
import com.legendoftheglassdragon.game.CombatStatus
import com.legendoftheglassdragon.game.GameActionRejection
import com.legendoftheglassdragon.game.GameActionResult
import com.legendoftheglassdragon.game.GameBalance
import com.legendoftheglassdragon.game.GameEngine
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.DragonCombatEngine
import com.legendoftheglassdragon.game.DragonCombatStatus
import com.legendoftheglassdragon.game.DragonSeasonActions
import com.legendoftheglassdragon.game.DragonStage
import com.legendoftheglassdragon.game.SeasonConclusionPhase
import com.legendoftheglassdragon.game.SeasonNarrator
import com.legendoftheglassdragon.game.MonsterCatalog
import com.legendoftheglassdragon.game.NameValidationError
import com.legendoftheglassdragon.game.NameValidationResult
import com.legendoftheglassdragon.game.NewGameCreationResult
import com.legendoftheglassdragon.game.NewGameFactory
import com.legendoftheglassdragon.game.WorldAdvancer
import com.legendoftheglassdragon.game.TrophyStatsTracker
import com.legendoftheglassdragon.navigation.AppNavigation
import com.legendoftheglassdragon.navigation.AppRoute
import com.legendoftheglassdragon.navigation.ProductWrapperPolicy
import com.legendoftheglassdragon.ui.screens.AreaPlaceholderScreen
import com.legendoftheglassdragon.ui.screens.CharacterStatusScreen
import com.legendoftheglassdragon.ui.screens.CombatScreen
import com.legendoftheglassdragon.ui.screens.EndDayScreen
import com.legendoftheglassdragon.ui.screens.ForestScreen
import com.legendoftheglassdragon.ui.screens.HealerScreen
import com.legendoftheglassdragon.ui.screens.MainMenuScreen
import com.legendoftheglassdragon.ui.screens.GlassChronicleScreen
import com.legendoftheglassdragon.ui.screens.DawnBriefScreen
import com.legendoftheglassdragon.ui.screens.NameConfirmationScreen
import com.legendoftheglassdragon.ui.screens.NameEntryScreen
import com.legendoftheglassdragon.ui.screens.NewGameWarningScreen
import com.legendoftheglassdragon.ui.screens.NpcProfileScreen
import com.legendoftheglassdragon.ui.screens.RankingsScreen
import com.legendoftheglassdragon.ui.screens.RealmNewsScreen
import com.legendoftheglassdragon.ui.screens.SaveErrorScreen
import com.legendoftheglassdragon.ui.screens.CreditsScreen
import com.legendoftheglassdragon.ui.screens.SettingsScreen
import com.legendoftheglassdragon.ui.screens.TownScreen
import com.legendoftheglassdragon.ui.screens.TrainingCompleteScreen
import com.legendoftheglassdragon.ui.screens.TrainingScreen
import com.legendoftheglassdragon.ui.screens.GossipScreen
import com.legendoftheglassdragon.ui.screens.InnScreen
import com.legendoftheglassdragon.ui.screens.WhoIsHereScreen
import com.legendoftheglassdragon.ui.screens.WeaponShopScreen
import com.legendoftheglassdragon.ui.screens.ArmorShopScreen
import com.legendoftheglassdragon.ui.screens.BankScreen
import com.legendoftheglassdragon.ui.screens.BankAmountScreen
import com.legendoftheglassdragon.ui.screens.ChallengeListScreen
import com.legendoftheglassdragon.ui.screens.ChallengeScreen
import com.legendoftheglassdragon.ui.screens.DragonWatchScreen
import com.legendoftheglassdragon.ui.screens.DragonCombatScreen
import com.legendoftheglassdragon.ui.screens.DragonVictoryNoticeScreen
import com.legendoftheglassdragon.ui.screens.FinalChronicleScreen
import com.legendoftheglassdragon.ui.screens.SeasonRecordScreen
import com.legendoftheglassdragon.ui.screens.PlayerLayerScreen
import com.legendoftheglassdragon.ui.screens.WelcomeScreen
import com.legendoftheglassdragon.ui.screens.GuideScreen
import com.legendoftheglassdragon.ui.screens.TrophyCabinetScreen
import com.legendoftheglassdragon.ui.screens.TrophyPopup

@Composable
fun GlassDragonApp(
    appPreferences: AppPreferences,
    gameSaves: GameSaveRepository,
    completedSeasons: GameSaveRepository,
    trophyRepository: TrophyRepository,
    newGameFactory: NewGameFactory,
    audioControls: AudioControls,
    versionName: String,
    onExit: () -> Unit,
) {
    val gameEngine = remember { GameEngine() }
    val challengeEngine = remember { ChallengeEngine() }
    val dragonCombatEngine = remember { DragonCombatEngine() }
    val initialSave = remember(gameSaves) { gameSaves.load() }
    val initialCompletedSeason = remember(completedSeasons, initialSave) {
        val archived = completedSeasons.load()
        val activeCompleted = (initialSave as? SaveLoadResult.Loaded)
            ?.gameState
            ?.takeIf { it.season.stage == DragonStage.RESOLVED }
        if (archived !is SaveLoadResult.Loaded && activeCompleted != null &&
            completedSeasons.save(activeCompleted) == SaveWriteResult.Saved
        ) {
            SaveLoadResult.Loaded(activeCompleted)
        } else {
            archived
        }
    }
    var localAppState by remember { mutableStateOf(appPreferences.load()) }
    var saveState by remember { mutableStateOf(initialSave) }
    var completedSeasonState by remember { mutableStateOf(initialCompletedSeason) }
    var trophyStoreState by remember(trophyRepository) { mutableStateOf(trophyRepository.load()) }
    var gameState by remember {
        mutableStateOf((initialSave as? SaveLoadResult.Loaded)?.gameState)
    }
    var routeName by rememberSaveable {
        mutableStateOf(AppNavigation.initialRoute().name)
    }
    var enteredName by rememberSaveable { mutableStateOf("") }
    var confirmedName by rememberSaveable { mutableStateOf("") }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedArea by rememberSaveable { mutableStateOf("The Realm") }
    var selectedNpcId by rememberSaveable { mutableStateOf<String?>(null) }
    var profileReturnRoute by rememberSaveable { mutableStateOf(AppRoute.Rankings.name) }
    var welcomeBack by rememberSaveable { mutableStateOf(false) }
    var townNotice by rememberSaveable { mutableStateOf<String?>(null) }
    var forestMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var healerMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var weaponShopMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var armorShopMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var bankMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var dragonMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var depositingAtBank by rememberSaveable { mutableStateOf(true) }
    var saveErrorMessage by rememberSaveable { mutableStateOf("The adventure could not be loaded.") }
    var newGameAuthorized by rememberSaveable { mutableStateOf(false) }
    var seasonRecordFromMenu by rememberSaveable { mutableStateOf(false) }
    var newGameCommitInProgress by remember { mutableStateOf(false) }
    var playerLayerReturnRoute by rememberSaveable { mutableStateOf(AppRoute.MainMenu.name) }
    var trophyQueue by remember { mutableStateOf<List<com.legendoftheglassdragon.game.TrophyUnlock>>(emptyList()) }

    val route = AppRoute.valueOf(routeName)
    val previousRoute = if (route == AppRoute.NpcProfile) {
        AppRoute.valueOf(profileReturnRoute)
    } else {
        AppNavigation.previousRoute(route)
    }

    fun beginNameEntry(authorized: Boolean) {
        newGameAuthorized = authorized
        enteredName = ""
        confirmedName = ""
        nameError = null
        routeName = AppRoute.NameEntry.name
    }

    fun submitName() {
        when (val result = CharacterNameValidator.validate(enteredName)) {
            is NameValidationResult.Valid -> {
                confirmedName = result.name
                enteredName = result.name
                nameError = null
                routeName = AppRoute.NameConfirmation.name
            }
            is NameValidationResult.Invalid -> nameError = result.error.displayMessage()
        }
    }

    fun persistAction(
        result: GameActionResult,
        onSaved: (GameState) -> Unit,
        onRejected: (GameActionRejection) -> Unit = {},
    ) {
        when (result) {
            is GameActionResult.Applied -> {
                val trackedState = TrophyStatsTracker.observe(gameState, result.gameState)
                when (val saved = gameSaves.save(trackedState)) {
                SaveWriteResult.Saved -> {
                    gameState = trackedState
                    saveState = SaveLoadResult.Loaded(trackedState)
                    trophyQueue = trophyQueue + trophyRepository.award(trackedState)
                    trophyStoreState = trophyRepository.load()
                    if (trackedState.season.stage == DragonStage.RESOLVED) {
                        when (val archived = completedSeasons.save(trackedState)) {
                            SaveWriteResult.Saved -> {
                                completedSeasonState = SaveLoadResult.Loaded(trackedState)
                                onSaved(trackedState)
                            }
                            is SaveWriteResult.Failed -> {
                                saveErrorMessage = archived.failure.message
                                routeName = AppRoute.SaveError.name
                            }
                        }
                    } else {
                        onSaved(trackedState)
                    }
                }
                is SaveWriteResult.Failed -> {
                    saveErrorMessage = saved.failure.message
                    routeName = AppRoute.SaveError.name
                }
            }
            }
            is GameActionResult.Rejected -> onRejected(result.reason)
        }
    }

    fun returnGameplayToMainMenu() {
        val current = gameState ?: run {
            routeName = AppRoute.MainMenu.name
            return
        }
        when (val saved = gameSaves.save(current)) {
            SaveWriteResult.Saved -> {
                saveState = SaveLoadResult.Loaded(current)
                routeName = AppRoute.MainMenu.name
            }
            is SaveWriteResult.Failed -> {
                saveErrorMessage = saved.failure.message
                routeName = AppRoute.SaveError.name
            }
        }
    }

    fun returnFromCombat(expectedRevision: Long) {
        val current = gameState ?: return
        val outcome = current.activeCombat?.status
        persistAction(
            gameEngine.leaveCompletedCombat(current, expectedRevision),
            onSaved = {
                townNotice = when (outcome) {
                    CombatStatus.VICTORY -> "The forest ledger records your victory."
                    CombatStatus.ESCAPED -> "You return safely from the forest."
                    CombatStatus.DEFEAT -> "Rest now. No forest fights remain today."
                    else -> null
                }
                welcomeBack = false
                routeName = AppRoute.Town.name
            },
        )
    }

    fun advanceToDawn(expectedRevision: Long) {
        val current = gameState ?: return
        persistAction(
            CommunityActions.advanceToDawnBrief(current, expectedRevision),
            onSaved = { routeName = AppRoute.DawnBrief.name },
        )
    }

    fun completeDayTransition(expectedRevision: Long, destination: AppRoute) {
        val current = gameState ?: return
        persistAction(
            CommunityActions.completeDayTransition(current, expectedRevision),
            onSaved = { saved ->
                routeName = if (saved.season.stage == DragonStage.RESOLVED) {
                    conclusionRoute(saved).name
                } else {
                    destination.name
                }
            },
        )
    }

    BackHandler(enabled = previousRoute != null) {
        routeName = requireNotNull(previousRoute).name
    }
    BackHandler(enabled = route == AppRoute.PlayerLayer) {
        routeName = playerLayerReturnRoute
    }
    BackHandler(
        enabled = route == AppRoute.Challenge &&
            gameState?.activeChallenge?.status?.let { it != ChallengeStatus.ACTIVE } == true,
    ) {
        val current = gameState ?: return@BackHandler
        persistAction(
            challengeEngine.leave(current, current.revision),
            onSaved = { routeName = AppRoute.Inn.name },
        )
    }

    when (route) {
        AppRoute.MainMenu -> MainMenuScreen(
            menuState = ProductWrapperPolicy.mainMenuState(
                activeSave = saveState,
                completedSeason = completedSeasonState,
            ),
            activeCharacterName = (saveState as? SaveLoadResult.Loaded)
                ?.gameState
                ?.player
                ?.name,
            activeDay = (saveState as? SaveLoadResult.Loaded)?.gameState?.currentDay,
            completedVictor = ProductWrapperPolicy.seasonRecordState(
                activeSave = saveState,
                completedSeason = completedSeasonState,
            )?.let(SeasonNarrator::victorName),
            versionName = versionName,
            onNewGame = {
                val menuState = ProductWrapperPolicy.mainMenuState(
                    activeSave = saveState,
                    completedSeason = completedSeasonState,
                )
                if (menuState.newGameNeedsConfirmation) {
                    newGameAuthorized = false
                    routeName = AppRoute.NewGameWarning.name
                } else {
                    beginNameEntry(authorized = true)
                }
            },
            onContinue = {
                when (val loaded = gameSaves.load()) {
                    is SaveLoadResult.Loaded -> {
                        saveState = loaded
                        gameState = loaded.gameState
                        welcomeBack = true
                        townNotice = null
                        routeName = if (!loaded.gameState.welcomeShown) {
                            AppRoute.Welcome.name
                        } else {
                            ProductWrapperPolicy.resumeRoute(loaded.gameState).name
                        }
                    }
                    is SaveLoadResult.Invalid -> {
                        saveState = loaded
                        saveErrorMessage = loaded.failure.message
                        routeName = AppRoute.SaveError.name
                    }
                    SaveLoadResult.Missing -> saveState = SaveLoadResult.Missing
                }
            },
            onSeasonRecord = {
                seasonRecordFromMenu = true
                routeName = AppRoute.SeasonRecord.name
            },
            onSettings = { routeName = AppRoute.Settings.name },
            onPlayerLayer = {
                playerLayerReturnRoute = AppRoute.MainMenu.name
                routeName = AppRoute.PlayerLayer.name
            },
            onCredits = { routeName = AppRoute.Credits.name },
            onExit = onExit,
        )

        AppRoute.PlayerLayer -> PlayerLayerScreen(
            characterName = gameState?.player?.name,
            day = gameState?.currentDay,
            trophies = trophyStoreState,
            onContinue = { routeName = playerLayerReturnRoute },
            onGuide = { routeName = AppRoute.Guide.name },
            onTrophies = { routeName = AppRoute.TrophyCabinet.name },
        )

        AppRoute.Guide -> GuideScreen(onClose = { routeName = AppRoute.PlayerLayer.name })

        AppRoute.TrophyCabinet -> TrophyCabinetScreen(
            gameState = gameState,
            store = trophyStoreState,
            onClose = { routeName = AppRoute.PlayerLayer.name },
        )

        AppRoute.Welcome -> WelcomeScreen(
            onDismiss = {
                val current = gameState ?: return@WelcomeScreen
                val dismissed = current.copy(welcomeShown = true)
                when (gameSaves.save(dismissed)) {
                    SaveWriteResult.Saved -> {
                        gameState = dismissed
                        saveState = SaveLoadResult.Loaded(dismissed)
                        routeName = AppRoute.Town.name
                    }
                    is SaveWriteResult.Failed -> {
                        saveErrorMessage = "The welcome screen could not be saved."
                        routeName = AppRoute.SaveError.name
                    }
                }
            },
        )

        AppRoute.Settings -> SettingsScreen(
            musicEnabled = localAppState.musicEnabled,
            clickSoundEnabled = localAppState.clickSoundEnabled,
            onToggleMusic = {
                val enabled = !localAppState.musicEnabled
                appPreferences.setMusicEnabled(enabled)
                localAppState = localAppState.copy(musicEnabled = enabled)
                audioControls.setMusicEnabled(enabled)
            },
            onToggleClickSound = {
                val enabled = !localAppState.clickSoundEnabled
                appPreferences.setClickSoundEnabled(enabled)
                localAppState = localAppState.copy(clickSoundEnabled = enabled)
                audioControls.setClickSoundEnabled(enabled)
            },
            onReturn = { routeName = AppRoute.MainMenu.name },
        )

        AppRoute.Credits -> CreditsScreen(
            versionName = versionName,
            onReturn = { routeName = AppRoute.MainMenu.name },
        )

        AppRoute.NewGameWarning -> NewGameWarningScreen(
            existingCharacterName = (saveState as? SaveLoadResult.Loaded)?.gameState?.player?.name,
            saveIsBroken = saveState is SaveLoadResult.Invalid,
            onStartOver = {
                beginNameEntry(authorized = true)
            },
            onReturn = { routeName = AppRoute.MainMenu.name },
        )

        AppRoute.NameEntry -> NameEntryScreen(
            name = enteredName,
            errorMessage = nameError,
            onNameChange = {
                enteredName = it
                nameError = null
            },
            onSubmit = ::submitName,
            onReturn = {
                newGameAuthorized = false
                routeName = AppRoute.MainMenu.name
            },
        )

        AppRoute.NameConfirmation -> NameConfirmationScreen(
            characterName = confirmedName,
            onEnterRealm = {
                if (!newGameAuthorized || newGameCommitInProgress) {
                    return@NameConfirmationScreen
                }
                newGameCommitInProgress = true
                when (val creation = newGameFactory.create(confirmedName)) {
                    is NewGameCreationResult.InvalidName -> {
                        newGameCommitInProgress = false
                        nameError = creation.error.displayMessage()
                        routeName = AppRoute.NameEntry.name
                    }
                    is NewGameCreationResult.Created -> {
                        when (val saved = gameSaves.save(creation.gameState)) {
                            SaveWriteResult.Saved -> {
                                newGameCommitInProgress = false
                                newGameAuthorized = false
                                gameState = creation.gameState
                                saveState = SaveLoadResult.Loaded(creation.gameState)
                                welcomeBack = false
                                townNotice = "A new adventure begins."
                                routeName = AppRoute.Welcome.name
                            }
                            is SaveWriteResult.Failed -> {
                                newGameCommitInProgress = false
                                saveErrorMessage = saved.failure.message
                                routeName = AppRoute.SaveError.name
                            }
                        }
                    }
                }
            },
            onChangeName = { routeName = AppRoute.NameEntry.name },
        )

        AppRoute.Town -> GameStateScreen(gameState, onMissing = {
            saveErrorMessage = "No lawful character is available for Town."
            routeName = AppRoute.SaveError.name
        }) { game ->
            TownScreen(
                gameState = game,
                welcomeBack = welcomeBack,
                townNotice = townNotice,
                onForest = {
                    forestMessage = null
                    routeName = if (game.activeCombat == null) {
                        AppRoute.Forest.name
                    } else {
                        AppRoute.Combat.name
                    }
                },
                onHealer = {
                    healerMessage = null
                    routeName = AppRoute.Healer.name
                },
                onWeaponShop = {
                    weaponShopMessage = null
                    routeName = AppRoute.WeaponShop.name
                },
                onArmorShop = {
                    armorShopMessage = null
                    routeName = AppRoute.ArmorShop.name
                },
                onBank = {
                    bankMessage = null
                    routeName = AppRoute.Bank.name
                },
                onTraining = { routeName = AppRoute.Training.name },
                onInn = {
                    routeName = AppRoute.Inn.name
                },
                onRankings = {
                    routeName = AppRoute.Rankings.name
                },
                onCharacter = { routeName = AppRoute.Character.name },
                onDragonWatch = {
                    dragonMessage = null
                    routeName = AppRoute.DragonWatch.name
                },
                onEndDay = { routeName = AppRoute.EndDay.name },
                onPlayerLayer = {
                    playerLayerReturnRoute = AppRoute.Town.name
                    routeName = AppRoute.PlayerLayer.name
                },
                onMainMenu = ::returnGameplayToMainMenu,
            )
        }

        AppRoute.GlassChronicle -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            GlassChronicleScreen(
                gameState = game,
                onContinueToDawn = { expectedRevision ->
                    advanceToDawn(expectedRevision)
                },
                onSkipToDawn = { expectedRevision ->
                    advanceToDawn(expectedRevision)
                },
                onSkipToTown = { expectedRevision ->
                    completeDayTransition(expectedRevision, AppRoute.Town)
                },
            )
        }

        AppRoute.DawnBrief -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            DawnBriefScreen(
                gameState = game,
                onEnterTown = { expectedRevision ->
                    completeDayTransition(expectedRevision, AppRoute.Town)
                },
                onReadPublicNews = { expectedRevision ->
                    completeDayTransition(expectedRevision, AppRoute.RealmNews)
                },
            )
        }

        AppRoute.DragonWatch -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            DragonWatchScreen(
                gameState = game,
                message = dragonMessage,
                onChallengeDragon = { expectedRevision ->
                    val current = gameState ?: return@DragonWatchScreen
                    persistAction(
                        dragonCombatEngine.begin(current, expectedRevision),
                        onSaved = {
                            dragonMessage = null
                            routeName = AppRoute.DragonCombat.name
                        },
                        onRejected = {
                            dragonMessage = "You cannot begin that challenge yet."
                        },
                    )
                },
                onSupportCandidate = { candidateId, expectedRevision ->
                    val current = gameState ?: return@DragonWatchScreen
                    persistAction(
                        DragonSeasonActions.supportCandidate(
                            current,
                            expectedRevision,
                            candidateId,
                        ),
                        onSaved = { saved ->
                            val name = saved.community.adventurers
                                .firstOrNull { it.id == candidateId }?.name ?: "the challenger"
                            dragonMessage = "Your support for " + name + " is entered in the ledger."
                        },
                        onRejected = {
                            dragonMessage = "That support cannot be given now."
                        },
                    )
                },
                onReturnToTown = { routeName = AppRoute.Town.name },
            )
        }

        AppRoute.DragonCombat -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            DragonCombatScreen(
                gameState = game,
                onAttack = { expectedRevision ->
                    val current = gameState ?: return@DragonCombatScreen
                    persistAction(
                        dragonCombatEngine.attack(current, expectedRevision),
                        onSaved = {},
                    )
                },
                onRetreat = { expectedRevision ->
                    val current = gameState ?: return@DragonCombatScreen
                    persistAction(
                        dragonCombatEngine.retreat(current, expectedRevision),
                        onSaved = {},
                    )
                },
                onLeave = { expectedRevision ->
                    val current = gameState ?: return@DragonCombatScreen
                    val outcome = current.activeDragonCombat?.status
                    persistAction(
                        dragonCombatEngine.leave(current, expectedRevision),
                        onSaved = { saved ->
                            routeName = if (outcome == DragonCombatStatus.PLAYER_VICTORY) {
                                conclusionRoute(saved).name
                            } else {
                                AppRoute.Town.name
                            }
                        },
                    )
                },
            )
        }

        AppRoute.DragonVictoryNotice -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            DragonVictoryNoticeScreen(game) { expectedRevision ->
                val current = gameState ?: return@DragonVictoryNoticeScreen
                persistAction(
                    DragonSeasonActions.advanceConclusion(current, expectedRevision),
                    onSaved = { routeName = AppRoute.FinalChronicle.name },
                )
            }
        }

        AppRoute.FinalChronicle -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            FinalChronicleScreen(game) { expectedRevision ->
                val current = gameState ?: return@FinalChronicleScreen
                persistAction(
                    DragonSeasonActions.advanceConclusion(current, expectedRevision),
                    onSaved = {
                        seasonRecordFromMenu = false
                        routeName = AppRoute.SeasonRecord.name
                    },
                )
            }
        }

        AppRoute.SeasonRecord -> {
            val record = if (seasonRecordFromMenu) {
                ProductWrapperPolicy.seasonRecordState(saveState, completedSeasonState)
            } else {
                gameState?.takeIf { it.season.stage == DragonStage.RESOLVED }
                    ?: ProductWrapperPolicy.seasonRecordState(saveState, completedSeasonState)
            }
            GameStateScreen(record, onMissing = {
                routeName = AppRoute.MainMenu.name
            }) { game ->
                SeasonRecordScreen(game) { expectedRevision ->
                    if (seasonRecordFromMenu) {
                        seasonRecordFromMenu = false
                        routeName = AppRoute.MainMenu.name
                    } else {
                        val current = gameState ?: return@SeasonRecordScreen
                        if (current.season.conclusionPhase == SeasonConclusionPhase.COMPLETE) {
                            routeName = AppRoute.MainMenu.name
                        } else {
                            persistAction(
                                DragonSeasonActions.advanceConclusion(current, expectedRevision),
                                onSaved = { routeName = AppRoute.MainMenu.name },
                            )
                        }
                    }
                }
            }
        }

        AppRoute.Rankings -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            RankingsScreen(
                gameState = game,
                onInspectNpc = { npcId ->
                    selectedNpcId = npcId
                    profileReturnRoute = AppRoute.Rankings.name
                    routeName = AppRoute.NpcProfile.name
                },
                onReturnToTown = { routeName = AppRoute.Town.name },
            )
        }

        AppRoute.NpcProfile -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            val npc = game.community.adventurers.firstOrNull { it.id == selectedNpcId }
            if (npc == null) {
                SaveErrorScreen(
                    message = "That adventurer is not present in the local ledger.",
                    onReturn = { routeName = profileReturnRoute },
                    onDeleteBrokenSave = { routeName = profileReturnRoute },
                )
            } else {
                NpcProfileScreen(
                    gameState = game,
                    npc = npc,
                    rank = CommunityRules.rankOf(game, npc.id),
                    onChallenge = { npcId, expectedRevision ->
                        val current = gameState ?: return@NpcProfileScreen
                        persistAction(
                            challengeEngine.begin(current, expectedRevision, npcId),
                            onSaved = {
                                selectedNpcId = npcId
                                routeName = AppRoute.Challenge.name
                            },
                        )
                    },
                    onReturn = { routeName = profileReturnRoute },
                )
            }
        }

        AppRoute.Inn -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            InnScreen(
                gameState = game,
                onGossip = { routeName = AppRoute.Gossip.name },
                onNews = { routeName = AppRoute.RealmNews.name },
                onWhoIsHere = { expectedRevision ->
                    val current = gameState ?: return@InnScreen
                    val visitors = CommunityRules.innVisitors(current).map { it.id }.toSet()
                    persistAction(
                        CommunityActions.recordInnMeeting(current, expectedRevision, visitors),
                        onSaved = { routeName = AppRoute.WhoIsHere.name },
                        onRejected = { rejection ->
                            if (rejection == GameActionRejection.NoCommunityChange) {
                                routeName = AppRoute.WhoIsHere.name
                            }
                        },
                    )
                },
                onTalk = { expectedRevision ->
                    val current = gameState ?: return@InnScreen
                    val visitors = CommunityRules.innVisitors(current).map { it.id }.toSet()
                    persistAction(
                        CommunityActions.recordInnMeeting(current, expectedRevision, visitors),
                        onSaved = { routeName = AppRoute.WhoIsHere.name },
                        onRejected = { rejection ->
                            if (rejection == GameActionRejection.NoCommunityChange) {
                                routeName = AppRoute.WhoIsHere.name
                            }
                        },
                    )
                },
                onChallenge = { routeName = AppRoute.ChallengeList.name },
                onReturnToTown = { routeName = AppRoute.Town.name },
            )
        }

        AppRoute.ChallengeList -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            ChallengeListScreen(
                gameState = game,
                onChallenge = { npcId, expectedRevision ->
                    val current = gameState ?: return@ChallengeListScreen
                    persistAction(
                        challengeEngine.begin(current, expectedRevision, npcId),
                        onSaved = {
                            selectedNpcId = npcId
                            routeName = AppRoute.Challenge.name
                        },
                    )
                },
                onReturnToInn = { routeName = AppRoute.Inn.name },
            )
        }

        AppRoute.Challenge -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            val challenge = game.activeChallenge
            val opponent = challenge?.let { active ->
                game.community.adventurers.firstOrNull { it.id == active.opponentId }
            }
            if (challenge == null || opponent == null) {
                SaveErrorScreen(
                    message = "No lawful adventurer challenge is available.",
                    onReturn = { routeName = AppRoute.Inn.name },
                    onDeleteBrokenSave = { routeName = AppRoute.Inn.name },
                )
            } else {
                ChallengeScreen(
                    gameState = game,
                    challenge = challenge,
                    opponent = opponent,
                    onAttack = { expectedRevision ->
                        val current = gameState ?: return@ChallengeScreen
                        persistAction(
                            challengeEngine.attack(current, expectedRevision),
                            onSaved = { routeName = AppRoute.Challenge.name },
                        )
                    },
                    onYield = { expectedRevision ->
                        val current = gameState ?: return@ChallengeScreen
                        persistAction(
                            challengeEngine.yield(current, expectedRevision),
                            onSaved = { routeName = AppRoute.Challenge.name },
                        )
                    },
                    onReturnToInn = { expectedRevision ->
                        val current = gameState ?: return@ChallengeScreen
                        persistAction(
                            challengeEngine.leave(current, expectedRevision),
                            onSaved = { routeName = AppRoute.Inn.name },
                        )
                    },
                )
            }
        }

        AppRoute.Gossip -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            GossipScreen(
                gameState = game,
                onReturnToInn = { routeName = AppRoute.Inn.name },
            )
        }

        AppRoute.RealmNews -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            RealmNewsScreen(
                gameState = game,
                onReturnToInn = { routeName = AppRoute.Inn.name },
            )
        }

        AppRoute.WhoIsHere -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            WhoIsHereScreen(
                gameState = game,
                onInspectNpc = { npcId ->
                    selectedNpcId = npcId
                    profileReturnRoute = AppRoute.WhoIsHere.name
                    routeName = AppRoute.NpcProfile.name
                },
                onReturnToInn = { routeName = AppRoute.Inn.name },
            )
        }

        AppRoute.Forest -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            ForestScreen(
                gameState = game,
                message = forestMessage,
                onSearch = { expectedRevision ->
                    val current = gameState ?: return@ForestScreen
                    persistAction(
                        gameEngine.startForestEncounter(current, expectedRevision),
                        onSaved = {
                            forestMessage = null
                            routeName = AppRoute.Combat.name
                        },
                        onRejected = { rejection ->
                            forestMessage = when (rejection) {
                                GameActionRejection.NoForestFights -> "No forest fights remain today."
                                GameActionRejection.ActiveCombat -> "The current encounter must be resolved."
                                else -> forestMessage
                            }
                        },
                    )
                },
                onReturnToTown = { routeName = AppRoute.Town.name },
            )
        }

        AppRoute.Combat -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            val combat = game.activeCombat
            if (combat == null) {
                SaveErrorScreen(
                    message = "No active encounter is available.",
                    onReturn = { routeName = AppRoute.Town.name },
                    onDeleteBrokenSave = { routeName = AppRoute.Town.name },
                )
            } else {
                CombatScreen(
                    gameState = game,
                    combat = combat,
                    monster = MonsterCatalog.require(combat.monsterId),
                    onAttack = { expectedRevision ->
                        val current = gameState ?: return@CombatScreen
                        persistAction(
                            gameEngine.attack(current, expectedRevision),
                            onSaved = { routeName = AppRoute.Combat.name },
                        )
                    },
                    onRun = { expectedRevision ->
                        val current = gameState ?: return@CombatScreen
                        persistAction(
                            gameEngine.runAway(current, expectedRevision),
                            onSaved = { routeName = AppRoute.Combat.name },
                        )
                    },
                    onSearchAgain = { expectedRevision ->
                        val current = gameState ?: return@CombatScreen
                        persistAction(
                            gameEngine.searchAgain(current, expectedRevision),
                            onSaved = { routeName = AppRoute.Combat.name },
                        )
                    },
                    onReturnToTown = ::returnFromCombat,
                )
            }
        }

        AppRoute.WeaponShop -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            WeaponShopScreen(
                gameState = game,
                message = weaponShopMessage,
                onBuy = { weaponId, expectedRevision ->
                    val current = gameState ?: return@WeaponShopScreen
                    val weapon = EquipmentCatalog.weapon(weaponId) ?: return@WeaponShopScreen
                    val cost = EconomyRules.weaponUpgradeCost(current.player, weapon)
                    persistAction(
                        EconomyEngine.buyWeapon(current, expectedRevision, weaponId),
                        onSaved = { updated ->
                            weaponShopMessage = "The smith takes " + cost + " gold. " +
                                "You now wield " + updated.player.weapon + ". Attack: " + updated.player.attack + "."
                            routeName = AppRoute.WeaponShop.name
                        },
                        onRejected = { rejection ->
                            weaponShopMessage = economyRejectionMessage(rejection)
                        },
                    )
                },
                onReturnToTown = { routeName = AppRoute.Town.name },
            )
        }

        AppRoute.ArmorShop -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            ArmorShopScreen(
                gameState = game,
                message = armorShopMessage,
                onBuy = { armorId, expectedRevision ->
                    val current = gameState ?: return@ArmorShopScreen
                    val armor = EquipmentCatalog.armor(armorId) ?: return@ArmorShopScreen
                    val cost = EconomyRules.armorUpgradeCost(current.player, armor)
                    persistAction(
                        EconomyEngine.buyArmor(current, expectedRevision, armorId),
                        onSaved = { updated ->
                            armorShopMessage = "The armorer takes " + cost + " gold. " +
                                "You now wear " + updated.player.armor + ". Defense: " + updated.player.defense + "."
                            routeName = AppRoute.ArmorShop.name
                        },
                        onRejected = { rejection ->
                            armorShopMessage = economyRejectionMessage(rejection)
                        },
                    )
                },
                onReturnToTown = { routeName = AppRoute.Town.name },
            )
        }

        AppRoute.Bank -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            BankScreen(
                gameState = game,
                message = bankMessage,
                onDeposit = {
                    depositingAtBank = true
                    routeName = AppRoute.BankAmount.name
                },
                onWithdraw = {
                    depositingAtBank = false
                    routeName = AppRoute.BankAmount.name
                },
                onDepositAll = { expectedRevision ->
                    val current = gameState ?: return@BankScreen
                    val amount = current.player.gold
                    persistAction(
                        EconomyEngine.deposit(current, expectedRevision, amount),
                        onSaved = {
                            bankMessage = "Deposited " + amount + " gold. The vault is sealed."
                            routeName = AppRoute.Bank.name
                        },
                        onRejected = { bankMessage = economyRejectionMessage(it) },
                    )
                },
                onWithdrawAll = { expectedRevision ->
                    val current = gameState ?: return@BankScreen
                    val amount = current.player.bankedGold
                    persistAction(
                        EconomyEngine.withdraw(current, expectedRevision, amount),
                        onSaved = {
                            bankMessage = "Withdrew " + amount + " gold."
                            routeName = AppRoute.Bank.name
                        },
                        onRejected = { bankMessage = economyRejectionMessage(it) },
                    )
                },
                onReturnToTown = { routeName = AppRoute.Town.name },
            )
        }

        AppRoute.BankAmount -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            BankAmountScreen(
                gameState = game,
                depositing = depositingAtBank,
                onChoose = { amount, expectedRevision ->
                    val current = gameState ?: return@BankAmountScreen
                    val action = if (depositingAtBank) {
                        EconomyEngine.deposit(current, expectedRevision, amount)
                    } else {
                        EconomyEngine.withdraw(current, expectedRevision, amount)
                    }
                    persistAction(
                        action,
                        onSaved = {
                            bankMessage = (if (depositingAtBank) "Deposited " else "Withdrew ") +
                                amount + " gold."
                            routeName = AppRoute.Bank.name
                        },
                        onRejected = {
                            bankMessage = economyRejectionMessage(it)
                            routeName = AppRoute.Bank.name
                        },
                    )
                },
                onCancel = { routeName = AppRoute.Bank.name },
            )
        }

        AppRoute.Healer -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            HealerScreen(
                gameState = game,
                message = healerMessage,
                onHeal = { expectedRevision ->
                    val current = gameState ?: return@HealerScreen
                    val missingHp = current.player.maximumHp - current.player.currentHp
                    val cost = missingHp * GameBalance.HEALING_GOLD_PER_HP
                    persistAction(
                        gameEngine.healFully(current, expectedRevision),
                        onSaved = {
                            healerMessage = "Your wounds are mended for " + cost + " gold."
                            routeName = AppRoute.Healer.name
                        },
                        onRejected = { rejection ->
                            healerMessage = when (rejection) {
                                GameActionRejection.NoHealingNeeded -> "You are already at full health."
                                is GameActionRejection.NotEnoughGold ->
                                    "The healer requires " + rejection.required + " gold."
                                else -> healerMessage
                            }
                        },
                    )
                },
                onReturnToTown = { routeName = AppRoute.Town.name },
            )
        }

        AppRoute.Training -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            TrainingScreen(
                gameState = game,
                onTrain = { expectedRevision ->
                    val current = gameState ?: return@TrainingScreen
                    persistAction(
                        gameEngine.train(current, expectedRevision),
                        onSaved = {
                            townNotice = "Your training trial is recorded in the village ledger."
                            routeName = AppRoute.TrainingComplete.name
                        },
                    )
                },
                onReturnToTown = { routeName = AppRoute.Town.name },
            )
        }

        AppRoute.TrainingComplete -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            TrainingCompleteScreen(
                gameState = game,
                onReturnToTown = { routeName = AppRoute.Town.name },
            )
        }

        AppRoute.Character -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            CharacterStatusScreen(
                gameState = game,
                onReturnToTown = { routeName = AppRoute.Town.name },
            )
        }

        AppRoute.AreaPlaceholder -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            AreaPlaceholderScreen(
                areaName = selectedArea,
                gameState = game,
                onReturnToTown = { routeName = AppRoute.Town.name },
            )
        }

        AppRoute.EndDay -> GameStateScreen(gameState, onMissing = {
            routeName = AppRoute.MainMenu.name
        }) { game ->
            EndDayScreen(
                gameState = game,
                onConfirm = { expectedRevision ->
                    val current = gameState ?: return@EndDayScreen
                    persistAction(
                        WorldAdvancer.advanceWorldToNextDay(current, expectedRevision),
                        onSaved = { next ->
                            welcomeBack = false
                            townNotice = "The realm advanced while you slept."
                            routeName = AppRoute.GlassChronicle.name
                        },
                    )
                },
                onReturnToTown = { routeName = AppRoute.Town.name },
            )
        }

        AppRoute.SaveError -> SaveErrorScreen(
            message = saveErrorMessage,
            onReturn = {
                saveState = gameSaves.load()
                routeName = AppRoute.MainMenu.name
            },
            onDeleteBrokenSave = {
                if (gameSaves.delete()) {
                    saveState = SaveLoadResult.Missing
                    gameState = null
                    routeName = AppRoute.MainMenu.name
                } else {
                    saveErrorMessage = "The broken save could not be deleted."
                }
            },
        )

    }
    trophyQueue.firstOrNull()?.let { unlock ->
        LaunchedEffect(unlock.trophy.id) {
            delay(3_500)
            trophyQueue = trophyQueue.drop(1)
        }
        Popup(onDismissRequest = { trophyQueue = trophyQueue.drop(1) }) {
            TrophyPopup(unlock = unlock, onDismiss = { trophyQueue = trophyQueue.drop(1) })
        }
    }
}

@Composable
private fun GameStateScreen(
    gameState: GameState?,
    onMissing: () -> Unit,
    content: @Composable (GameState) -> Unit,
) {
    if (gameState == null) {
        SaveErrorScreen(
            message = "The active adventure is no longer available.",
            onReturn = onMissing,
            onDeleteBrokenSave = onMissing,
        )
    } else {
        content(gameState)
    }
}

private fun conclusionRoute(state: GameState): AppRoute = when (state.season.conclusionPhase) {
    SeasonConclusionPhase.VICTORY_NOTICE -> AppRoute.DragonVictoryNotice
    SeasonConclusionPhase.FINAL_CHRONICLE -> AppRoute.FinalChronicle
    SeasonConclusionPhase.SEASON_RECORD,
    SeasonConclusionPhase.COMPLETE,
    null,
    -> AppRoute.SeasonRecord
}

private fun NameValidationError.displayMessage(): String = when (this) {
    NameValidationError.Empty -> "A name must be entered."
    NameValidationError.TooLong -> "Name must be 24 characters or fewer."
}

private fun economyRejectionMessage(rejection: GameActionRejection): String = when (rejection) {
    is GameActionRejection.NotEnoughCarriedGold ->
        "You need ${rejection.required} gold on hand."
    is GameActionRejection.NotEnoughBankedGold ->
        "The vault contains less than ${rejection.required} gold."
    is GameActionRejection.EquipmentLevelRequired ->
        "The smith will reserve that for Level ${rejection.level} adventurers."
    GameActionRejection.EquipmentNotUpgrade ->
        "That would not improve your current equipment."
    GameActionRejection.UnknownEquipment ->
        "That item is not recorded in the merchant's ledger."
    GameActionRejection.InvalidGoldAmount ->
        "Choose a lawful amount of gold."
    GameActionRejection.ActiveCombat,
    GameActionRejection.ActiveChallenge ->
        "Finish your present contest first."
    GameActionRejection.StaleState ->
        "The ledger changed before the transaction completed. Try again."
    else -> "The transaction could not be completed."
}
