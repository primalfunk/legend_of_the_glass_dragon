package com.legendoftheglassdragon.game

import java.lang.Math.floorMod

object ChronicleRules {
    const val MAX_ITEMS_PER_DAY = 7
    const val MAX_HISTORY_DAYS = 40
    const val MAX_DAWN_EVENT_ITEMS = 4

    fun selectChronicleEvents(
        events: List<WorldEvent>,
        playerId: String,
    ): List<WorldEvent> {
        val ranked = events
            .distinctBy { it.id }
            .filter(::isChronicleWorthy)
            .sortedWith(eventOrdering(playerId))

        val majorCount = ranked.count { it.importance >= 3 }
        val targetSize = when {
            ranked.any { it.type == WorldEventType.DRAGON_VICTORY } -> 7
            majorCount >= 8 -> 6
            majorCount >= 4 -> 5
            majorCount >= 1 -> 4
            else -> 3
        }
        // Importance and continuity create the recurring cast; no representation quota forces
        // every resident into the record. Quiet days therefore remain visibly quiet.
        return ranked.take(minOf(targetSize, MAX_ITEMS_PER_DAY))
    }

    private fun isChronicleWorthy(event: WorldEvent): Boolean =
        event.importance >= 3 ||
            event.relatedPriorEventId != null ||
            event.directlyAffectsPlayer ||
            event.type == WorldEventType.RANK_CHANGE ||
            event.type.name.startsWith("DRAGON_") ||
            (event.type == WorldEventType.GREAT_SUCCESS && event.value >= 30)

    fun selectPublicNews(events: List<WorldEvent>, playerId: String): List<WorldEvent> =
        events
            .distinctBy { it.id }
            .filter { it.visibility != WorldEventVisibility.CHRONICLE_ONLY && it.importance >= 2 }
            .sortedWith(eventOrdering(playerId))
            .take(CommunityRules.MAX_DAILY_EVENTS)

    fun appendRecord(
        history: List<ChronicleRecord>,
        record: ChronicleRecord,
    ): List<ChronicleRecord> = (history.filterNot { it.day == record.day } + record)
        .sortedBy { it.day }
        .takeLast(MAX_HISTORY_DAYS)

    fun recordFor(state: GameState, day: Int): ChronicleRecord? =
        state.community.chronicleHistory.firstOrNull { it.day == day }

    private fun eventOrdering(playerId: String): Comparator<WorldEvent> =
        compareByDescending<WorldEvent> { it.importance }
            .thenByDescending { it.relatedPriorEventId != null || it.relationshipDelta != 0 }
            .thenByDescending { it.directlyAffectsPlayer || it.characterId == playerId }
            .thenByDescending { typePriority(it.type) }
            .thenBy { it.id }

    private fun typePriority(type: WorldEventType): Int = when (type) {
        WorldEventType.CHALLENGE -> 8
        WorldEventType.DRAGON_VICTORY -> 13
        WorldEventType.DRAGON_ATTEMPT -> 12
        WorldEventType.DRAGON_INTENT -> 11
        WorldEventType.DRAGON_INCIDENT -> 10
        WorldEventType.DRAGON_SUPPORT -> 9
        WorldEventType.LEVEL_GAIN -> 7
        WorldEventType.DEFEAT -> 6
        WorldEventType.RANK_CHANGE -> 5
        WorldEventType.EQUIPMENT -> 4
        WorldEventType.RECOVERY -> 3
        WorldEventType.GREAT_SUCCESS -> 2
        WorldEventType.SOCIAL -> 1
    }
}

object WorldEventLedger {
    fun append(state: GameState, event: WorldEvent): GameState = state.copy(
        community = state.community.copy(
            recentEvents = (state.community.recentEvents + event)
                .distinctBy { it.id }
                .takeLast(CommunityRules.MAX_RETAINED_EVENTS),
        ),
    )
}

