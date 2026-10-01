package com.legendoftheglassdragon.game

data class RankingEntry(
    val characterId: String,
    val name: String,
    val level: Int,
    val experience: Int,
    val isPlayer: Boolean,
    val npcStatus: NpcStatus? = null,
)

object CommunityRules {
    const val MAX_DAILY_EVENTS = 6
    const val MAX_RETAINED_EVENTS = 84
    const val NEWS_RETENTION_DAYS = 14
    const val INN_VISITOR_COUNT = 4
    const val MAX_FAMILIARITY = 100

    fun rankings(state: GameState): List<RankingEntry> =
        rankings(state.player, state.community.adventurers)

    fun rankings(
        player: PlayerCharacter,
        adventurers: List<NpcAdventurer>,
    ): List<RankingEntry> {
        val entries = buildList {
            add(
                RankingEntry(
                    characterId = player.id,
                    name = player.name,
                    level = player.level,
                    experience = player.experience,
                    isPlayer = true,
                ),
            )
            adventurers.forEach { npc ->
                add(
                    RankingEntry(
                        characterId = npc.id,
                        name = npc.name,
                        level = npc.level,
                        experience = npc.experience,
                        isPlayer = false,
                        npcStatus = npc.status,
                    ),
                )
            }
        }
        return entries.sortedWith(
            compareByDescending<RankingEntry> { it.level }
                .thenByDescending { it.experience }
                .thenBy { it.characterId },
        )
    }

    fun rankOf(state: GameState, characterId: String): Int =
        rankings(state).indexOfFirst { it.characterId == characterId } + 1

    fun innVisitors(state: GameState): List<NpcAdventurer> {
        val sorted = state.community.adventurers.sortedWith(
            compareByDescending<NpcAdventurer> { it.status == NpcStatus.RECOVERING }
                .thenBy { it.id },
        )
        if (sorted.isEmpty()) return emptyList()
        val start = Math.floorMod(
            state.world.seed + state.currentDay * 31L,
            sorted.size.toLong(),
        ).toInt()
        return List(minOf(INN_VISITOR_COUNT, sorted.size)) { offset ->
            sorted[(start + offset) % sorted.size]
        }
    }
}

object RivalryRules {
    fun label(value: Int): String = when {
        value >= 18 -> "Fierce Rival"
        value >= 8 -> "Rival"
        value >= 3 -> "Competitive"
        else -> "Acquaintance"
    }
}

object CommunityActions {
    fun advanceToDawnBrief(
        state: GameState,
        expectedRevision: Long,
    ): GameActionResult {
        if (state.revision != expectedRevision) {
            return GameActionResult.Rejected(GameActionRejection.StaleState)
        }
        val transition = state.pendingDayTransition
            ?: return GameActionResult.Rejected(GameActionRejection.NoPendingDayTransition)
        if (transition.phase != DayTransitionPhase.CHRONICLE) {
            return GameActionResult.Rejected(GameActionRejection.InvalidDayTransitionPhase)
        }
        return GameActionResult.Applied(
            state.copy(
                revision = state.revision + 1,
                pendingDayTransition = transition.copy(phase = DayTransitionPhase.DAWN_BRIEF),
            ),
        )
    }

    fun completeDayTransition(
        state: GameState,
        expectedRevision: Long,
    ): GameActionResult {
        if (state.revision != expectedRevision) {
            return GameActionResult.Rejected(GameActionRejection.StaleState)
        }
        if (state.pendingDayTransition == null) {
            return GameActionResult.Rejected(GameActionRejection.NoPendingDayTransition)
        }
        return GameActionResult.Applied(
            state.copy(
                revision = state.revision + 1,
                pendingDayTransition = null,
            ),
        )
    }

    fun recordInnMeeting(
        state: GameState,
        expectedRevision: Long,
        visitorIds: Set<String>,
    ): GameActionResult {
        if (state.revision != expectedRevision) {
            return GameActionResult.Rejected(GameActionRejection.StaleState)
        }
        var changed = false
        val updated = state.community.adventurers.map { npc ->
            if (npc.id in visitorIds && npc.lastMetDay < state.currentDay) {
                changed = true
                npc.copy(
                    familiarity = (npc.familiarity + 1).coerceAtMost(CommunityRules.MAX_FAMILIARITY),
                    lastMetDay = state.currentDay,
                )
            } else {
                npc
            }
        }
        if (!changed) {
            return GameActionResult.Rejected(GameActionRejection.NoCommunityChange)
        }
        return GameActionResult.Applied(
            state.copy(
                revision = state.revision + 1,
                community = state.community.copy(adventurers = updated),
            ),
        )
    }
}

