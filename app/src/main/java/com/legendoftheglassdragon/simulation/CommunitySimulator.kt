package com.legendoftheglassdragon.simulation

import com.legendoftheglassdragon.game.CommunityRules
import com.legendoftheglassdragon.game.CommunityState
import com.legendoftheglassdragon.game.ChronicleRules
import com.legendoftheglassdragon.game.EquipmentCatalog
import com.legendoftheglassdragon.game.GameBalance
import com.legendoftheglassdragon.game.GameRandom
import com.legendoftheglassdragon.game.NpcAdventurer
import com.legendoftheglassdragon.game.NpcPersonality
import com.legendoftheglassdragon.game.NpcStatus
import com.legendoftheglassdragon.game.PlayerCharacter
import com.legendoftheglassdragon.game.ProgressionRules
import com.legendoftheglassdragon.game.RankingEntry
import com.legendoftheglassdragon.game.WorldEvent
import com.legendoftheglassdragon.game.WorldEventLocation
import com.legendoftheglassdragon.game.WorldEventType
import com.legendoftheglassdragon.game.WorldEventVisibility
import com.legendoftheglassdragon.game.WorldState

data class CommunityAdvanceResult(
    val community: CommunityState,
    val world: WorldState,
    val transitionEvents: List<WorldEvent>,
)

