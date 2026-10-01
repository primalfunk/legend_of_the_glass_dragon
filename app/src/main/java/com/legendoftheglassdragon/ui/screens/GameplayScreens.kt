package com.legendoftheglassdragon.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.legendoftheglassdragon.game.CombatMessageKind
import com.legendoftheglassdragon.game.CombatState
import com.legendoftheglassdragon.game.CombatStatus
import com.legendoftheglassdragon.game.GameBalance
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.MonsterDefinition
import com.legendoftheglassdragon.game.ProgressionRules
import com.legendoftheglassdragon.ui.components.TerminalGap
import com.legendoftheglassdragon.ui.components.MajorEventArt
import com.legendoftheglassdragon.ui.components.MajorEventBanner
import com.legendoftheglassdragon.ui.components.TerminalMenuRow
import com.legendoftheglassdragon.ui.components.TerminalFrameTone
import com.legendoftheglassdragon.ui.components.TerminalPanel
import com.legendoftheglassdragon.ui.components.TerminalSurface
import com.legendoftheglassdragon.ui.components.TerminalText
import com.legendoftheglassdragon.ui.theme.AnsiColor

@Composable
fun ForestScreen(
    gameState: GameState,
    message: String?,
    onSearch: (Long) -> Unit,
    onReturnToTown: () -> Unit,
) {
    val fights = gameState.playerDay.fightsRemaining
    TerminalSurface {
        ScreenHeader("THE DEEP FOREST", color = AnsiColor.BrightGreen)
        TerminalGap(lines = 2)
        TerminalText(
            gameState.player.name + " stands where the village road vanishes beneath the trees.",
            color = AnsiColor.LightGray,
        )
        TerminalGap()
        TerminalText("Forest Fights Remaining: $fights", color = AnsiColor.Yellow)
        message?.let {
            TerminalGap()
            TerminalText(it, color = AnsiColor.BrightRed)
        }
        TerminalGap(lines = 2)
        if (fights > 0 && !gameState.playerDay.wasDefeatedToday) {
            TerminalMenuRow(
                number = 1,
                label = "Search the Forest",
                onClick = { onSearch(gameState.revision) },
            )
        } else {
            TerminalText(
                if (gameState.playerDay.wasDefeatedToday) {
                    "Your wounds forbid another expedition today. End the Day to recover."
                } else {
                    "Your fighting day is over. The forest will wait until tomorrow."
                },
                color = AnsiColor.DimGray,
            )
            TerminalMenuRow(
                number = 1,
                label = "Search the Forest",
                enabled = false,
                note = if (gameState.playerDay.wasDefeatedToday) "<DEFEATED>" else "<NO FIGHTS>",
                onClick = {},
            )
        }
        TerminalMenuRow(number = 2, label = "Return to Town", onClick = onReturnToTown)
    }
}

@Composable
fun CombatScreen(
    gameState: GameState,
    combat: CombatState,
    monster: MonsterDefinition,
    onAttack: (Long) -> Unit,
    onRun: (Long) -> Unit,
    onSearchAgain: (Long) -> Unit,
    onReturnToTown: (Long) -> Unit,
) {
    val frameTone = when (combat.status) {
        CombatStatus.VICTORY -> TerminalFrameTone.GOLD
        CombatStatus.ACTIVE, CombatStatus.DEFEAT -> TerminalFrameTone.DANGER
        CombatStatus.ESCAPED -> TerminalFrameTone.GLASS
    }
    TerminalSurface(frameTone = frameTone) {
        when (combat.status) {
            CombatStatus.VICTORY -> MajorEventBanner(MajorEventArt.VICTORY)
            CombatStatus.DEFEAT -> MajorEventBanner(MajorEventArt.DEFEAT)
            else -> Unit
        }
        ScreenHeader(
            title = when (combat.status) {
                CombatStatus.ACTIVE -> "DEEP FOREST"
                CombatStatus.VICTORY -> "*** VICTORY! ***"
                CombatStatus.ESCAPED -> "ESCAPED"
                CombatStatus.DEFEAT -> "YOU HAVE FALLEN"
            },
            color = when (combat.status) {
                CombatStatus.ACTIVE -> AnsiColor.BrightGreen
                CombatStatus.VICTORY -> AnsiColor.Yellow
                CombatStatus.ESCAPED -> AnsiColor.Cyan
                CombatStatus.DEFEAT -> AnsiColor.BrightRed
            },
        )
        TerminalGap()
        when (combat.status) {
            CombatStatus.ACTIVE -> ActiveCombatContent(
                gameState = gameState,
                combat = combat,
                monster = monster,
                onAttack = onAttack,
                onRun = onRun,
            )
            CombatStatus.VICTORY -> VictoryContent(
                gameState = gameState,
                combat = combat,
                monster = monster,
                onSearchAgain = onSearchAgain,
                onReturnToTown = onReturnToTown,
            )
            CombatStatus.ESCAPED -> EscapeContent(
                gameState = gameState,
                combat = combat,
                onSearchAgain = onSearchAgain,
                onReturnToTown = onReturnToTown,
            )
            CombatStatus.DEFEAT -> DefeatContent(
                gameState = gameState,
                combat = combat,
                onReturnToTown = onReturnToTown,
            )
        }
    }
}

