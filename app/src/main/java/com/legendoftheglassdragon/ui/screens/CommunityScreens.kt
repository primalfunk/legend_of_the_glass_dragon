package com.legendoftheglassdragon.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.legendoftheglassdragon.game.CommunityRules
import com.legendoftheglassdragon.game.ChronicleNarrator
import com.legendoftheglassdragon.game.ChronicleRules
import com.legendoftheglassdragon.game.DawnBriefRules
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.NpcAdventurer
import com.legendoftheglassdragon.game.NpcStatus
import com.legendoftheglassdragon.game.RivalryRules
import com.legendoftheglassdragon.game.WorldEventFormatter
import com.legendoftheglassdragon.game.WorldEventType
import com.legendoftheglassdragon.ui.components.AnsiSpan
import com.legendoftheglassdragon.ui.components.AnsiText
import com.legendoftheglassdragon.ui.components.TerminalGap
import com.legendoftheglassdragon.ui.components.MajorEventArt
import com.legendoftheglassdragon.ui.components.MajorEventBanner
import com.legendoftheglassdragon.ui.components.TerminalFrameTone
import com.legendoftheglassdragon.ui.components.TerminalMenuRow
import com.legendoftheglassdragon.ui.components.TerminalRule
import com.legendoftheglassdragon.ui.components.TerminalSurface
import com.legendoftheglassdragon.ui.components.TerminalText
import com.legendoftheglassdragon.ui.theme.AnsiColor

@Composable
fun GlassChronicleScreen(
    gameState: GameState,
    onContinueToDawn: (Long) -> Unit,
    onSkipToDawn: (Long) -> Unit,
    onSkipToTown: (Long) -> Unit,
) {
    val transition = requireNotNull(gameState.pendingDayTransition)
    val record = ChronicleRules.recordFor(gameState, transition.resolvedDay)
    TerminalSurface(frameTone = TerminalFrameTone.GOLD) {
        TerminalText(
            "DAY " + transition.resolvedDay + " ENDS",
            color = AnsiColor.DimGray,
            style = MaterialTheme.typography.bodyMedium,
        )
        ScreenHeader("THE GLASS CHRONICLE", color = AnsiColor.Cyan)
        TerminalText(
            "Day " + transition.resolvedDay + " passes into history...",
            color = AnsiColor.Yellow,
        )
        TerminalRule(color = AnsiColor.BrightBlue)
        TerminalGap(lines = 2)
        if (record == null || record.events.isEmpty()) {
            TerminalText(
                "The glass reveals no deed weighty enough for the permanent record.",
                color = AnsiColor.DimGray,
            )
        } else {
            record.events.forEach { event ->
                TerminalText(
                    "◆ " + ChronicleNarrator.format(event, gameState),
                    color = chronicleColor(event.type, event.importance),
                )
                TerminalGap(lines = 2)
            }
        }
        TerminalGap(lines = 2)
        TerminalMenuRow(
            number = 1,
            label = "Continue to Dawn Brief",
            onClick = { onContinueToDawn(gameState.revision) },
        )
        TerminalMenuRow(
            number = 2,
            label = "Skip Chronicle to Dawn",
            onClick = { onSkipToDawn(gameState.revision) },
        )
        TerminalMenuRow(
            number = 0,
            label = "Skip Transition to Village",
            onClick = { onSkipToTown(gameState.revision) },
        )
    }
}

