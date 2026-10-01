package com.legendoftheglassdragon.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.legendoftheglassdragon.game.CombatMessageKind
import com.legendoftheglassdragon.game.CommunityRules
import com.legendoftheglassdragon.game.DragonCombatStatus
import com.legendoftheglassdragon.game.DragonReadinessRules
import com.legendoftheglassdragon.game.DragonSeasonBalance
import com.legendoftheglassdragon.game.DragonStage
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.SeasonNarrator
import com.legendoftheglassdragon.ui.components.AnsiSpan
import com.legendoftheglassdragon.ui.components.AnsiText
import com.legendoftheglassdragon.ui.components.CrystalDivider
import com.legendoftheglassdragon.ui.components.GlassDragonCrest
import com.legendoftheglassdragon.ui.components.TerminalFrameTone
import com.legendoftheglassdragon.ui.components.TerminalGap
import com.legendoftheglassdragon.ui.components.TerminalMenuRow
import com.legendoftheglassdragon.ui.components.TerminalPanel
import com.legendoftheglassdragon.ui.components.TerminalRule
import com.legendoftheglassdragon.ui.components.TerminalSurface
import com.legendoftheglassdragon.ui.components.TerminalText
import com.legendoftheglassdragon.ui.theme.AnsiColor

@Composable
fun DragonWatchScreen(
    gameState: GameState,
    message: String?,
    onChallengeDragon: (Long) -> Unit,
    onSupportCandidate: (String, Long) -> Unit,
    onReturnToTown: () -> Unit,
) {
    val readiness = DragonReadinessRules.player(gameState)
    val declared = gameState.season.declaredChallengerIds.mapNotNull { id ->
        gameState.community.adventurers.firstOrNull { it.id == id }
    }
    val supportCandidate = declared.maxByOrNull {
        DragonReadinessRules.npc(it, gameState.season.stage).combatPower
    }
    TerminalSurface(
        frameTone = if (gameState.season.stage >= DragonStage.CRISIS) {
            TerminalFrameTone.DANGER
        } else {
            TerminalFrameTone.GLASS
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassDragonCrest(sizeDp = 82)
        }
        ScreenHeader("THE DRAGON WATCH", color = AnsiColor.IceBlue)
        TerminalText(
            "Season stage: " + stageName(gameState.season.stage),
            color = stageColor(gameState.season.stage),
        )
        TerminalText(
            stageDescription(gameState.season.stage),
            color = AnsiColor.LightGray,
        )
        CrystalDivider(danger = gameState.season.stage >= DragonStage.CRISIS)
        TerminalText("YOUR READINESS", color = AnsiColor.Yellow)
        if (readiness.isReady) {
            TerminalText(
                if (readiness.combatPower < 145) {
                    "You meet the minimum, but the fight will be desperate. Stronger gear is advised."
                } else {
                    "You are well prepared to enter the crystal wastes. The danger remains real."
                },
                color = if (readiness.combatPower < 145) AnsiColor.Yellow else AnsiColor.BrightGreen,
            )
        } else {
            readiness.playerGuidance().forEach { guidance ->
                TerminalText("• " + guidance, color = AnsiColor.LightGray)
            }
        }
        TerminalGap()
        TerminalText("DECLARED CHAMPIONS", color = AnsiColor.Magenta)
        if (declared.isEmpty()) {
            TerminalText("No resident has yet made a public claim.", color = AnsiColor.DimGray)
        } else {
            declared.forEach { npc ->
                TerminalText(
                    "• " + npc.name + " — Level " + npc.level + ", " + npc.weapon,
                    color = AnsiColor.Cyan,
                )
            }
        }
        message?.let {
            TerminalGap()
            TerminalText(it, color = AnsiColor.Yellow)
        }
        TerminalGap(lines = 2)
        TerminalMenuRow(
            number = 1,
            label = "Face the Glass Dragon",
            enabled = readiness.isReady &&
                gameState.season.playerLastAttemptDay != gameState.currentDay,
            note = when {
                gameState.season.stage != DragonStage.CHALLENGE_WINDOW -> "<WINDOW CLOSED>"
                gameState.season.playerLastAttemptDay == gameState.currentDay -> "<ATTEMPT USED TODAY>"
                !readiness.isReady -> "<NOT READY>"
                else -> null
            },
            onClick = { onChallengeDragon(gameState.revision) },
        )
        if (supportCandidate != null) {
            TerminalMenuRow(
                number = 2,
                label = "Support " + supportCandidate.name + " (" +
                    DragonSeasonBalance.SUPPORT_GOLD + " gold)",
                enabled = gameState.player.gold >= DragonSeasonBalance.SUPPORT_GOLD,
                note = if (gameState.player.gold < DragonSeasonBalance.SUPPORT_GOLD) {
                    "<INSUFFICIENT GOLD>"
                } else {
                    null
                },
                onClick = { onSupportCandidate(supportCandidate.id, gameState.revision) },
            )
        }
        TerminalMenuRow(number = 0, label = "Return to Town", onClick = onReturnToTown)
    }
}