class CommunitySimulator(
    private val random: GameRandom,
) {
    fun advance(
        community: CommunityState,
        player: PlayerCharacter,
        worldState: WorldState,
        resolvedDay: Int,
    ): CommunityAdvanceResult {
        if (community.lastSimulatedDay >= resolvedDay) {
            return CommunityAdvanceResult(community, worldState, emptyList())
        }

        var world = worldState
        var eventSequence = 0
        val candidates = mutableListOf<WorldEvent>()
        val priorRankings = rankings(player, community.adventurers)

        fun roll(bound: Int): Int {
            val result = random.nextInt(world, bound)
            world = result.world
            return result.value
        }

        fun event(
            npc: NpcAdventurer,
            type: WorldEventType,
            importance: Int,
            value: Int = 0,
            detail: String? = null,
            visibility: WorldEventVisibility = WorldEventVisibility.PUBLIC,
            location: WorldEventLocation = WorldEventLocation.VILLAGE,
            beforeValue: Int = 0,
            afterValue: Int = value,
        ) {
            candidates += WorldEvent(
                id = "day-" + resolvedDay + "-" + npc.id + "-" + type.name.lowercase() +
                    "-" + eventSequence++,
                day = resolvedDay,
                type = type,
                characterId = npc.id,
                value = value,
                detail = detail,
                importance = importance,
                visibility = visibility,
                location = location,
                beforeValue = beforeValue,
                afterValue = afterValue,
            )
        }

        var advanced = community.adventurers.map { original ->
            if (original.lastAdvancedDay >= resolvedDay) return@map original
            if (original.status == NpcStatus.RECOVERING) {
                val days = (original.recoveryDaysRemaining - 1).coerceAtLeast(0)
                val recovered = original.copy(
                    status = if (days == 0) NpcStatus.ACTIVE else NpcStatus.RECOVERING,
                    recoveryDaysRemaining = days,
                    lastAdvancedDay = resolvedDay,
                )
                if (days == 0) {
                    event(
                        recovered,
                        WorldEventType.RECOVERY,
                        importance = 2,
                        location = WorldEventLocation.VILLAGE,
                    )
                }
                return@map recovered
            }

            var npc = original
            val actions = actionBudget(npc.personality, ::roll)
            val baseDefeatChance = defeatChance(npc.personality)
            var dailyGold = 0
            var notableSuccess = false

            repeat(actions) {
                if (npc.status != NpcStatus.ACTIVE) return@repeat
                val encounterPressure = 4 + npc.level * 2 + roll(5)
                val combatStrength = npc.level + npc.attack + npc.defense
                val defeatChance = (
                    baseDefeatChance + (encounterPressure - combatStrength) * 2
                    ).coerceIn(3, 30)
                val outcome = roll(100)
                if (outcome < defeatChance) {
                    val loss = ((npc.gold.toLong() * 15 + 99) / 100)
                        .coerceAtMost(npc.gold.toLong())
                        .toInt()
                    npc = npc.copy(
                        gold = npc.gold - loss,
                        status = NpcStatus.RECOVERING,
                        recoveryDaysRemaining = 1,
                    )
                    event(
                        npc,
                        WorldEventType.DEFEAT,
                        importance = 3,
                        value = loss,
                        location = WorldEventLocation.FOREST,
                        beforeValue = npc.gold + loss,
                        afterValue = npc.gold,
                    )
                } else {
                    // Mirrors the player's increasingly valuable same-level monster ladder
                    // without simulating hidden combat turn by turn.
                    val experience = npcExperienceReward(npc.level, roll(5))
                    val gold = 3 + npc.level + roll(7)
                    dailyGold = safeAdd(dailyGold, gold)
                    notableSuccess = notableSuccess || outcome >= 92
                    npc = npc.copy(
                        experience = safeAdd(npc.experience, experience),
                        gold = safeAdd(npc.gold, gold),
                    )
                }
            }

            if (npc.status == NpcStatus.ACTIVE &&
                ProgressionRules.isTrainingEligible(npc.asPlayerForProgression()) &&
                roll(100) < trainingChance(npc.personality)
            ) {
                val priorLevel = npc.level
                val newMaximumHp = safeAdd(npc.maximumHp, GameBalance.LEVEL_MAX_HP_GAIN)
                npc = npc.copy(
                    level = safeAdd(npc.level, 1),
                    maximumHp = newMaximumHp,
                    baseAttack = safeAdd(npc.baseAttack, GameBalance.LEVEL_ATTACK_GAIN),
                    baseDefense = safeAdd(npc.baseDefense, GameBalance.LEVEL_DEFENSE_GAIN),
                )
                event(
                    npc,
                    WorldEventType.LEVEL_GAIN,
                    importance = 3,
                    value = npc.level,
                    location = WorldEventLocation.TRAINING_HALL,
                    beforeValue = priorLevel,
                    afterValue = npc.level,
                )
            }

            if (npc.status == NpcStatus.ACTIVE) {
                npc = attemptEquipmentPurchase(npc, ::roll)?.also { purchase ->
                    event(
                        purchase.first,
                        WorldEventType.EQUIPMENT,
                        importance = 2,
                        detail = purchase.second,
                        location = WorldEventLocation.MARKET,
                    )
                }?.first ?: npc
            }

            if (npc.status == NpcStatus.ACTIVE && notableSuccess && dailyGold >= 12) {
                event(
                    npc,
                    WorldEventType.GREAT_SUCCESS,
                    importance = 2,
                    value = dailyGold,
                    visibility = WorldEventVisibility.CHRONICLE_ONLY,
                    location = WorldEventLocation.FOREST,
                )
            }
            if (npc.status == NpcStatus.ACTIVE &&
                (npc.personality == NpcPersonality.SOCIAL || roll(100) < 8)
            ) {
                event(npc, WorldEventType.SOCIAL, importance = 1)
            }
            npc.copy(lastAdvancedDay = resolvedDay)
        }

        val eligibleChallengers = advanced.filter { it.status == NpcStatus.ACTIVE }
        if (eligibleChallengers.size >= 2 && roll(100) < 55) {
            val activeIds = eligibleChallengers.mapTo(mutableSetOf()) { it.id }
            val priorChallenge = community.recentEvents.asReversed().firstOrNull { prior ->
                prior.type == WorldEventType.CHALLENGE &&
                    prior.day >= resolvedDay - 6 &&
                    prior.characterId in activeIds &&
                    prior.relatedCharacterId in activeIds
            }
            val rematch = priorChallenge != null && roll(100) < 45
            val pair = if (rematch) {
                val prior = requireNotNull(priorChallenge)
                requireNotNull(eligibleChallengers.firstOrNull { it.id == prior.characterId }) to
                    requireNotNull(eligibleChallengers.firstOrNull { it.id == prior.relatedCharacterId })
            } else {
                val firstIndex = roll(eligibleChallengers.size)
                val secondOffset = 1 + roll(eligibleChallengers.size - 1)
                eligibleChallengers[firstIndex] to
                    eligibleChallengers[(firstIndex + secondOffset) % eligibleChallengers.size]
            }
            val first = pair.first
            val second = pair.second
            val firstScore = first.level + first.attack + first.defense + roll(8)
            val secondScore = second.level + second.attack + second.defense + roll(8)
            val winner = if (firstScore >= secondScore) first else second
            val loser = if (winner.id == first.id) second else first
            val stake = minOf(loser.gold, 6 + winner.level * 2)
            advanced = advanced.map { npc ->
                when (npc.id) {
                    winner.id -> npc.copy(
                        gold = safeAdd(npc.gold, stake),
                        challengeWins = safeAdd(npc.challengeWins, 1),
                    )
                    loser.id -> npc.copy(
                        gold = npc.gold - stake,
                        challengeLosses = safeAdd(npc.challengeLosses, 1),
                    )
                    else -> npc
                }
            }
            candidates += WorldEvent(
                id = "day-" + resolvedDay + "-npc-challenge-" + eventSequence++,
                day = resolvedDay,
                type = WorldEventType.CHALLENGE,
                characterId = winner.id,
                relatedCharacterId = loser.id,
                detail = "NPC_WIN",
                importance = 3,
                visibility = WorldEventVisibility.PUBLIC,
                location = WorldEventLocation.INN,
                relatedPriorEventId = community.recentEvents
                    .asReversed()
                    .firstOrNull { prior ->
                        prior.type == WorldEventType.CHALLENGE &&
                            setOf(prior.characterId, prior.relatedCharacterId) == setOf(winner.id, loser.id)
                    }
                    ?.id,
            )
        }

        val newRankings = rankings(player, advanced)
        addRankEvents(
            resolvedDay = resolvedDay,
            player = player,
            prior = priorRankings,
            current = newRankings,
            candidates = candidates,
            startingSequence = eventSequence,
        )

        val existingToday = community.recentEvents.filter { it.day == resolvedDay }
        val curated = ChronicleRules.selectPublicNews(existingToday + candidates, player.id)
        val earliestDay = (resolvedDay - CommunityRules.NEWS_RETENTION_DAYS + 1).coerceAtLeast(1)
        val retained = (community.recentEvents.filter { it.day != resolvedDay } + curated)
            .filter { it.day >= earliestDay }
            .takeLast(CommunityRules.MAX_RETAINED_EVENTS)

        return CommunityAdvanceResult(
            community = community.copy(
                adventurers = advanced,
                recentEvents = retained,
                lastSimulatedDay = resolvedDay,
            ),
            world = world,
            transitionEvents = candidates,
        )
    }

    private fun actionBudget(
        personality: NpcPersonality,
        roll: (Int) -> Int,
    ): Int = when (personality) {
        NpcPersonality.AGGRESSIVE -> 5
        NpcPersonality.CAUTIOUS -> 2
        NpcPersonality.AMBITIOUS -> 9 + roll(2)
        NpcPersonality.GREEDY -> 4
        NpcPersonality.SOCIAL -> 2
        NpcPersonality.ERRATIC -> 3 + roll(3)
    }

    private fun defeatChance(personality: NpcPersonality): Int = when (personality) {
        NpcPersonality.AGGRESSIVE -> 14
        NpcPersonality.CAUTIOUS -> 7
        NpcPersonality.AMBITIOUS -> 9
        NpcPersonality.GREEDY -> 10
        NpcPersonality.SOCIAL -> 10
        NpcPersonality.ERRATIC -> 15
    }

    private fun trainingChance(personality: NpcPersonality): Int = when (personality) {
        NpcPersonality.AGGRESSIVE -> 90
        NpcPersonality.CAUTIOUS -> 62
        NpcPersonality.AMBITIOUS -> 96
        NpcPersonality.GREEDY -> 66
        NpcPersonality.SOCIAL -> 58
        NpcPersonality.ERRATIC -> 72
    }

    private fun attemptEquipmentPurchase(
        npc: NpcAdventurer,
        roll: (Int) -> Int,
    ): Pair<NpcAdventurer, String>? {
        val chance = when (npc.personality) {
            NpcPersonality.GREEDY -> 76
            NpcPersonality.AMBITIOUS -> 58
            NpcPersonality.AGGRESSIVE -> 48
            else -> 34
        }
        if (roll(100) >= chance) return null
        val preferWeapon = roll(2) == 0
        fun weaponPurchase(): Pair<NpcAdventurer, String>? {
            val equipment = EquipmentCatalog.nextWeapon(npc.weaponId) ?: return null
            if (npc.level < equipment.minimumLevel) return null
            val tradeIn = EquipmentCatalog.requireWeapon(npc.weaponId).resaleValue
            val price = (equipment.purchasePrice - tradeIn).coerceAtLeast(0)
            if (price > npc.gold) return null
            return npc.copy(gold = npc.gold - price, weaponId = equipment.id) to equipment.displayName
        }

        fun armorPurchase(): Pair<NpcAdventurer, String>? {
            val equipment = EquipmentCatalog.nextArmor(npc.armorId) ?: return null
            if (npc.level < equipment.minimumLevel) return null
            val tradeIn = EquipmentCatalog.requireArmor(npc.armorId).resaleValue
            val price = (equipment.purchasePrice - tradeIn).coerceAtLeast(0)
            if (price > npc.gold) return null
            return npc.copy(gold = npc.gold - price, armorId = equipment.id) to equipment.displayName
        }

        return if (preferWeapon) {
            weaponPurchase() ?: armorPurchase()
        } else {
            armorPurchase() ?: weaponPurchase()
        }
    }

    private fun addRankEvents(
        resolvedDay: Int,
        player: PlayerCharacter,
        prior: List<RankingEntry>,
        current: List<RankingEntry>,
        candidates: MutableList<WorldEvent>,
        startingSequence: Int,
    ) {
        var sequence = startingSequence
        val priorLeader = prior.firstOrNull()?.characterId
        val currentLeader = current.firstOrNull()?.characterId
        if (currentLeader != null && currentLeader != priorLeader) {
            candidates += WorldEvent(
                id = "day-" + resolvedDay + "-rank-leader-" + sequence++,
                day = resolvedDay,
                type = WorldEventType.RANK_CHANGE,
                characterId = currentLeader,
                relatedCharacterId = priorLeader,
                previousRank = prior.indexOfFirst { it.characterId == currentLeader } + 1,
                newRank = 1,
                importance = 3,
                visibility = WorldEventVisibility.PUBLIC,
                location = WorldEventLocation.VILLAGE,
                beforeValue = prior.indexOfFirst { it.characterId == currentLeader } + 1,
                afterValue = 1,
                directlyAffectsPlayer = currentLeader == player.id || priorLeader == player.id,
            )
        }
        val oldPlayerRank = prior.indexOfFirst { it.characterId == player.id } + 1
        val newPlayerRank = current.indexOfFirst { it.characterId == player.id } + 1
        if (oldPlayerRank != newPlayerRank) {
            val mover = if (newPlayerRank > oldPlayerRank) {
                current.getOrNull(oldPlayerRank - 1)?.characterId
            } else {
                player.id
            } ?: player.id
            candidates += WorldEvent(
                id = "day-" + resolvedDay + "-rank-player-" + sequence,
                day = resolvedDay,
                type = WorldEventType.RANK_CHANGE,
                characterId = mover,
                relatedCharacterId = player.id,
                previousRank = oldPlayerRank,
                newRank = if (mover == player.id) newPlayerRank else current.indexOfFirst {
                    it.characterId == mover
                } + 1,
                importance = 2,
                visibility = WorldEventVisibility.PUBLIC,
                location = WorldEventLocation.VILLAGE,
                beforeValue = oldPlayerRank,
                afterValue = newPlayerRank,
                directlyAffectsPlayer = true,
            )
        }
    }

    private fun rankings(
        player: PlayerCharacter,
        adventurers: List<NpcAdventurer>,
    ): List<RankingEntry> = CommunityRules.rankings(player, adventurers)

    private fun NpcAdventurer.asPlayerForProgression(): PlayerCharacter =
        PlayerCharacter(
            id = id,
            name = name,
            level = level,
            experience = experience,
            currentHp = maximumHp,
            maximumHp = maximumHp,
            baseAttack = baseAttack,
            baseDefense = baseDefense,
            gold = gold,
            bankedGold = 0,
            weaponId = weaponId,
            armorId = armorId,
        )

    private fun safeAdd(value: Int, amount: Int): Int =
        (value.toLong() + amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    private fun npcExperienceReward(level: Int, variation: Int): Int =
        (4L + level.toLong() * level.toLong() * 2L + variation.toLong())
            .coerceAtMost(1_000_000L)
            .toInt()
}