@Composable
fun DawnBriefScreen(
    gameState: GameState,
    onEnterTown: (Long) -> Unit,
    onReadPublicNews: (Long) -> Unit,
) {
    val transition = requireNotNull(gameState.pendingDayTransition)
    val record = ChronicleRules.recordFor(gameState, transition.resolvedDay)
    val visibleEvents = record?.let { DawnBriefRules.visibleEvents(it, gameState) }.orEmpty()
    val playerRank = CommunityRules.rankOf(gameState, gameState.player.id)
    TerminalSurface(frameTone = TerminalFrameTone.GLASS) {
        MajorEventBanner(MajorEventArt.NEW_DAY)
        ScreenHeader("DAWN BRIEF — DAY " + transition.nextDay, color = AnsiColor.Cyan)
        TerminalGap()
        TerminalText("You awaken to another day in the realm.", color = AnsiColor.LightGray)
        TerminalRule(color = AnsiColor.BrightBlue)
        AnsiText(
            spans = listOf(
                AnsiSpan("Health....... ", AnsiColor.DimGray),
                AnsiSpan(
                    gameState.player.currentHp.toString() + " / " + gameState.player.maximumHp,
                    AnsiColor.BrightGreen,
                    bold = true,
                ),
            ),
        )
        AnsiText(
            spans = listOf(
                AnsiSpan("Forest fights. ", AnsiColor.DimGray),
                AnsiSpan(gameState.playerDay.fightsRemaining.toString(), AnsiColor.Yellow),
            ),
        )
        AnsiText(
            spans = listOf(
                AnsiSpan("Challenges.... ", AnsiColor.DimGray),
                AnsiSpan(gameState.playerDay.challengesRemaining.toString(), AnsiColor.Yellow),
            ),
        )
        AnsiText(
            spans = listOf(
                AnsiSpan("Current rank.. ", AnsiColor.DimGray),
                AnsiSpan("#" + playerRank, AnsiColor.Cyan, bold = true),
            ),
        )
        TerminalGap(lines = 2)
        TerminalText("PUBLIC NOTICES", color = AnsiColor.Yellow)
        TerminalRule(color = AnsiColor.DimGray)
        if (visibleEvents.isEmpty()) {
            TerminalText("No urgent notice awaits you this morning.", color = AnsiColor.DimGray)
        } else {
            visibleEvents.forEach { event ->
                TerminalText(
                    "• " + DawnBriefRules.format(event, gameState),
                    color = chronicleColor(event.type, event.importance),
                )
                TerminalGap()
            }
        }
        TerminalGap(lines = 2)
        TerminalMenuRow(
            number = 1,
            label = "Enter the Village Square",
            onClick = { onEnterTown(gameState.revision) },
        )
        TerminalMenuRow(
            number = 2,
            label = "Read Public Realm News",
            onClick = { onReadPublicNews(gameState.revision) },
        )
    }
}

@Composable
fun RankingsScreen(
    gameState: GameState,
    onInspectNpc: (String) -> Unit,
    onReturnToTown: () -> Unit,
) {
    val rankings = CommunityRules.rankings(gameState)
    TerminalSurface {
        ScreenHeader("HEROES OF THE REALM", color = AnsiColor.Yellow)
        TerminalGap()
        TerminalText(
            "Ranked by level, experience, and the ancient ledger.",
            color = AnsiColor.DimGray,
            style = MaterialTheme.typography.bodyMedium,
        )
        TerminalGap()
        rankings.forEachIndexed { index, entry ->
            val rank = index + 1
            if (entry.isPlayer) {
                AnsiText(
                    spans = listOf(
                        AnsiSpan(rank.toString().padStart(2) + ". ", AnsiColor.Yellow, bold = true),
                        AnsiSpan(entry.name.padEnd(14, '.'), AnsiColor.BrightBlue, bold = true),
                        AnsiSpan(" Level " + entry.level + "  < YOU", AnsiColor.Cyan),
                    ),
                )
                TerminalGap()
            } else {
                TerminalMenuRow(
                    number = rank,
                    label = entry.name.padEnd(14, '.') + " Level " + entry.level,
                    note = when {
                        entry.characterId in gameState.season.declaredChallengerIds -> "<DRAGON CHAMPION>"
                        entry.npcStatus == NpcStatus.RECOVERING -> "<RECOVERING>"
                        else -> null
                    },
                    onClick = { onInspectNpc(entry.characterId) },
                )
            }
        }
        TerminalGap()
        TerminalMenuRow(number = 0, label = "Return to Town", onClick = onReturnToTown)
    }
}