@Composable
fun DragonCombatScreen(
    gameState: GameState,
    onAttack: (Long) -> Unit,
    onRetreat: (Long) -> Unit,
    onLeave: (Long) -> Unit,
) {
    val combat = requireNotNull(gameState.activeDragonCombat)
    TerminalSurface(frameTone = TerminalFrameTone.DANGER) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            GlassDragonCrest(sizeDp = 96)
        }
        ScreenHeader("THE CRYSTAL WASTES", color = AnsiColor.BrightRed)
        TerminalText(
            if (combat.crystalHideBroken) "THE DRAGON'S HIDE IS FRACTURED" else "CRYSTAL HIDE: UNBROKEN",
            color = if (combat.crystalHideBroken) AnsiColor.Magenta else AnsiColor.IceBlue,
            style = MaterialTheme.typography.bodyMedium,
        )
        TerminalGap()
        combat.recentMessages.forEach { message ->
            TerminalText(
                message.text,
                color = when (message.kind) {
                    CombatMessageKind.PLAYER_ACTION -> AnsiColor.BrightGreen
                    CombatMessageKind.MONSTER_ACTION,
                    CombatMessageKind.DEFEAT,
                    -> AnsiColor.BrightRed
                    CombatMessageKind.VICTORY -> AnsiColor.Yellow
                    CombatMessageKind.ESCAPE -> AnsiColor.Cyan
                    else -> AnsiColor.LightGray
                },
            )
            TerminalGap()
        }
        TerminalPanel(tone = TerminalFrameTone.DANGER) {
            TerminalText("GLASS DRAGON", color = AnsiColor.BrightRed)
            TerminalText(
                "HP: " + combat.dragonCurrentHp + " / " + DragonSeasonBalance.DRAGON_MAXIMUM_HP,
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
        }
        TerminalGap(lines = 2)
        when (combat.status) {
            DragonCombatStatus.ACTIVE -> {
                TerminalMenuRow(number = 1, label = "Strike the Dragon", onClick = {
                    onAttack(gameState.revision)
                })
                TerminalMenuRow(number = 2, label = "Withdraw", onClick = {
                    onRetreat(gameState.revision)
                })
            }
            DragonCombatStatus.PLAYER_VICTORY -> {
                TerminalText("THE GLASS DRAGON HAS FALLEN.", color = AnsiColor.Yellow)
                TerminalMenuRow(number = 1, label = "Witness the Conclusion", onClick = {
                    onLeave(gameState.revision)
                })
            }
            DragonCombatStatus.PLAYER_DEFEAT -> {
                TerminalText("Gold lost: " + combat.goldLost, color = AnsiColor.Yellow)
                TerminalText("End the Day before another attempt.", color = AnsiColor.Cyan)
                TerminalMenuRow(number = 1, label = "Return to Town", onClick = {
                    onLeave(gameState.revision)
                })
            }
            DragonCombatStatus.RETREATED -> {
                TerminalText("Your Dragon attempt is spent for today.", color = AnsiColor.Cyan)
                TerminalMenuRow(number = 1, label = "Return to Town", onClick = {
                    onLeave(gameState.revision)
                })
            }
        }
    }
}

@Composable
fun DragonVictoryNoticeScreen(gameState: GameState, onContinue: (Long) -> Unit) {
    val victor = SeasonNarrator.victorName(gameState)
    TerminalSurface(frameTone = TerminalFrameTone.GOLD) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            GlassDragonCrest(sizeDp = 112)
        }
        ScreenHeader("THE GLASS DRAGON HAS FALLEN", color = AnsiColor.Yellow)
        TerminalGap(lines = 2)
        TerminalText(victor.uppercase() + " IS THE DRAGON VICTOR", color = AnsiColor.IceBlue)
        TerminalText(
            if (gameState.season.victorId == gameState.player.id) {
                "Your name passes into the oldest ledger of the realm."
            } else {
                "The realm is safe, and another adventurer's name passes into legend."
            },
            color = AnsiColor.LightGray,
        )
        TerminalGap(lines = 2)
        TerminalMenuRow(number = 1, label = "Open the Final Chronicle", onClick = {
            onContinue(gameState.revision)
        })
    }
}