object ChronicleNarrator {
    fun format(event: WorldEvent, state: GameState): String {
        val subject = characterName(event.characterId, state)
        val related = event.relatedCharacterId?.let { characterName(it, state) }
        return when (event.type) {
            WorldEventType.LEVEL_GAIN -> choose(
                event,
                subject + " completed Physical Training and attained Level " + event.value + ".",
                "The Training Hall bells rang for " + subject + ", newly raised to Level " + event.value + ".",
                subject + " climbed another rung of the adventurers' ladder: Level " + event.value + ".",
            )

            WorldEventType.DEFEAT -> if (event.characterId == state.player.id) {
                state.player.name + " was carried home from the forest. The day's road ended in defeat."
            } else {
                choose(
                    event,
                    subject + " returned from the " + locationName(event.location) + " badly wounded and considerably poorer.",
                    "The " + locationName(event.location) + " sent " + subject + " limping back toward town before nightfall.",
                    subject + "'s expedition ended in darkness; healers say another attempt will have to wait.",
                )
            }

            WorldEventType.RECOVERY -> choose(
                event,
                subject + " returned to adventuring after a day in recovery.",
                subject + " left the healer's care and took up arms again.",
                "Rest restored " + subject + ", who is once more abroad in the realm.",
            )

            WorldEventType.EQUIPMENT -> choose(
                event,
                subject + " acquired " + (event.detail ?: "new equipment") + ".",
                "The smith's ledger records " + (event.detail ?: "a new purchase") + " in " + subject + "'s name.",
                subject + " now carries " + (event.detail ?: "newly forged gear") + ".",
            )

            WorldEventType.GREAT_SUCCESS -> choose(
                event,
                subject + " returned from a secretive expedition with " + event.value + " gold.",
                "Fortune favored " + subject + ": " + event.value + " gold changed hands before dawn.",
                subject + " enjoyed an unusually profitable day beyond the village roads.",
            )

            WorldEventType.SOCIAL -> choose(
                event,
                subject + " held court at the Dragon's Rest until the lamps burned low.",
                "Half the Inn gathered around one of " + subject + "'s increasingly unlikely stories.",
                subject + " spent the evening trading rumors at the Dragon's Rest.",
            )

            WorldEventType.RANK_CHANGE -> when {
                event.newRank == 1 -> subject + " seized the #1 place in the realm's ledger."
                event.characterId == state.player.id ->
                    state.player.name + " moved from rank #" + event.previousRank + " to #" + event.newRank + "."
                related == state.player.name ->
                    subject + " overtook " + state.player.name + " and now holds rank #" + event.newRank + "."
                else -> subject + " advanced to rank #" + event.newRank + "."
            }

            WorldEventType.CHALLENGE -> challengeText(event, subject, related, state)
            WorldEventType.DRAGON_INCIDENT,
            WorldEventType.DRAGON_INTENT,
            WorldEventType.DRAGON_ATTEMPT,
            WorldEventType.DRAGON_VICTORY,
            WorldEventType.DRAGON_SUPPORT,
            -> DragonEventNarrator.chronicle(event, state)
        }
    }

    private fun challengeText(
        event: WorldEvent,
        winner: String,
        loser: String?,
        state: GameState,
    ): String {
        val defeated = loser ?: "another adventurer"
        val base = choose(
            event,
            winner + " defeated " + defeated + " in a challenge at the Inn.",
            winner + " sent " + defeated + " from the dueling floor with wounded pride.",
            "Steel and boasts settled the matter: " + winner + " bested " + defeated + ".",
        )
        val npc = listOfNotNull(event.characterId, event.relatedCharacterId)
            .firstNotNullOfOrNull { id -> state.community.adventurers.firstOrNull { it.id == id } }
        return when {
            npc != null && npc.playerWinsAgainst + npc.playerLossesAgainst >= 3 ->
                base + " It was the latest chapter in a rivalry the Inn now knows by heart."
            npc != null && npc.rivalry >= 8 ->
                base + " The rivalry between them has hardened."
            event.relatedPriorEventId != null ->
                base + " Old words were plainly not forgotten."
            else -> base
        }
    }

    private fun choose(event: WorldEvent, vararg variants: String): String =
        variants[floorMod(event.id.hashCode(), variants.size)]

    private fun characterName(characterId: String, state: GameState): String =
        if (characterId == state.player.id) state.player.name
        else state.community.adventurers.firstOrNull { it.id == characterId }?.name ?: "An adventurer"