@Composable
fun NpcProfileScreen(
    gameState: GameState,
    npc: NpcAdventurer,
    rank: Int,
    onChallenge: (String, Long) -> Unit,
    onReturn: () -> Unit,
) {
    val atInn = CommunityRules.innVisitors(gameState).any { it.id == npc.id }
    val canChallenge = atInn && npc.status == NpcStatus.ACTIVE &&
        gameState.playerDay.challengesRemaining > 0 && !gameState.playerDay.wasDefeatedToday
    TerminalSurface {
        ScreenHeader(npc.name.uppercase(), color = AnsiColor.Magenta)
        TerminalGap(lines = 2)
        ProfileLine("Level", npc.level.toString())
        ProfileLine("Rank", rank.toString(), AnsiColor.Yellow)
        ProfileLine("Experience", npc.experience.toString())
        ProfileLine("Attack", npc.attack.toString())
        ProfileLine("Defense", npc.defense.toString())
        ProfileLine("Weapon", npc.weapon, AnsiColor.BrightGreen)
        ProfileLine("Armor", npc.armor, AnsiColor.BrightGreen)
        ProfileLine(
            "Status",
            if (npc.status == NpcStatus.ACTIVE) "Adventuring" else "Recovering",
            if (npc.status == NpcStatus.ACTIVE) AnsiColor.Cyan else AnsiColor.BrightRed,
        )
        ProfileLine("Familiarity", npc.familiarity.toString(), AnsiColor.Cyan)
        ProfileLine("Attitude", RivalryRules.label(npc.rivalry), AnsiColor.Magenta)
        ProfileLine(
            "Challenges",
            "${npc.challengeWins} wins / ${npc.challengeLosses} losses",
            AnsiColor.Yellow,
        )
        ProfileLine(
            "Against You",
            "${npc.playerLossesAgainst} wins / ${npc.playerWinsAgainst} losses",
            AnsiColor.Cyan,
        )
        if (npc.id in gameState.season.declaredChallengerIds) {
            ProfileLine("Dragon Intent", "Declared Champion", AnsiColor.BrightRed)
        }
        TerminalGap(lines = 2)
        TerminalText(
            when {
                rank == 1 -> npc.name + " is presently the most renowned adventurer in the realm."
                npc.status == NpcStatus.RECOVERING ->
                    npc.name + " is expected to return after a day of recovery."
                npc.level >= 3 -> npc.name + " has become a seasoned name around the village."
                else -> npc.name + " is still writing the opening chapters of an adventure."
            },
            color = AnsiColor.LightGray,
        )
        TerminalGap(lines = 2)
        TerminalMenuRow(
            number = 1,
            label = "Challenge " + npc.name,
            enabled = canChallenge,
            note = when {
                npc.status != NpcStatus.ACTIVE -> "<RECOVERING>"
                !atInn -> "<NOT AT THE INN>"
                gameState.playerDay.wasDefeatedToday -> "<YOU MUST RECOVER>"
                gameState.playerDay.challengesRemaining == 0 -> "<USED TODAY>"
                else -> null
            },
            onClick = { onChallenge(npc.id, gameState.revision) },
        )
        TerminalMenuRow(number = 2, label = "Return", onClick = onReturn)
    }
}

@Composable
fun InnScreen(
    gameState: GameState,
    onGossip: () -> Unit,
    onNews: () -> Unit,
    onWhoIsHere: (Long) -> Unit,
    onTalk: (Long) -> Unit,
    onChallenge: () -> Unit,
    onReturnToTown: () -> Unit,
) {
    TerminalSurface {
        ScreenHeader("THE DRAGON'S REST", color = AnsiColor.Yellow)
        TerminalGap(lines = 2)
        TerminalText(
            "The room is thick with smoke, ale, and exaggerated stories.",
            color = AnsiColor.LightGray,
        )
        TerminalGap()
        TerminalText(
            CommunityRules.INN_VISITOR_COUNT.toString() + " familiar figures occupy the common room today.",
            color = AnsiColor.Cyan,
        )
        TerminalGap(lines = 2)
        TerminalMenuRow(number = 1, label = "Hear the Latest Gossip", onClick = onGossip)
        TerminalMenuRow(number = 2, label = "Read Realm News", onClick = onNews)
        TerminalMenuRow(
            number = 3,
            label = "See Who Is Here",
            onClick = { onWhoIsHere(gameState.revision) },
        )
        TerminalMenuRow(
            number = 4,
            label = "Talk to an Adventurer",
            onClick = { onTalk(gameState.revision) },
        )
        TerminalMenuRow(number = 5, label = "Challenge an Adventurer", onClick = onChallenge)
        TerminalMenuRow(number = 6, label = "Return to Town", onClick = onReturnToTown)
    }
}

