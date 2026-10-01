package com.legendoftheglassdragon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legendoftheglassdragon.game.CharacterNameValidator
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.ProgressionRules
import com.legendoftheglassdragon.game.CommunityRules
import com.legendoftheglassdragon.game.EquipmentCatalog
import com.legendoftheglassdragon.game.DragonStage
import com.legendoftheglassdragon.navigation.CampaignSlotState
import com.legendoftheglassdragon.navigation.MainMenuState
import com.legendoftheglassdragon.ui.components.AnsiSpan
import com.legendoftheglassdragon.ui.components.AnsiText
import com.legendoftheglassdragon.ui.components.CrystalDivider
import com.legendoftheglassdragon.ui.components.GlassDragonCrest
import com.legendoftheglassdragon.ui.components.GlassDragonTitleBanner
import com.legendoftheglassdragon.ui.components.LocationArt
import com.legendoftheglassdragon.ui.components.LocationPlate
import com.legendoftheglassdragon.ui.components.TerminalBox
import com.legendoftheglassdragon.ui.components.TerminalFrameTone
import com.legendoftheglassdragon.ui.components.TerminalGap
import com.legendoftheglassdragon.ui.components.TerminalMenuRow
import com.legendoftheglassdragon.ui.components.TerminalRule
import com.legendoftheglassdragon.ui.components.TerminalSurface
import com.legendoftheglassdragon.ui.components.TerminalText
import com.legendoftheglassdragon.ui.theme.AnsiColor
import kotlinx.coroutines.delay