    private fun locationName(location: WorldEventLocation): String = when (location) {
        WorldEventLocation.FOREST -> "forest"
        WorldEventLocation.INN -> "Inn"
        WorldEventLocation.TRAINING_HALL -> "Training Hall"
        WorldEventLocation.MARKET -> "market"
        WorldEventLocation.VILLAGE -> "village"
        WorldEventLocation.UNKNOWN -> "wilds"
    }
}

object DawnBriefRules {
    fun visibleEvents(record: ChronicleRecord, state: GameState): List<WorldEvent> =
        record.events
            .filter { it.visibility != WorldEventVisibility.CHRONICLE_ONLY }
            .filter {
                it.directlyAffectsPlayer ||
                    it.characterId == state.player.id ||
                    it.relatedCharacterId == state.player.id ||
                    it.type in setOf(
                        WorldEventType.LEVEL_GAIN,
                        WorldEventType.DEFEAT,
                        WorldEventType.RECOVERY,
                        WorldEventType.RANK_CHANGE,
                        WorldEventType.CHALLENGE,
                        WorldEventType.DRAGON_INCIDENT,
                        WorldEventType.DRAGON_INTENT,
                        WorldEventType.DRAGON_ATTEMPT,
                        WorldEventType.DRAGON_VICTORY,
                        WorldEventType.DRAGON_SUPPORT,
                    )
            }
            .sortedWith(
                compareByDescending<WorldEvent> {
                    it.directlyAffectsPlayer || it.characterId == state.player.id ||
                        it.relatedCharacterId == state.player.id
                }.thenByDescending { it.importance }.thenBy { it.id },
            )
            .take(ChronicleRules.MAX_DAWN_EVENT_ITEMS)

    fun format(event: WorldEvent, state: GameState): String {
        val subject = name(event.characterId, state)
        val related = event.relatedCharacterId?.let { name(it, state) }
        return when (event.type) {
            WorldEventType.DEFEAT -> when (event.characterId) {
                state.player.id -> "Your wounds have closed overnight. The Forest is open to you again."
                else -> subject + " was seen returning to town badly wounded before dawn."
            }
            WorldEventType.RECOVERY -> subject + " has returned to public life after recovering."
            WorldEventType.LEVEL_GAIN -> if (event.characterId == state.player.id) {
                "Your Level " + event.value + " standing is now entered in the public ledger."
            } else {
                "The Training Hall lists " + subject + " at Level " + event.value + "."
            }
            WorldEventType.RANK_CHANGE -> when {
                event.characterId == state.player.id ->
                    "You begin the day at rank #" + event.newRank + "."
                event.relatedCharacterId == state.player.id ->
                    subject + " has moved ahead of you to rank #" + event.newRank + "."
                event.newRank == 1 -> subject + " now leads the public rankings."
                else -> subject + " now holds rank #" + event.newRank + "."
            }
            WorldEventType.CHALLENGE -> when {
                event.characterId == state.player.id || event.relatedCharacterId == state.player.id ->
                    "Talk at the Inn still follows your challenge with " +
                        (if (event.characterId == state.player.id) related else subject) + "."
                else -> subject + " and " + (related ?: "another adventurer") +
                    " are the Inn's newest subject of argument."
            }
            WorldEventType.EQUIPMENT -> subject + " has been seen carrying " +
                (event.detail ?: "new equipment") + "."
            WorldEventType.GREAT_SUCCESS,
            WorldEventType.SOCIAL,
            -> ChronicleNarrator.format(event, state)
            WorldEventType.DRAGON_INCIDENT,
            WorldEventType.DRAGON_INTENT,
            WorldEventType.DRAGON_ATTEMPT,
            WorldEventType.DRAGON_VICTORY,
            WorldEventType.DRAGON_SUPPORT,
            -> DragonEventNarrator.dawn(event, state)
        }
    }

    private fun name(characterId: String, state: GameState): String =
        if (characterId == state.player.id) state.player.name
        else state.community.adventurers.firstOrNull { it.id == characterId }?.name ?: "An adventurer"
}
