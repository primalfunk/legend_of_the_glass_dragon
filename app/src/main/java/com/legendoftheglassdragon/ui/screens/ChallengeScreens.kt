package com.legendoftheglassdragon.ui.screens

import androidx.compose.runtime.Composable
import com.legendoftheglassdragon.game.ChallengeState
import com.legendoftheglassdragon.game.ChallengeStatus
import com.legendoftheglassdragon.game.CombatMessageKind
import com.legendoftheglassdragon.game.CommunityRules
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.NpcAdventurer
import com.legendoftheglassdragon.game.NpcStatus
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
fun ChallengeListScreen(
    gameState: GameState,
    onChallenge: (String, Long) -> Unit,
    onReturnToInn: () -> Unit,
) {
    val visitors = CommunityRules.innVisitors(gameState)
    val opportunityAvailable = gameState.playerDay.challengesRemaining > 0 &&
        !gameState.playerDay.wasDefeatedToday
    TerminalSurface {
        ScreenHeader("CHALLENGE AN ADVENTURER", color = AnsiColor.BrightRed)
        TerminalGap()
        TerminalText(
            "Challenges remaining today: " + gameState.playerDay.challengesRemaining,
            color = AnsiColor.Yellow,
        )
        TerminalText("Choose your opponent carefully.", color = AnsiColor.LightGray)
        TerminalGap(lines = 2)
        visitors.forEachIndexed { index, npc ->
            val eligible = opportunityAvailable && npc.status == NpcStatus.ACTIVE
            TerminalMenuRow(
                number = index + 1,
                label = npc.name + "  L" + npc.level + "  " + npc.weapon,
                enabled = eligible,
                note = when {
                    npc.status != NpcStatus.ACTIVE -> "<RECOVERING>"
                    !opportunityAvailable -> "<NO CHALLENGES>"
                    else -> null
                },
                onClick = { onChallenge(npc.id, gameState.revision) },
            )
        }
        TerminalGap()
        TerminalMenuRow(number = 0, label = "Return to the Inn", onClick = onReturnToInn)
    }
}

@Composable
fun ChallengeScreen(
    gameState: GameState,
    challenge: ChallengeState,
    opponent: NpcAdventurer,
    onAttack: (Long) -> Unit,
    onYield: (Long) -> Unit,
    onReturnToInn: (Long) -> Unit,
) {
    val frameTone = when (challenge.status) {
        ChallengeStatus.PLAYER_VICTORY -> TerminalFrameTone.GOLD
        ChallengeStatus.ACTIVE, ChallengeStatus.PLAYER_DEFEAT -> TerminalFrameTone.DANGER
        ChallengeStatus.YIELDED, ChallengeStatus.REFUSED -> TerminalFrameTone.GLASS
    }
    TerminalSurface(frameTone = frameTone) {
        when (challenge.status) {
            ChallengeStatus.PLAYER_VICTORY -> MajorEventBanner(MajorEventArt.VICTORY)
            ChallengeStatus.PLAYER_DEFEAT -> MajorEventBanner(MajorEventArt.DEFEAT)
            else -> Unit
        }
        ScreenHeader(
            title = when (challenge.status) {
                ChallengeStatus.ACTIVE -> "A CHALLENGE!"
                ChallengeStatus.PLAYER_VICTORY -> "CHALLENGE WON"
                ChallengeStatus.PLAYER_DEFEAT -> "CHALLENGE LOST"
                ChallengeStatus.YIELDED -> "YOU YIELDED"
                ChallengeStatus.REFUSED -> "CHALLENGE DECLINED"
            },
            color = when (challenge.status) {
                ChallengeStatus.PLAYER_VICTORY -> AnsiColor.Yellow
                ChallengeStatus.PLAYER_DEFEAT -> AnsiColor.BrightRed
                ChallengeStatus.YIELDED -> AnsiColor.Cyan
                ChallengeStatus.REFUSED -> AnsiColor.DimGray
                ChallengeStatus.ACTIVE -> AnsiColor.Magenta
            },
        )
        TerminalGap()
        challenge.recentMessages.forEach { message ->
            TerminalText(
                message.text,
                color = when (message.kind) {
                    CombatMessageKind.PLAYER_ACTION -> AnsiColor.BrightGreen
                    CombatMessageKind.MONSTER_ACTION, CombatMessageKind.DEFEAT -> AnsiColor.BrightRed
                    CombatMessageKind.VICTORY -> AnsiColor.Yellow
                    CombatMessageKind.ESCAPE -> AnsiColor.Cyan
                    else -> AnsiColor.LightGray
                },
            )
            TerminalGap()
        }
        if (challenge.status == ChallengeStatus.ACTIVE) {
            TerminalPanel {
                TerminalText(
                    gameState.player.name + "  Level " + gameState.player.level,
                    color = AnsiColor.IceBlue,
                )
                TerminalText(
                    "HP: " + gameState.player.currentHp + " / " + gameState.player.maximumHp,
                    color = AnsiColor.LightGray,
                )
            }
            TerminalGap()
            TerminalPanel(tone = TerminalFrameTone.DANGER) {
                TerminalText(opponent.name + "  Level " + opponent.level, color = AnsiColor.Magenta)
                TerminalText(
                    "HP: " + challenge.opponentCurrentHp + " / " + opponent.maximumHp,
                    color = AnsiColor.LightGray,
                )
                TerminalText("Weapon: " + opponent.weapon, color = AnsiColor.DimGray)
                TerminalText("Armor:  " + opponent.armor, color = AnsiColor.DimGray)
            }
            TerminalGap(lines = 2)
            TerminalMenuRow(number = 1, label = "Attack", onClick = { onAttack(gameState.revision) })
            TerminalMenuRow(number = 2, label = "Yield", onClick = { onYield(gameState.revision) })
        } else {
            when (challenge.status) {
                ChallengeStatus.PLAYER_VICTORY -> {
                    TerminalText("Experience gained: " + challenge.experienceReward, color = AnsiColor.Cyan)
                    TerminalText("Gold won:         " + challenge.goldTransferred, color = AnsiColor.Yellow)
                }
                ChallengeStatus.PLAYER_DEFEAT -> {
                    TerminalText("Gold lost: " + challenge.goldTransferred, color = AnsiColor.Yellow)
                    TerminalText("Your Forest day remains available.", color = AnsiColor.BrightGreen)
                }
                ChallengeStatus.YIELDED ->
                    TerminalText("No reward was earned.", color = AnsiColor.DimGray)
                ChallengeStatus.REFUSED ->
                    TerminalText("Your daily challenge remains available.", color = AnsiColor.Cyan)
                ChallengeStatus.ACTIVE -> Unit
            }
            TerminalGap()
            TerminalText(
                "Your challenge record: " + gameState.player.challengeWins + " wins / " +
                    gameState.player.challengeLosses + " losses",
                color = AnsiColor.LightGray,
            )
            TerminalGap(lines = 2)
            TerminalMenuRow(
                number = 1,
                label = "Return to the Inn",
                onClick = { onReturnToInn(gameState.revision) },
            )
        }
    }
}