@Composable
fun MainMenuScreen(
    menuState: MainMenuState,
    activeCharacterName: String?,
    activeDay: Int?,
    completedVictor: String?,
    versionName: String,
    onNewGame: () -> Unit,
    onContinue: () -> Unit,
    onSeasonRecord: () -> Unit,
    onSettings: () -> Unit,
    onPlayerLayer: () -> Unit,
    onCredits: () -> Unit,
    onExit: () -> Unit,
) {
    TerminalSurface(frameTone = TerminalFrameTone.GOLD) {
        GlassDragonTitleBanner()
        CrystalDivider()
        TerminalText(
            text = "A WINGTIP STUDIO DOOR GAME",
            modifier = Modifier.fillMaxWidth(),
            color = AnsiColor.DimGray,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        TerminalGap(lines = 2)
        val status = when (menuState.campaign) {
            CampaignSlotState.NONE -> "No season is currently active."
            CampaignSlotState.ACTIVE ->
                (activeCharacterName ?: "An adventurer") + " — Day " + (activeDay ?: "?")
            CampaignSlotState.COMPLETED ->
                "Season complete — " + (completedVictor ?: "a champion") + " defeated the Dragon."
            CampaignSlotState.INVALID -> "The active ledger cannot be read safely."
        }
        TerminalText(status, color = when (menuState.campaign) {
            CampaignSlotState.COMPLETED -> AnsiColor.Gold
            CampaignSlotState.INVALID -> AnsiColor.BrightRed
            CampaignSlotState.ACTIVE -> AnsiColor.IceBlue
            CampaignSlotState.NONE -> AnsiColor.DimGray
        })
        TerminalGap(lines = 2)
        var command = 1
        if (menuState.showContinue) {
            TerminalMenuRow(
                number = command++,
                label = if (menuState.conclusionPending) "Continue Conclusion" else "Continue",
                onClick = onContinue,
            )
        }
        if (menuState.campaign == CampaignSlotState.COMPLETED && menuState.showSeasonRecord) {
            TerminalMenuRow(number = command++, label = "Season Record", onClick = onSeasonRecord)
        }
        TerminalMenuRow(number = command++, label = "New Game", onClick = onNewGame)
        if (menuState.campaign != CampaignSlotState.COMPLETED && menuState.showSeasonRecord) {
            TerminalMenuRow(number = command++, label = "Last Season Record", onClick = onSeasonRecord)
        }
        TerminalMenuRow(number = command++, label = "Settings", onClick = onSettings)
        TerminalMenuRow(number = command++, label = "Player Layer", onClick = onPlayerLayer)
        TerminalMenuRow(number = command, label = "Credits", onClick = onCredits)
        TerminalMenuRow(number = 0, label = "Exit", onClick = onExit)
        TerminalGap()
        TerminalText(
            text = "Version $versionName",
            modifier = Modifier.fillMaxWidth(),
            color = AnsiColor.DimGray,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun SettingsScreen(
    musicEnabled: Boolean,
    clickSoundEnabled: Boolean,
    onToggleMusic: () -> Unit,
    onToggleClickSound: () -> Unit,
    onReturn: () -> Unit,
) {
    TerminalSurface {
        ScreenHeader("SETTINGS")
        TerminalGap(lines = 2)
        TerminalText("Changes take effect immediately.", color = AnsiColor.LightGray)
        TerminalGap()
        TerminalMenuRow(
            number = 1,
            label = "Music: " + if (musicEnabled) "ON" else "OFF",
            onClick = onToggleMusic,
        )
        TerminalMenuRow(
            number = 2,
            label = "Click Sound: " + if (clickSoundEnabled) "ON" else "OFF",
            onClick = onToggleClickSound,
        )
        TerminalMenuRow(number = 0, label = "Return to Main Menu", onClick = onReturn)
    }
}

@Composable
fun NameEntryScreen(
    name: String,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onReturn: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(180)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    TerminalSurface {
        ScreenHeader("NEW ADVENTURER")
        TerminalGap(lines = 2)
        TerminalText("The realm has no record of you.", color = AnsiColor.LightGray)
        TerminalGap()
        TerminalText("What name shall be written in the ledger?", color = AnsiColor.Cyan)
        TerminalGap()

        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .focusRequester(focusRequester)
                .background(AnsiColor.Cyan.copy(alpha = 0.09f))
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .semantics { contentDescription = "Adventurer name terminal prompt" },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = AnsiColor.IceBlue),
            singleLine = true,
            cursorBrush = SolidColor(AnsiColor.IceBlue),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            decorationBox = { innerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TerminalText("> ", color = AnsiColor.Yellow)
                    Box(modifier = Modifier.weight(1f)) {
                        if (name.isEmpty()) {
                            TerminalText("<enter name>", color = AnsiColor.DimGray)
                        }
                        innerTextField()
                    }
                }
            },
        )
        TerminalRule(color = AnsiColor.Cyan)
        TerminalText(
            text = "Maximum ${CharacterNameValidator.MAX_CODE_POINTS} characters. Unicode is welcome.",
            color = AnsiColor.DimGray,
            style = MaterialTheme.typography.bodyMedium,
        )
        errorMessage?.let {
            TerminalGap()
            TerminalText("ERROR: $it", color = AnsiColor.BrightRed)
        }
        TerminalGap(lines = 2)
        TerminalMenuRow(number = 1, label = "Accept Name", onClick = onSubmit)
        TerminalMenuRow(number = 2, label = "Return to Main Menu", onClick = onReturn)
    }
}

@Composable
fun NameConfirmationScreen(
    characterName: String,
    onEnterRealm: () -> Unit,
    onChangeName: () -> Unit,
) {
    TerminalSurface(frameTone = TerminalFrameTone.GOLD) {
        ScreenHeader("LEDGER CONFIRMATION")
        TerminalGap(lines = 2)
        TerminalText("You shall be known as:", color = AnsiColor.LightGray)
        TerminalGap()
        TerminalText(
            text = characterName,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            color = AnsiColor.BrightBlue,
            style = MaterialTheme.typography.titleLarge,
        )
        TerminalGap(lines = 2)
        TerminalMenuRow(number = 1, label = "Enter the Realm", onClick = onEnterRealm)
        TerminalMenuRow(number = 2, label = "Change Name", onClick = onChangeName)
    }
}

@Composable
fun NewGameWarningScreen(
    existingCharacterName: String?,
    saveIsBroken: Boolean,
    onStartOver: () -> Unit,
    onReturn: () -> Unit,
) {
    TerminalSurface(frameTone = TerminalFrameTone.DANGER) {
        ScreenHeader("START A NEW SEASON?", color = AnsiColor.BrightRed)
        TerminalGap(lines = 2)
        if (saveIsBroken) {
            TerminalText("An unreadable adventure already occupies the ledger.", color = AnsiColor.Yellow)
        } else {
            TerminalText(
                "A record already exists for ${existingCharacterName ?: "this adventurer"}.",
                color = AnsiColor.Yellow,
            )
        }
        TerminalGap()
        TerminalText("The active season will be replaced only after a new adventurer is confirmed.", color = AnsiColor.BrightRed)
        TerminalGap(lines = 2)
        TerminalMenuRow(number = 1, label = "Start New Season", onClick = onStartOver)
        TerminalMenuRow(number = 2, label = "Go Back", onClick = onReturn)
    }
}

@Composable
fun TownScreen(
    gameState: GameState,
    welcomeBack: Boolean,
    townNotice: String?,
    onForest: () -> Unit,
    onHealer: () -> Unit,
    onWeaponShop: () -> Unit,
    onArmorShop: () -> Unit,
    onBank: () -> Unit,
    onTraining: () -> Unit,
    onInn: () -> Unit,
    onRankings: () -> Unit,
    onCharacter: () -> Unit,
    onDragonWatch: () -> Unit,
    onEndDay: () -> Unit,
    onPlayerLayer: () -> Unit,
    onMainMenu: () -> Unit,
) {
    val player = gameState.player
    TerminalSurface {
        ScreenHeader("THE VILLAGE SQUARE", color = AnsiColor.IceBlue)
        TerminalGap()
        TerminalText(
            text = if (welcomeBack) "Welcome back, ${player.name}." else "Welcome, ${player.name}.",
            color = AnsiColor.IceBlue,
        )
        TerminalText("It is Day ${gameState.currentDay} in the realm.", color = AnsiColor.BrightGreen)
        TerminalGap()
        AnsiText(
            spans = listOf(
                AnsiSpan(player.name, AnsiColor.IceBlue, bold = true),
                AnsiSpan(", Level ${player.level}", AnsiColor.LightGray),
            ),
        )
        TerminalText("HP: ${player.currentHp} / ${player.maximumHp}", color = AnsiColor.LightGray)
        TerminalText("Gold: ${player.gold}", color = AnsiColor.Yellow)
        TerminalText("Bank: ${player.bankedGold}", color = AnsiColor.BrightGreen)
        TerminalText(
            "Forest Fights Remaining: ${gameState.playerDay.fightsRemaining}",
            color = AnsiColor.BrightGreen,
        )
        TerminalText(
            "Realm Rank: #${CommunityRules.rankOf(gameState, player.id)}",
            color = AnsiColor.Cyan,
        )
        TerminalText(
            "Adventurer Challenges: ${gameState.playerDay.challengesRemaining}",
            color = AnsiColor.Magenta,
        )
        TerminalText(
            "Dragon Season: " + when (gameState.season.stage) {
                DragonStage.WHISPERS -> "Whispers"
                DragonStage.SIGNS -> "Signs"
                DragonStage.FEAR -> "Fear"
                DragonStage.CRISIS -> "Crisis"
                DragonStage.CHALLENGE_WINDOW -> "Challenge Window"
                DragonStage.RESOLVED -> "Resolved"
            },
            color = when (gameState.season.stage) {
                DragonStage.WHISPERS -> AnsiColor.DimGray
                DragonStage.SIGNS -> AnsiColor.Cyan
                DragonStage.FEAR -> AnsiColor.Yellow
                DragonStage.CRISIS -> AnsiColor.Magenta
                DragonStage.CHALLENGE_WINDOW -> AnsiColor.BrightRed
                DragonStage.RESOLVED -> AnsiColor.BrightGreen
            },
        )
        if (ProgressionRules.isTrainingEligible(player)) {
            TerminalGap()
            TerminalText("You feel ready for greater challenges.", color = AnsiColor.Yellow)
            TerminalText("The Training Master may test you.", color = AnsiColor.Cyan)
        }
        if (gameState.playerDay.wasDefeatedToday) {
            TerminalGap()
            TerminalText("Your Forest adventure ended in defeat.", color = AnsiColor.BrightRed)
            TerminalText("End the Day to recover and fight again.", color = AnsiColor.Cyan)
        }
        townNotice?.let { notice ->
            TerminalGap()
            TerminalText(notice, color = AnsiColor.BrightBlue)
        }
        TerminalGap()
        TerminalText(
            "Town criers shout the day's news while adventurers come and go through the square.",
            color = AnsiColor.LightGray,
        )
        TerminalGap(lines = 2)
        TerminalMenuRow(number = 1, label = "Enter the Forest", onClick = onForest)
        TerminalMenuRow(number = 2, label = "Visit the Healer", onClick = onHealer)
        TerminalMenuRow(number = 3, label = "Weapon Shop", onClick = onWeaponShop)
        TerminalMenuRow(number = 4, label = "Armor Shop", onClick = onArmorShop)
        TerminalMenuRow(number = 5, label = "First Bank of the Realm", onClick = onBank)
        TerminalMenuRow(number = 6, label = "Training Hall", onClick = onTraining)
        TerminalMenuRow(number = 7, label = "The Dragon's Rest", onClick = onInn)
        TerminalMenuRow(number = 8, label = "View Rankings", onClick = onRankings)
        TerminalMenuRow(number = 9, label = "Character", onClick = onCharacter)
        TerminalMenuRow(
            number = 10,
            label = if (gameState.season.stage == DragonStage.CHALLENGE_WINDOW) {
                "Face the Glass Dragon"
            } else {
                "Dragon Watch"
            },
            onClick = onDragonWatch,
        )
        TerminalMenuRow(number = 0, label = "End the Day", onClick = onEndDay)
        TerminalMenuRow(number = 11, label = "Main Menu", onClick = onMainMenu)
        TerminalMenuRow(number = 12, label = "Player Layer", onClick = onPlayerLayer)
    }
}

@Composable
fun CharacterStatusScreen(
    gameState: GameState,
    onReturnToTown: () -> Unit,
) {
    val player = gameState.player
    TerminalSurface {
        ScreenHeader("CHARACTER RECORD", color = AnsiColor.Magenta)
        TerminalGap(lines = 2)
        RecordLine("Name", player.name, AnsiColor.BrightBlue)
        RecordLine("Level", player.level.toString())
        RecordLine("Experience", player.experience.toString())
        RecordLine("Hit Points", "${player.currentHp} / ${player.maximumHp}")
        val weapon = EquipmentCatalog.requireWeapon(player.weaponId)
        val armor = EquipmentCatalog.requireArmor(player.armorId)
        RecordLine("Attack", "${player.attack} (${player.baseAttack} + ${weapon.attackContribution})")
        RecordLine("Defense", "${player.defense} (${player.baseDefense} + ${armor.defenseContribution})")
        RecordLine("Gold Carried", player.gold.toString(), AnsiColor.Yellow)
        RecordLine("Gold Banked", player.bankedGold.toString(), AnsiColor.BrightGreen)
        TerminalGap()
        RecordLine("Weapon", player.weapon, AnsiColor.BrightGreen)
        RecordLine("Armor", player.armor, AnsiColor.BrightGreen)
        RecordLine("Game Day", gameState.currentDay.toString(), AnsiColor.Cyan)
        RecordLine("Realm Rank", CommunityRules.rankOf(gameState, player.id).toString(), AnsiColor.Yellow)
        RecordLine(
            "Challenges",
            "${player.challengeWins} wins / ${player.challengeLosses} losses",
            AnsiColor.Magenta,
        )
        RecordLine("Duels Left", gameState.playerDay.challengesRemaining.toString(), AnsiColor.Cyan)
        RecordLine("Forest Fights", gameState.playerDay.fightsRemaining.toString(), AnsiColor.BrightGreen)
        RecordLine(
            "Next Level XP",
            ProgressionRules.experienceRequiredForNextLevel(player).toString(),
            AnsiColor.Yellow,
        )
        TerminalGap(lines = 2)
        TerminalMenuRow(number = 1, label = "Return to Town", onClick = onReturnToTown)
    }
}

@Composable
fun AreaPlaceholderScreen(
    areaName: String,
    gameState: GameState,
    onReturnToTown: () -> Unit,
) {
    TerminalSurface {
        ScreenHeader(areaName.uppercase(), color = areaColor(areaName))
        TerminalGap(lines = 2)
        TerminalText(
            "${gameState.player.name} approaches ${areaApproach(areaName)}.",
            color = AnsiColor.LightGray,
        )
        TerminalGap()
        TerminalText("This area is not yet available.", color = AnsiColor.DimGray)
        TerminalGap(lines = 2)
        TerminalMenuRow(number = 1, label = "Return to Town", onClick = onReturnToTown)
    }
}

@Composable
fun SaveErrorScreen(
    message: String,
    onReturn: () -> Unit,
    onDeleteBrokenSave: () -> Unit,
) {
    TerminalSurface(frameTone = TerminalFrameTone.DANGER) {
        ScreenHeader("SAVE DATA ERROR", color = AnsiColor.BrightRed)
        TerminalGap(lines = 2)
        TerminalText("This adventure could not be loaded.", color = AnsiColor.Yellow)
        TerminalText(message, color = AnsiColor.DimGray)
        TerminalGap()
        TerminalText("The save has not been erased.", color = AnsiColor.LightGray)
        TerminalGap(lines = 2)
        TerminalMenuRow(number = 1, label = "Return to Main Menu", onClick = onReturn)
        TerminalMenuRow(number = 2, label = "Delete Broken Save", onClick = onDeleteBrokenSave)
    }
}

@Composable
fun CreditsScreen(versionName: String, onReturn: () -> Unit) {
    TerminalSurface {
        ScreenHeader("CREDITS")
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GlassDragonCrest(sizeDp = 88)
        }
        TerminalGap()
        TerminalText("LEGEND OF THE GLASS DRAGON", color = AnsiColor.IceBlue)
        TerminalText("A Wingtip Studio Game", color = AnsiColor.Gold)
        TerminalGap()
        TerminalText(
            "Design and development: Wingtip Studio",
            color = AnsiColor.LightGray,
        )
        TerminalGap()
        TerminalText(
            "Music: Pictures at an Exhibition by Modest Mussorgsky.",
            color = AnsiColor.LightGray,
        )
        TerminalText(
            "Performed by The Skidmore College Orchestra. Supplied metadata marks the recordings Public Domain.",
            color = AnsiColor.DimGray,
            style = MaterialTheme.typography.bodyMedium,
        )
        TerminalText(
            "Recording source: Classicals.de (CC Public Domain Mark 1.0).",
            color = AnsiColor.DimGray,
            style = MaterialTheme.typography.bodyMedium,
        )
        TerminalGap()
        TerminalText(
            "Built with AndroidX and Kotlin (Apache License 2.0). Third-party notices are packaged with the game.",
            color = AnsiColor.DimGray,
            style = MaterialTheme.typography.bodyMedium,
        )
        TerminalGap()
        TerminalBox(
            lines = listOf(
                "Terminal: ANSI enabled",
                "Network: not required",
                "Version: $versionName",
            ),
            color = AnsiColor.Magenta,
        )
        TerminalGap(lines = 2)
        TerminalMenuRow(number = 1, label = "Return to Main Menu", onClick = onReturn)
    }
}