@Composable
fun GossipScreen(
    gameState: GameState,
    onReturnToInn: () -> Unit,
) {
    TerminalSurface {
        ScreenHeader("LATEST GOSSIP", color = AnsiColor.Magenta)
        TerminalGap(lines = 2)
        WorldEventFormatter.gossip(gameState).forEach { line ->
            TerminalText(line, color = AnsiColor.LightGray)
            TerminalGap(lines = 2)
        }
        TerminalMenuRow(number = 1, label = "Return to the Inn", onClick = onReturnToInn)
    }
}

@Composable
fun RealmNewsScreen(
    gameState: GameState,
    onReturnToInn: () -> Unit,
) {
    val events = gameState.community.recentEvents.sortedWith(
        compareByDescending<com.legendoftheglassdragon.game.WorldEvent> { it.day }
            .thenByDescending { it.importance },
    )
    TerminalSurface {
        ScreenHeader("REALM NEWS", color = AnsiColor.Cyan)
        TerminalGap()
        if (events.isEmpty()) {
            TerminalText("No realm news has yet been entered into the ledger.", color = AnsiColor.DimGray)
        } else {
            var displayedDay: Int? = null
            events.forEach { event ->
                if (displayedDay != event.day) {
                    displayedDay = event.day
                    TerminalGap()
                    TerminalText("DAY " + event.day, color = AnsiColor.Yellow)
                    TerminalRule(color = AnsiColor.DimGray)
                }
                TerminalText(
                    "* " + WorldEventFormatter.format(event, gameState),
                    color = if (event.importance == 3) AnsiColor.BrightGreen else AnsiColor.LightGray,
                )
                TerminalGap()
            }
        }
        TerminalGap(lines = 2)
        TerminalMenuRow(number = 1, label = "Return to the Inn", onClick = onReturnToInn)
    }
}

@Composable
fun WhoIsHereScreen(
    gameState: GameState,
    onInspectNpc: (String) -> Unit,
    onReturnToInn: () -> Unit,
) {
    val visitors = CommunityRules.innVisitors(gameState)
    TerminalSurface {
        ScreenHeader("WHO'S HERE?", color = AnsiColor.BrightBlue)
        TerminalGap(lines = 2)
        TerminalText("Today's company at the Dragon's Rest:", color = AnsiColor.LightGray)
        TerminalGap()
        visitors.forEachIndexed { index, npc ->
            TerminalMenuRow(
                number = index + 1,
                label = npc.name.padEnd(14, '.') + " Level " + npc.level,
                note = if (npc.status == NpcStatus.RECOVERING) "<RECOVERING>" else null,
                onClick = { onInspectNpc(npc.id) },
            )
        }
        TerminalGap()
        TerminalText(
            "Meeting these adventurers has been recorded in your local ledger.",
            color = AnsiColor.DimGray,
            style = MaterialTheme.typography.bodyMedium,
        )
        TerminalGap()
        TerminalMenuRow(number = 0, label = "Return to the Inn", onClick = onReturnToInn)
    }
}

private fun chronicleColor(type: WorldEventType, importance: Int) = when {
    type == WorldEventType.DEFEAT -> AnsiColor.BrightRed
    type == WorldEventType.CHALLENGE -> AnsiColor.Magenta
    importance >= 3 -> AnsiColor.Yellow
    type == WorldEventType.RECOVERY -> AnsiColor.BrightGreen
    else -> AnsiColor.LightGray
}

@Composable
private fun ProfileLine(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = AnsiColor.LightGray,
) {
    val dots = ".".repeat((12 - label.length).coerceAtLeast(1))
    AnsiText(
        spans = listOf(
            AnsiSpan(label + dots + " ", AnsiColor.DimGray),
            AnsiSpan(value, color),
        ),
    )
}