@Composable
fun FinalChronicleScreen(gameState: GameState, onContinue: (Long) -> Unit) {
    TerminalSurface(frameTone = TerminalFrameTone.GOLD) {
        ScreenHeader("THE FINAL GLASS CHRONICLE", color = AnsiColor.Cyan)
        TerminalRule(color = AnsiColor.Yellow)
        TerminalGap(lines = 2)
        SeasonNarrator.finalChronicle(gameState).forEach { paragraph ->
            TerminalText(paragraph, color = AnsiColor.LightGray)
            TerminalGap(lines = 2)
        }
        TerminalMenuRow(number = 1, label = "Read the Season Record", onClick = {
            onContinue(gameState.revision)
        })
    }
}

@Composable
fun SeasonRecordScreen(
    gameState: GameState,
    onReturnToMainMenu: (Long) -> Unit,
) {
    val playerRank = CommunityRules.rankOf(gameState, gameState.player.id)
    val victor = SeasonNarrator.victorName(gameState)
    TerminalSurface(frameTone = TerminalFrameTone.GLASS) {
        ScreenHeader("SEASON RECORD", color = AnsiColor.IceBlue)
        TerminalGap(lines = 2)
        RecordLine("Days", gameState.season.resolvedDay.toString())
        RecordLine("Dragon Victor", victor, AnsiColor.Yellow)
        RecordLine("Name", gameState.player.name)
        RecordLine("Final Level", gameState.player.level.toString())
        RecordLine("Final Rank", "#" + playerRank)
        RecordLine("Carried Gold", gameState.player.gold.toString())
        RecordLine("Banked Gold", gameState.player.bankedGold.toString())
        RecordLine("Weapon", gameState.player.weapon, AnsiColor.BrightGreen)
        RecordLine("Armor", gameState.player.armor, AnsiColor.BrightGreen)
        RecordLine(
            "Challenges",
            gameState.player.challengeWins.toString() + " won / " +
                gameState.player.challengeLosses + " lost",
            AnsiColor.Magenta,
        )
        RecordLine("Dragon Attempts", gameState.season.attempts.size.toString())
        TerminalGap(lines = 2)
        TerminalText(
            if (gameState.season.victorId == gameState.player.id) {
                "You ended the season as the hero of the crystal wastes."
            } else {
                "Your adventure remains part of the season that " + victor + " brought to an end."
            },
            color = AnsiColor.LightGray,
        )
        TerminalGap(lines = 2)
        TerminalMenuRow(number = 1, label = "Return to Main Menu", onClick = {
            onReturnToMainMenu(gameState.revision)
        })
    }
}

@Composable
private fun RecordLine(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = AnsiColor.LightGray,
) {
    AnsiText(
        spans = listOf(
            AnsiSpan(label.padEnd(16, '.') + " ", AnsiColor.DimGray),
            AnsiSpan(value, color),
        ),
    )
}

private fun stageName(stage: DragonStage): String = when (stage) {
    DragonStage.WHISPERS -> "WHISPERS"
    DragonStage.SIGNS -> "SIGNS"
    DragonStage.FEAR -> "FEAR"
    DragonStage.CRISIS -> "CRISIS"
    DragonStage.CHALLENGE_WINDOW -> "THE CHALLENGE WINDOW"
    DragonStage.RESOLVED -> "RESOLVED"
}

private fun stageDescription(stage: DragonStage): String = when (stage) {
    DragonStage.WHISPERS -> "The Glass Dragon is still spoken of as an old and doubtful legend."
    DragonStage.SIGNS -> "Strange signs have appeared beyond the village roads."
    DragonStage.FEAR -> "The realm now believes the Dragon is real and active."
    DragonStage.CRISIS -> "The Dragon is growing bolder. Champions are beginning to prepare."
    DragonStage.CHALLENGE_WINDOW -> "The road to the crystal wastes is open. Someone must act."
    DragonStage.RESOLVED -> "The Dragon crisis has ended."
}

private fun stageColor(stage: DragonStage) = when (stage) {
    DragonStage.WHISPERS -> AnsiColor.DimGray
    DragonStage.SIGNS -> AnsiColor.Cyan
    DragonStage.FEAR -> AnsiColor.Yellow
    DragonStage.CRISIS -> AnsiColor.Magenta
    DragonStage.CHALLENGE_WINDOW -> AnsiColor.BrightRed
    DragonStage.RESOLVED -> AnsiColor.BrightGreen
}