object WorldEventFormatter {
    fun format(event: WorldEvent, state: GameState): String {
        val subject = characterName(event.characterId, state)
        val related = event.relatedCharacterId?.let { characterName(it, state) }
        return when (event.type) {
            WorldEventType.LEVEL_GAIN ->
                subject + " reached Level " + event.value + "!"
            WorldEventType.DEFEAT ->
                subject + " was carried from the forest after a crushing defeat."
            WorldEventType.RECOVERY ->
                subject + " returned to adventuring after recovering."
            WorldEventType.EQUIPMENT ->
                subject + " purchased " + (event.detail ?: "new equipment") + "."
            WorldEventType.GREAT_SUCCESS ->
                subject + " returned from the forest with " + event.value + " gold."
            WorldEventType.SOCIAL ->
                subject + " held court at the Dragon's Rest."
            WorldEventType.RANK_CHANGE -> when {
                event.newRank == 1 -> subject + " has taken the #1 position!"
                event.characterId == state.player.id ->
                    "You moved from rank #" + event.previousRank + " to #" + event.newRank + "."
                related == state.player.name ->
                    subject + " has overtaken you in the rankings."
                else ->
                    subject + " moved to rank #" + event.newRank + "."
            }
            WorldEventType.CHALLENGE -> when (event.detail) {
                "PLAYER_WIN" -> subject + " defeated " + (related ?: "an adventurer") +
                    " in a challenge at the Inn!"
                "PLAYER_LOSS" -> subject + " bested " + (related ?: "an adventurer") +
                    " in a challenge."
                "PLAYER_YIELD" -> subject + " forced " + (related ?: "an adventurer") +
                    " to yield."
                else -> subject + " defeated " + (related ?: "an adventurer") +
                    " in a challenge."
            }
            WorldEventType.DRAGON_INCIDENT,
            WorldEventType.DRAGON_INTENT,
            WorldEventType.DRAGON_ATTEMPT,
            WorldEventType.DRAGON_VICTORY,
            WorldEventType.DRAGON_SUPPORT,
            -> DragonEventNarrator.dawn(event, state)
        }
    }

    fun gossip(state: GameState): List<String> {
        val rankings = CommunityRules.rankings(state)
        val leader = rankings.first()
        val recovering = state.community.adventurers.firstOrNull { it.status == NpcStatus.RECOVERING }
        val bestEquipped = state.community.adventurers.maxByOrNull { npc ->
            EquipmentCatalog.requireWeapon(npc.weaponId).tier +
                EquipmentCatalog.requireArmor(npc.armorId).tier
        }
        val rival = state.community.adventurers.maxByOrNull { it.rivalry }
            ?.takeIf { it.rivalry >= 3 }
        val latestChallenge = state.community.recentEvents
            .filter { it.type == WorldEventType.CHALLENGE }
            .maxByOrNull { it.day }
        val latestDragonEvent = state.community.recentEvents
            .filter {
                it.type in setOf(
                    WorldEventType.DRAGON_INCIDENT,
                    WorldEventType.DRAGON_INTENT,
                    WorldEventType.DRAGON_ATTEMPT,
                    WorldEventType.DRAGON_VICTORY,
                )
            }
            .maxByOrNull { it.day }
        val declaredChampion = state.season.declaredChallengerIds.firstNotNullOfOrNull { id ->
            state.community.adventurers.firstOrNull { it.id == id }
        }
        val playerRank = rankings.indexOfFirst { it.isPlayer } + 1
        return buildList {
            add("'" + leader.name + " has everyone talking. Rank one changes a person.'")
            recovering?.let {
                add("'" + it.name + " is still nursing wounds from the forest.'")
            }
            bestEquipped?.let {
                add("'" + it.name + " has been spending freely on " + it.weapon + " and " + it.armor + ".'")
            }
            latestChallenge?.let { event ->
                add("'" + format(event, state) + " The whole Inn is still arguing about it.'")
            }
            rival?.let {
                add("'" + it.name + " and " + state.player.name + " have become " +
                    RivalryRules.label(it.rivalry).lowercase() + "s.'")
            }
            latestDragonEvent?.let {
                add("'" + DragonEventNarrator.dawn(it, state) + " That's what everyone says, anyway.'")
            }
            declaredChampion?.let {
                add("'" + it.name + " is buying steel and talking about the Dragon. Draw your own conclusion.'")
            }
            add("'They say " + state.player.name + " now stands at rank #" + playerRank + ".'")
        }
    }

    private fun characterName(characterId: String, state: GameState): String =
        if (characterId == state.player.id) {
            state.player.name
        } else {
            state.community.adventurers.firstOrNull { it.id == characterId }?.name ?: "An adventurer"
        }
}