@Composable
private fun ActiveCombatContent(
    gameState: GameState,
    combat: CombatState,
    monster: MonsterDefinition,
    onAttack: (Long) -> Unit,
    onRun: (Long) -> Unit,
) {
    CombatLog(combat)
    TerminalGap()
    TerminalPanel(tone = TerminalFrameTone.DANGER) {
        TerminalText(monster.displayName.uppercase(), color = AnsiColor.BrightRed)
        TerminalText(
            "HP: " + combat.monsterCurrentHp + " / " + monster.maximumHp,
            color = AnsiColor.LightGray,
        )
    }
    TerminalGap()
    TerminalPanel {
        TerminalText(gameState.player.name, color = AnsiColor.IceBlue)
        TerminalText(
            "HP: " + gameState.player.currentHp + " / " + gameState.player.maximumHp,
            color = AnsiColor.LightGray,
        )
        TerminalText(
            "Forest Fights Remaining: " + gameState.playerDay.fightsRemaining,
            color = AnsiColor.DimGray,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    TerminalGap(lines = 2)
    TerminalMenuRow(
        number = 1,
        label = "Attack",
        onClick = { onAttack(gameState.revision) },
    )
    TerminalMenuRow(
        number = 2,
        label = "Run Away",
        onClick = { onRun(gameState.revision) },
    )
}

@Composable
private fun VictoryContent(
    gameState: GameState,
    combat: CombatState,
    monster: MonsterDefinition,
    onSearchAgain: (Long) -> Unit,
    onReturnToTown: (Long) -> Unit,
) {
    TerminalText("The " + monster.displayName + " falls!", color = AnsiColor.Yellow)
    TerminalGap()
    TerminalText("Experience gained: " + combat.experienceReward, color = AnsiColor.Cyan)
    TerminalText("Gold found:       " + combat.goldReward, color = AnsiColor.Yellow)
    TerminalText(
        "Your HP:          " + gameState.player.currentHp + " / " + gameState.player.maximumHp,
        color = AnsiColor.LightGray,
    )
    TerminalGap()
    TerminalText(
        "Forest Fights Remaining: " + gameState.playerDay.fightsRemaining,
        color = AnsiColor.BrightGreen,
    )
    if (ProgressionRules.isTrainingEligible(gameState.player)) {
        TerminalText("You are ready to seek Training.", color = AnsiColor.Magenta)
    }
    TerminalGap(lines = 2)
    TerminalMenuRow(
        number = 1,
        label = "Search Again",
        enabled = gameState.playerDay.fightsRemaining > 0,
        note = if (gameState.playerDay.fightsRemaining == 0) "<NO FIGHTS>" else null,
        onClick = { onSearchAgain(gameState.revision) },
    )
    TerminalMenuRow(
        number = 2,
        label = "Return to Town",
        onClick = { onReturnToTown(gameState.revision) },
    )
}

@Composable
private fun EscapeContent(
    gameState: GameState,
    combat: CombatState,
    onSearchAgain: (Long) -> Unit,
    onReturnToTown: (Long) -> Unit,
) {
    CombatLog(combat)
    TerminalGap()
    TerminalText("The spent forest fight is not restored.", color = AnsiColor.DimGray)
    TerminalText(
        "Forest Fights Remaining: " + gameState.playerDay.fightsRemaining,
        color = AnsiColor.BrightGreen,
    )
    TerminalGap(lines = 2)
    TerminalMenuRow(
        number = 1,
        label = "Search Again",
        enabled = gameState.playerDay.fightsRemaining > 0,
        note = if (gameState.playerDay.fightsRemaining == 0) "<NO FIGHTS>" else null,
        onClick = { onSearchAgain(gameState.revision) },
    )
    TerminalMenuRow(
        number = 2,
        label = "Return to Town",
        onClick = { onReturnToTown(gameState.revision) },
    )
}

@Composable
private fun DefeatContent(
    gameState: GameState,
    combat: CombatState,
    onReturnToTown: (Long) -> Unit,
) {
    TerminalText("Darkness takes you...", color = AnsiColor.BrightRed)
    TerminalGap()
    TerminalText(
        "Hours later, you awaken in the healer's care.",
        color = AnsiColor.LightGray,
    )
    TerminalText("Gold lost: " + combat.goldLost, color = AnsiColor.Yellow)
    TerminalText(
        "Recovered HP: " + gameState.player.currentHp + " / " + gameState.player.maximumHp,
        color = AnsiColor.Cyan,
    )
    TerminalText("Your fighting day is over.", color = AnsiColor.BrightRed)
    TerminalText("Your level and experience are safe.", color = AnsiColor.BrightGreen)
    TerminalText("Return to Town and End the Day to recover fully.", color = AnsiColor.Cyan)
    TerminalGap(lines = 2)
    TerminalMenuRow(
        number = 1,
        label = "Return to Town",
        onClick = { onReturnToTown(gameState.revision) },
    )
}

@Composable
private fun CombatLog(combat: CombatState) {
    combat.recentMessages.forEach { message ->
        TerminalText(
            text = message.text,
            color = combatMessageColor(message.kind),
        )
        TerminalGap()
    }
}

private fun combatMessageColor(kind: CombatMessageKind): Color = when (kind) {
    CombatMessageKind.ENCOUNTER -> AnsiColor.Yellow
    CombatMessageKind.PLAYER_ACTION -> AnsiColor.BrightGreen
    CombatMessageKind.MONSTER_ACTION -> AnsiColor.BrightRed
    CombatMessageKind.VICTORY -> AnsiColor.Yellow
    CombatMessageKind.ESCAPE -> AnsiColor.Cyan
    CombatMessageKind.DEFEAT -> AnsiColor.BrightRed
    CombatMessageKind.INFO -> AnsiColor.LightGray
}

@Composable
fun HealerScreen(
    gameState: GameState,
    message: String?,
    onHeal: (Long) -> Unit,
    onReturnToTown: () -> Unit,
) {
    val missingHp = gameState.player.maximumHp - gameState.player.currentHp
    val cost = missingHp * GameBalance.HEALING_GOLD_PER_HP
    val canAfford = gameState.player.gold >= cost
    TerminalSurface {
        ScreenHeader("THE HEALER", color = AnsiColor.Cyan)
        TerminalGap(lines = 2)
        TerminalText("'I can mend those wounds.'", color = AnsiColor.LightGray)
        TerminalGap()
        TerminalText(
            "Health: " + gameState.player.currentHp + " / " + gameState.player.maximumHp,
            color = AnsiColor.BrightGreen,
        )
        TerminalText("Gold:   " + gameState.player.gold, color = AnsiColor.Yellow)
        TerminalText("Cost:   " + cost + " gold", color = AnsiColor.Cyan)
        message?.let {
            TerminalGap()
            TerminalText(it, color = AnsiColor.Magenta)
        }
        if (missingHp > 0 && !canAfford) {
            TerminalGap()
            TerminalText("'Return when you can pay the full fee.'", color = AnsiColor.BrightRed)
        }
        if (missingHp == 0) {
            TerminalGap()
            TerminalText("'You are already in perfect health.'", color = AnsiColor.DimGray)
        }
        TerminalGap(lines = 2)
        TerminalMenuRow(
            number = 1,
            label = "Heal Me",
            enabled = missingHp > 0 && canAfford,
            note = when {
                missingHp == 0 -> "<FULL HEALTH>"
                !canAfford -> "<NEED " + cost + " GOLD>"
                else -> null
            },
            onClick = { onHeal(gameState.revision) },
        )
        TerminalMenuRow(number = 2, label = "Return to Town", onClick = onReturnToTown)
    }
}

@Composable
fun TrainingScreen(
    gameState: GameState,
    onTrain: (Long) -> Unit,
    onReturnToTown: () -> Unit,
) {
    val player = gameState.player
    val ready = ProgressionRules.isTrainingEligible(player)
    val attempted = gameState.playerDay.trainingAttemptedToday
    val required = ProgressionRules.experienceRequiredForNextLevel(player)
    TerminalSurface {
        ScreenHeader("TRAINING HALL", color = AnsiColor.BrightRed)
        TerminalGap(lines = 2)
        TerminalText("The old master studies you carefully.", color = AnsiColor.LightGray)
        TerminalGap()
        TerminalText("Level:      " + player.level, color = AnsiColor.Cyan)
        TerminalText("Experience: " + player.experience, color = AnsiColor.LightGray)
        TerminalText("Required:   " + required, color = AnsiColor.Yellow)
        TerminalGap()
        if (attempted) {
            TerminalText("'One trial is enough for today. Return tomorrow.'", color = AnsiColor.Magenta)
        } else if (ready) {
            TerminalText("'Your spirit is ready. Show me your discipline.'", color = AnsiColor.Yellow)
        } else {
            TerminalText("'You have more to learn in the forest.'", color = AnsiColor.DimGray)
            TerminalText(
                "Experience still needed: " + ProgressionRules.experienceStillNeeded(player),
                color = AnsiColor.BrightRed,
            )
        }
        TerminalGap(lines = 2)
        TerminalMenuRow(
            number = 1,
            label = "Undertake Training",
            enabled = ready && !attempted,
            note = when {
                attempted -> "<TRIED TODAY>"
                ready -> null
                else -> "<NOT READY>"
            },
            onClick = { onTrain(gameState.revision) },
        )
        TerminalMenuRow(number = 2, label = "Return to Town", onClick = onReturnToTown)
    }
}

@Composable
fun TrainingCompleteScreen(
    gameState: GameState,
    onReturnToTown: () -> Unit,
) {
    val succeeded = gameState.playerDay.lastTrainingSucceeded == true
    TerminalSurface(frameTone = if (succeeded) TerminalFrameTone.GOLD else TerminalFrameTone.DANGER) {
        if (succeeded) MajorEventBanner(MajorEventArt.LEVEL_UP)
        ScreenHeader(
            if (succeeded) "TRAINING COMPLETE" else "TRAINING ENDS",
            color = if (succeeded) AnsiColor.Yellow else AnsiColor.BrightRed,
        )
        TerminalGap(lines = 2)
        if (succeeded) {
            TerminalText(
                "You have attained Level " + gameState.player.level + "!",
                color = AnsiColor.BrightGreen,
            )
            TerminalGap()
            TerminalText("Maximum HP increased by " + GameBalance.LEVEL_MAX_HP_GAIN + ".", color = AnsiColor.Cyan)
            TerminalText("Attack increased by " + GameBalance.LEVEL_ATTACK_GAIN + ".", color = AnsiColor.Cyan)
            TerminalText("Defense increased by " + GameBalance.LEVEL_DEFENSE_GAIN + ".", color = AnsiColor.Cyan)
            TerminalText("Your health is fully restored.", color = AnsiColor.LightGray)
            TerminalText(
                "Next promotion: " + ProgressionRules.experienceRequiredForNextLevel(gameState.player) + " XP.",
                color = AnsiColor.Yellow,
            )
            TerminalText("Old foes will feel the difference.", color = AnsiColor.Magenta)
        } else {
            TerminalText("The old master knocks you to the sand.", color = AnsiColor.BrightRed)
            TerminalGap()
            TerminalText("Your experience is safe, but your promotion must wait.", color = AnsiColor.Cyan)
            TerminalText("Rest, improve your gear, and try again tomorrow.", color = AnsiColor.Yellow)
            TerminalText(
                "Health: " + gameState.player.currentHp + " / " + gameState.player.maximumHp,
                color = AnsiColor.LightGray,
            )
        }
        TerminalGap(lines = 2)
        TerminalMenuRow(number = 1, label = "Return to Town", onClick = onReturnToTown)
    }
}

@Composable
fun EndDayScreen(
    gameState: GameState,
    onConfirm: (Long) -> Unit,
    onReturnToTown: () -> Unit,
) {
    TerminalSurface {
        ScreenHeader("END THE DAY?", color = AnsiColor.Yellow)
        TerminalGap(lines = 2)
        TerminalText(
            gameState.player.name + ", you have " + gameState.playerDay.fightsRemaining +
                " forest fights remaining.",
            color = AnsiColor.LightGray,
        )
        TerminalGap()
        TerminalText(
            "Once you retire for the night, those fights will be lost.",
            color = AnsiColor.BrightRed,
        )
        TerminalText(
            "Every adventurer in the realm will then take a turn.",
            color = AnsiColor.Yellow,
        )
        TerminalText(
            "You will awaken fully healed on Day " + (gameState.currentDay + 1) + ".",
            color = AnsiColor.Cyan,
        )
        TerminalGap(lines = 2)
        TerminalMenuRow(
            number = 1,
            label = "Sleep Until Tomorrow",
            onClick = { onConfirm(gameState.revision) },
        )
        TerminalMenuRow(number = 2, label = "Return to Town", onClick = onReturnToTown)
    }
}