@Composable
private fun RecordLine(
    label: String,
    value: String,
    valueColor: Color = AnsiColor.LightGray,
) {
    val dots = ".".repeat((12 - label.length).coerceAtLeast(1))
    AnsiText(
        spans = listOf(
            AnsiSpan(label, AnsiColor.DimGray),
            AnsiSpan(dots, AnsiColor.DimGray),
            AnsiSpan(" $value", valueColor),
        ),
    )
}

@Composable
internal fun ScreenHeader(
    title: String,
    color: Color = AnsiColor.Cyan,
) {
    CrystalDivider(danger = color == AnsiColor.BrightRed || color == AnsiColor.Red)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val headerStyle = if (maxWidth < 480.dp) {
            MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 25.sp)
        } else {
            MaterialTheme.typography.titleLarge
        }
        TerminalText(
            text = "╟ ◆ $title ◆ ╢",
            modifier = Modifier.fillMaxWidth(),
            color = color,
            style = headerStyle,
            textAlign = TextAlign.Center,
        )
    }
    locationForTitle(title)?.let { location ->
        TerminalGap()
        LocationPlate(location)
    }
    CrystalDivider(danger = color == AnsiColor.BrightRed || color == AnsiColor.Red)
}

private fun locationForTitle(title: String): LocationArt? = when (title) {
    "THE VILLAGE SQUARE" -> LocationArt.TOWN
    "THE DEEP FOREST", "DEEP FOREST" -> LocationArt.FOREST
    "THE DRAGON'S REST" -> LocationArt.INN
    "TRAINING HALL" -> LocationArt.TRAINING
    "THE HEALER" -> LocationArt.HEALER
    "YE OLDE WEAPON SHOPPE" -> LocationArt.WEAPON_SHOP
    "THE VILLAGE ARMORER" -> LocationArt.ARMOR_SHOP
    "FIRST BANK OF THE REALM", "DEPOSIT GOLD", "WITHDRAW GOLD" -> LocationArt.BANK
    else -> null
}

private fun areaColor(areaName: String): Color = when (areaName) {
    "Forest" -> AnsiColor.BrightGreen
    "Inn" -> AnsiColor.Yellow
    "Training" -> AnsiColor.BrightRed
    "Rankings" -> AnsiColor.Cyan
    else -> AnsiColor.LightGray
}

private fun areaApproach(areaName: String): String = when (areaName) {
    "Forest" -> "the forest road"
    "Inn" -> "the lantern-lit inn"
    "Training" -> "the old training yard"
    "Rankings" -> "the Hall of Records"
    else -> "this place"
}
