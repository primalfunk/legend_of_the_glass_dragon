package com.legendoftheglassdragon.game

import kotlin.math.max

object DragonSeasonBalance {
    const val SIGNS_PRESSURE = 18
    const val FEAR_PRESSURE = 40
    const val CRISIS_PRESSURE = 68
    const val CHALLENGE_WINDOW_PRESSURE = 92
    const val MAX_PRESSURE = 120
    const val PLAYER_MINIMUM_LEVEL = 7
    const val NPC_MINIMUM_LEVEL = 5
    const val MINIMUM_WEAPON_TIER = 4
    const val MINIMUM_ARMOR_TIER = 4
    const val NPC_MINIMUM_WEAPON_TIER = 2
    const val NPC_MINIMUM_ARMOR_TIER = 2
    const val MINIMUM_COMBAT_POWER = 125
    const val NPC_MINIMUM_COMBAT_POWER = 105
    const val POTENTIAL_COMBAT_POWER = 85
    const val NPC_FALLBACK_DECLARATION_DAY = 21
    const val NPC_FALLBACK_ATTEMPT_DAY = 23
    const val GUARANTEED_RESOLUTION_DAY = 32
    const val ATTEMPT_RECOVERY_DAYS = 2
    const val MAX_INCIDENTS = 60
    const val MAX_ATTEMPTS = 24
    const val SUPPORT_GOLD = 50
    const val DRAGON_MAXIMUM_HP = 108
    const val DRAGON_ATTACK = 21
    const val DRAGON_CRYSTAL_DEFENSE = 15
    const val DRAGON_FRACTURED_DEFENSE = 11
    const val DRAGON_BREATH_BONUS = 7
    const val WELL_PREPARED_COMBAT_POWER = 140
    const val ABSTRACT_DRAGON_POWER = 125
}

enum class DragonDisposition {
    FEARFUL,
    CAUTIOUS,
    OPPORTUNISTIC,
    SUPPORTIVE,
    AMBITIOUS,
    OBSESSED,
}

data class DragonReadiness(
    val combatPower: Int,
    val stageReady: Boolean,
    val levelReady: Boolean,
    val weaponReady: Boolean,
    val armorReady: Boolean,
    val healthReady: Boolean,
    val minimumCombatPower: Int,
) {
    val isReady: Boolean
        get() = stageReady && levelReady && weaponReady && armorReady && healthReady &&
            combatPower >= minimumCombatPower

    val tier: DragonReadinessTier
        get() = when {
            !isReady -> DragonReadinessTier.NOT_READY
            combatPower >= minimumCombatPower + 15 -> DragonReadinessTier.WELL_PREPARED
            combatPower >= minimumCombatPower + 8 -> DragonReadinessTier.READY
            else -> DragonReadinessTier.DANGEROUS
        }

    fun playerGuidance(): List<String> = buildList {
        if (!stageReady) add("The Dragon's territory is not yet open to challenge.")
        if (!levelReady) add("You need more Physical Training.")
        if (!weaponReady) add("Your weapon is too weak for crystalline scales.")
        if (!armorReady) add("Your armor will not withstand the Dragon's breath.")
        if (!healthReady) add("Face the Dragon only at full health.")
        if (combatPower < DragonSeasonBalance.MINIMUM_COMBAT_POWER &&
            levelReady && weaponReady && armorReady
        ) {
            add("You still lack the strength for a safe attempt.")
        }
        if (isReady && combatPower < DragonSeasonBalance.WELL_PREPARED_COMBAT_POWER) {
            add("You may face the Dragon now, but the attempt will be dangerous.")
        } else if (isReady) {
            add("You are as well prepared as any champion can be.")
        }
    }
}

enum class DragonReadinessTier {
    NOT_READY,
    DANGEROUS,
    READY,
    WELL_PREPARED,
}

object DragonReadinessRules {
    fun player(state: GameState): DragonReadiness = DragonReadiness(
        combatPower = power(
            level = state.player.level,
            attack = state.player.attack,
            defense = state.player.defense,
            maximumHp = state.player.maximumHp,
            weaponTier = EquipmentCatalog.requireWeapon(state.player.weaponId).tier,
            armorTier = EquipmentCatalog.requireArmor(state.player.armorId).tier,
        ),
        stageReady = state.season.stage == DragonStage.CHALLENGE_WINDOW,
        levelReady = state.player.level >= DragonSeasonBalance.PLAYER_MINIMUM_LEVEL,
        weaponReady = EquipmentCatalog.requireWeapon(state.player.weaponId).tier >=
            DragonSeasonBalance.MINIMUM_WEAPON_TIER,
        armorReady = EquipmentCatalog.requireArmor(state.player.armorId).tier >=
            DragonSeasonBalance.MINIMUM_ARMOR_TIER,
        healthReady = state.player.currentHp == state.player.maximumHp &&
            !state.playerDay.wasDefeatedToday,
        minimumCombatPower = DragonSeasonBalance.MINIMUM_COMBAT_POWER,
    )

    fun npc(npc: NpcAdventurer, stage: DragonStage): DragonReadiness = DragonReadiness(
        combatPower = power(
            level = npc.level,
            attack = npc.attack,
            defense = npc.defense,
            maximumHp = npc.maximumHp,
            weaponTier = EquipmentCatalog.requireWeapon(npc.weaponId).tier,
            armorTier = EquipmentCatalog.requireArmor(npc.armorId).tier,
        ),
        stageReady = stage == DragonStage.CHALLENGE_WINDOW,
        levelReady = npc.level >= DragonSeasonBalance.NPC_MINIMUM_LEVEL,
        weaponReady = EquipmentCatalog.requireWeapon(npc.weaponId).tier >=
            DragonSeasonBalance.NPC_MINIMUM_WEAPON_TIER,
        armorReady = EquipmentCatalog.requireArmor(npc.armorId).tier >=
            DragonSeasonBalance.NPC_MINIMUM_ARMOR_TIER,
        healthReady = npc.status == NpcStatus.ACTIVE,
        minimumCombatPower = DragonSeasonBalance.NPC_MINIMUM_COMBAT_POWER,
    )

    fun potentialCandidates(state: GameState): List<NpcAdventurer> =
        state.community.adventurers
            .filter { it.status == NpcStatus.ACTIVE }
            .filter { npc(state.season.stage, it).combatPower >= DragonSeasonBalance.POTENTIAL_COMBAT_POWER }
            .sortedWith(
                compareByDescending<NpcAdventurer> { npc(state.season.stage, it).combatPower }
                    .thenBy { it.id },
            )

    fun disposition(npc: NpcAdventurer): DragonDisposition = when (npc.personality) {
        NpcPersonality.CAUTIOUS -> DragonDisposition.CAUTIOUS
        NpcPersonality.AGGRESSIVE -> DragonDisposition.OBSESSED
        NpcPersonality.AMBITIOUS -> DragonDisposition.AMBITIOUS
        NpcPersonality.GREEDY -> DragonDisposition.OPPORTUNISTIC
        NpcPersonality.SOCIAL -> DragonDisposition.SUPPORTIVE
        NpcPersonality.ERRATIC -> DragonDisposition.FEARFUL
    }

    private fun npc(stage: DragonStage, adventurer: NpcAdventurer): DragonReadiness =
        npc(adventurer, stage)

    private fun power(
        level: Int,
        attack: Int,
        defense: Int,
        maximumHp: Int,
        weaponTier: Int,
        armorTier: Int,
    ): Int = safeInt(
        level.toLong() * 4L + attack.toLong() * 2L + defense.toLong() * 2L +
            maximumHp.toLong() / 4L + (weaponTier + armorTier).toLong() * 2L,
    )

    private fun safeInt(value: Long): Int = value.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

data class DragonSeasonAdvanceResult(
    val season: DragonSeasonState,
    val community: CommunityState,
    val world: WorldState,
    val events: List<WorldEvent>,
)

class DragonSeasonSimulator(
    private val random: GameRandom = PersistedGameRandom,
) {
    fun advance(
        state: GameState,
        community: CommunityState,
        worldState: WorldState,
        resolvedDay: Int,
    ): DragonSeasonAdvanceResult {
        if (state.season.stage == DragonStage.RESOLVED) {
            return DragonSeasonAdvanceResult(state.season, community, worldState, emptyList())
        }
        var world = worldState
        fun roll(bound: Int): Int {
            val result = random.nextInt(world, bound)
            world = result.world
            return result.value
        }

        var season = state.season
        var residents = community.adventurers
        val events = mutableListOf<WorldEvent>()
        val strongResidents = residents.count {
            DragonReadinessRules.npc(it, season.stage).combatPower >=
                DragonSeasonBalance.POTENTIAL_COMBAT_POWER
        }
        val increment = 4 + roll(3) + minOf(2, strongResidents / 5)
        val nextPressure = (season.pressure + increment).coerceAtMost(DragonSeasonBalance.MAX_PRESSURE)
        val nextStage = stageFor(nextPressure)
        val stageChanged = nextStage.ordinal > season.stage.ordinal
        season = season.copy(
            pressure = nextPressure,
            stage = nextStage,
            stageStartedDay = if (stageChanged) resolvedDay else season.stageStartedDay,
        )

        val incidentChance = when (season.stage) {
            DragonStage.WHISPERS -> 22
            DragonStage.SIGNS -> 38
            DragonStage.FEAR -> 52
            DragonStage.CRISIS -> 64
            DragonStage.CHALLENGE_WINDOW -> 70
            DragonStage.RESOLVED -> 0
        }
        val incidentDue = stageChanged ||
            (resolvedDay - season.lastIncidentDay >= 2 && roll(100) < incidentChance)
        if (incidentDue) {
            val allowed = allowedIncidents(season.stage)
            val incidentType = if (stageChanged) allowed.last() else allowed[roll(allowed.size)]
            val visibility = if (
                season.stage <= DragonStage.SIGNS &&
                incidentType in setOf(
                    DragonIncidentType.DISTANT_CHIMES,
                    DragonIncidentType.GLASS_FRAGMENTS,
                )
            ) {
                WorldEventVisibility.CHRONICLE_ONLY
            } else {
                WorldEventVisibility.PUBLIC
            }
            val incident = DragonIncidentRecord(
                id = "dragon-incident-day-" + resolvedDay + "-" + incidentType.name.lowercase(),
                day = resolvedDay,
                type = incidentType,
                stage = season.stage,
                visibility = visibility,
            )
            season = season.copy(
                lastIncidentDay = resolvedDay,
                incidents = (season.incidents + incident).takeLast(DragonSeasonBalance.MAX_INCIDENTS),
            )
            events += incidentEvent(incident, state.player.id, stageChanged)
        }

        if (season.stage >= DragonStage.CRISIS && season.stage < DragonStage.RESOLVED) {
            val stateForCandidates = state.copy(community = community.copy(adventurers = residents), season = season)
            val undeclared = DragonReadinessRules.potentialCandidates(stateForCandidates)
                .filter { it.id !in season.declaredChallengerIds }
            val fallback = resolvedDay >= DragonSeasonBalance.NPC_FALLBACK_DECLARATION_DAY
            val candidate = undeclared.firstOrNull { npc ->
                fallback || roll(100) < intentChance(DragonReadinessRules.disposition(npc))
            }
            if (candidate != null) {
                season = season.copy(
                    declaredChallengerIds = (season.declaredChallengerIds + candidate.id).distinct(),
                )
                events += WorldEvent(
                    id = "dragon-intent-day-" + resolvedDay + "-" + candidate.id,
                    day = resolvedDay,
                    type = WorldEventType.DRAGON_INTENT,
                    characterId = candidate.id,
                    importance = 3,
                    visibility = WorldEventVisibility.PUBLIC,
                    location = WorldEventLocation.INN,
                )
            }
        }

        if (season.stage >= DragonStage.FEAR && season.stage < DragonStage.RESOLVED) {
            val preparerIds = season.declaredChallengerIds.toSet()
            residents = residents.map { npc ->
                if (npc.id !in preparerIds || npc.status != NpcStatus.ACTIVE || roll(100) >= 58) {
                    npc
                } else {
                    val purchase = dragonPreparationPurchase(npc, ::roll)
                    if (purchase != null) {
                        events += WorldEvent(
                            id = "dragon-preparation-day-" + resolvedDay + "-" + npc.id,
                            day = resolvedDay,
                            type = WorldEventType.EQUIPMENT,
                            characterId = npc.id,
                            detail = purchase.second,
                            importance = 2,
                            visibility = WorldEventVisibility.PUBLIC,
                            location = WorldEventLocation.MARKET,
                        )
                        purchase.first
                    } else {
                        npc
                    }
                }
            }
        }

        if (season.stage == DragonStage.CHALLENGE_WINDOW) {
            val previousAttemptIds = season.attempts.mapTo(mutableSetOf()) { it.challengerId }
            val declared = season.declaredChallengerIds.mapNotNull { id ->
                residents.firstOrNull { it.id == id }
            }.filter { it.status == NpcStatus.ACTIVE }
            val qualified = declared.filter {
                DragonReadinessRules.npc(it, season.stage).isReady
            }.sortedWith(
                compareByDescending<NpcAdventurer> {
                    DragonReadinessRules.npc(it, season.stage).combatPower
                }.thenBy { it.id },
            )
            val daysOpen = resolvedDay - season.stageStartedDay
            val challenger = qualified.firstOrNull { npc ->
                val lastAttempt = season.attempts.lastOrNull { it.challengerId == npc.id }?.day ?: 0
                val rested = resolvedDay - lastAttempt >= DragonSeasonBalance.ATTEMPT_RECOVERY_DAYS
                val firstTryBonus = if (npc.id !in previousAttemptIds) 12 else 0
                rested && daysOpen >= 1 && (
                    resolvedDay >= DragonSeasonBalance.NPC_FALLBACK_ATTEMPT_DAY ||
                        roll(100) < attemptChance(DragonReadinessRules.disposition(npc)) +
                        daysOpen * 3 + firstTryBonus
                    )
            }
            if (challenger != null) {
                val readiness = DragonReadinessRules.npc(challenger, season.stage)
                val failedAttempts = season.attempts.count { it.outcome != DragonAttemptOutcome.VICTORY }
                val dragonPower = (
                    DragonSeasonBalance.ABSTRACT_DRAGON_POWER - minOf(25, failedAttempts * 5) -
                        (resolvedDay - 25).coerceAtLeast(0) * 2
                    ).coerceAtLeast(92)
                val challengerRoll = roll(21)
                val dragonRoll = roll(21)
                val margin = readiness.combatPower + challengerRoll - dragonPower - dragonRoll
                val outcome = when {
                    resolvedDay >= DragonSeasonBalance.GUARANTEED_RESOLUTION_DAY ->
                        DragonAttemptOutcome.VICTORY
                    margin >= 0 -> DragonAttemptOutcome.VICTORY
                    margin >= -8 -> DragonAttemptOutcome.RETREAT
                    margin >= -18 -> DragonAttemptOutcome.NARROW_DEFEAT
                    else -> DragonAttemptOutcome.DECISIVE_DEFEAT
                }
                val attempt = DragonAttemptRecord(
                    id = "dragon-attempt-day-" + resolvedDay + "-" + challenger.id,
                    day = resolvedDay,
                    challengerId = challenger.id,
                    outcome = outcome,
                    challengerPower = readiness.combatPower,
                    dragonPower = dragonPower,
                )
                season = season.copy(
                    attempts = (season.attempts + attempt).takeLast(DragonSeasonBalance.MAX_ATTEMPTS),
                )
                if (outcome == DragonAttemptOutcome.VICTORY) {
                    season = season.copy(
                        stage = DragonStage.RESOLVED,
                        victorId = challenger.id,
                        resolvedDay = resolvedDay,
                        conclusionPhase = SeasonConclusionPhase.VICTORY_NOTICE,
                    )
                    events += attemptEvent(attempt, WorldEventType.DRAGON_VICTORY)
                } else {
                    val loss = ((challenger.gold.toLong() * 20L + 99L) / 100L)
                        .coerceAtMost(challenger.gold.toLong()).toInt()
                    residents = residents.map { npc ->
                        if (npc.id == challenger.id) {
                            npc.copy(
                                gold = npc.gold - loss,
                                status = NpcStatus.RECOVERING,
                                recoveryDaysRemaining = DragonSeasonBalance.ATTEMPT_RECOVERY_DAYS,
                            )
                        } else {
                            npc
                        }
                    }
                    events += attemptEvent(attempt, WorldEventType.DRAGON_ATTEMPT)
                }
            }
        }

        return DragonSeasonAdvanceResult(
            season = season,
            community = community.copy(adventurers = residents),
            world = world,
            events = events,
        )
    }

    private fun stageFor(pressure: Int): DragonStage = when {
        pressure >= DragonSeasonBalance.CHALLENGE_WINDOW_PRESSURE -> DragonStage.CHALLENGE_WINDOW
        pressure >= DragonSeasonBalance.CRISIS_PRESSURE -> DragonStage.CRISIS
        pressure >= DragonSeasonBalance.FEAR_PRESSURE -> DragonStage.FEAR
        pressure >= DragonSeasonBalance.SIGNS_PRESSURE -> DragonStage.SIGNS
        else -> DragonStage.WHISPERS
    }

    private fun allowedIncidents(stage: DragonStage): List<DragonIncidentType> = when (stage) {
        DragonStage.WHISPERS -> listOf(DragonIncidentType.DISTANT_CHIMES)
        DragonStage.SIGNS -> listOf(
            DragonIncidentType.DISTANT_CHIMES,
            DragonIncidentType.GLASS_FRAGMENTS,
            DragonIncidentType.MISSING_TRAVELERS,
        )
        DragonStage.FEAR -> listOf(
            DragonIncidentType.MISSING_TRAVELERS,
            DragonIncidentType.BURNED_WOODLAND,
            DragonIncidentType.WOUNDED_SCOUT,
        )
        DragonStage.CRISIS -> listOf(
            DragonIncidentType.BURNED_WOODLAND,
            DragonIncidentType.WOUNDED_SCOUT,
            DragonIncidentType.SHATTERED_HOMESTEAD,
            DragonIncidentType.DRAGON_SIGHTING,
        )
        DragonStage.CHALLENGE_WINDOW -> listOf(
            DragonIncidentType.SHATTERED_HOMESTEAD,
            DragonIncidentType.DRAGON_SIGHTING,
            DragonIncidentType.CHAMPION_CALL,
        )
        DragonStage.RESOLVED -> emptyList()
    }

    private fun intentChance(disposition: DragonDisposition): Int = when (disposition) {
        DragonDisposition.FEARFUL -> 14
        DragonDisposition.CAUTIOUS -> 22
        DragonDisposition.OPPORTUNISTIC -> 42
        DragonDisposition.SUPPORTIVE -> 30
        DragonDisposition.AMBITIOUS -> 68
        DragonDisposition.OBSESSED -> 82
    }

    private fun attemptChance(disposition: DragonDisposition): Int = when (disposition) {
        DragonDisposition.FEARFUL -> 8
        DragonDisposition.CAUTIOUS -> 16
        DragonDisposition.OPPORTUNISTIC -> 28
        DragonDisposition.SUPPORTIVE -> 22
        DragonDisposition.AMBITIOUS -> 48
        DragonDisposition.OBSESSED -> 62
    }

    private fun dragonPreparationPurchase(
        npc: NpcAdventurer,
        roll: (Int) -> Int,
    ): Pair<NpcAdventurer, String>? {
        fun weapon(): Pair<NpcAdventurer, String>? {
            val next = EquipmentCatalog.nextWeapon(npc.weaponId) ?: return null
            if (npc.level < next.minimumLevel) return null
            val cost = (next.purchasePrice - EquipmentCatalog.requireWeapon(npc.weaponId).resaleValue)
                .coerceAtLeast(0)
            if (cost > npc.gold) return null
            return npc.copy(gold = npc.gold - cost, weaponId = next.id) to next.displayName
        }

        fun armor(): Pair<NpcAdventurer, String>? {
            val next = EquipmentCatalog.nextArmor(npc.armorId) ?: return null
            if (npc.level < next.minimumLevel) return null
            val cost = (next.purchasePrice - EquipmentCatalog.requireArmor(npc.armorId).resaleValue)
                .coerceAtLeast(0)
            if (cost > npc.gold) return null
            return npc.copy(gold = npc.gold - cost, armorId = next.id) to next.displayName
        }

        return if (roll(2) == 0) weapon() ?: armor() else armor() ?: weapon()
    }

    private fun incidentEvent(
        incident: DragonIncidentRecord,
        playerId: String,
        stageChanged: Boolean,
    ): WorldEvent = WorldEvent(
        id = incident.id,
        day = incident.day,
        type = WorldEventType.DRAGON_INCIDENT,
        characterId = playerId,
        detail = incident.id,
        value = incident.stage.ordinal,
        importance = if (stageChanged || incident.stage >= DragonStage.FEAR) 3 else 2,
        visibility = incident.visibility,
        location = WorldEventLocation.VILLAGE,
        beforeValue = (incident.stage.ordinal - if (stageChanged) 1 else 0).coerceAtLeast(0),
        afterValue = incident.stage.ordinal,
    )

    private fun attemptEvent(
        attempt: DragonAttemptRecord,
        type: WorldEventType,
    ): WorldEvent = WorldEvent(
        id = attempt.id,
        day = attempt.day,
        type = type,
        characterId = attempt.challengerId,
        detail = attempt.id,
        value = attempt.challengerPower,
        importance = 3,
        visibility = WorldEventVisibility.PUBLIC,
        location = WorldEventLocation.FOREST,
        beforeValue = attempt.challengerPower,
        afterValue = attempt.dragonPower,
    )
}

object DragonSeasonActions {
    fun supportCandidate(
        state: GameState,
        expectedRevision: Long,
        candidateId: String,
    ): GameActionResult {
        if (state.revision != expectedRevision) return GameActionResult.Rejected(GameActionRejection.StaleState)
        if (state.season.stage == DragonStage.RESOLVED) {
            return GameActionResult.Rejected(GameActionRejection.SeasonResolved)
        }
        if (state.activeCombat != null || state.activeChallenge != null || state.activeDragonCombat != null) {
            return GameActionResult.Rejected(GameActionRejection.InvalidDragonSupport)
        }
        val candidate = state.community.adventurers.firstOrNull { it.id == candidateId }
            ?: return GameActionResult.Rejected(GameActionRejection.InvalidDragonSupport)
        if (candidate.id !in state.season.declaredChallengerIds ||
            state.player.gold < DragonSeasonBalance.SUPPORT_GOLD
        ) {
            return GameActionResult.Rejected(GameActionRejection.InvalidDragonSupport)
        }
        val updated = candidate.copy(
            gold = safeAdd(candidate.gold, DragonSeasonBalance.SUPPORT_GOLD),
            familiarity = minOf(CommunityRules.MAX_FAMILIARITY, candidate.familiarity + 3),
        )
        val event = WorldEvent(
            id = "dragon-support-day-" + state.currentDay + "-" + candidate.id + "-" + state.revision,
            day = state.currentDay,
            type = WorldEventType.DRAGON_SUPPORT,
            characterId = state.player.id,
            relatedCharacterId = candidate.id,
            value = DragonSeasonBalance.SUPPORT_GOLD,
            importance = 2,
            visibility = WorldEventVisibility.PLAYER_DIRECT,
            location = WorldEventLocation.INN,
            directlyAffectsPlayer = true,
        )
        return GameActionResult.Applied(
            WorldEventLedger.append(
                state.copy(
                    revision = state.revision + 1,
                    player = state.player.copy(
                        gold = state.player.gold - DragonSeasonBalance.SUPPORT_GOLD,
                    ),
                    community = state.community.copy(
                        adventurers = state.community.adventurers.map {
                            if (it.id == updated.id) updated else it
                        },
                    ),
                ),
                event,
            ),
        )
    }

    fun advanceConclusion(state: GameState, expectedRevision: Long): GameActionResult {
        if (state.revision != expectedRevision) return GameActionResult.Rejected(GameActionRejection.StaleState)
        if (state.season.stage != DragonStage.RESOLVED || state.season.conclusionPhase == null) {
            return GameActionResult.Rejected(GameActionRejection.SeasonResolved)
        }
        val next = when (state.season.conclusionPhase) {
            SeasonConclusionPhase.VICTORY_NOTICE -> SeasonConclusionPhase.FINAL_CHRONICLE
            SeasonConclusionPhase.FINAL_CHRONICLE -> SeasonConclusionPhase.SEASON_RECORD
            SeasonConclusionPhase.SEASON_RECORD -> SeasonConclusionPhase.COMPLETE
            SeasonConclusionPhase.COMPLETE -> SeasonConclusionPhase.COMPLETE
        }
        return GameActionResult.Applied(
            state.copy(
                revision = state.revision + 1,
                season = state.season.copy(conclusionPhase = next),
            ),
        )
    }

    private fun safeAdd(value: Int, amount: Int): Int =
        (value.toLong() + amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

class DragonCombatEngine(
    private val random: GameRandom = PersistedGameRandom,
) {
    fun begin(state: GameState, expectedRevision: Long): GameActionResult {
        stale(state, expectedRevision)?.let { return it }
        if (state.season.stage == DragonStage.RESOLVED) return rejected(GameActionRejection.SeasonResolved)
        if (state.season.stage != DragonStage.CHALLENGE_WINDOW) {
            return rejected(GameActionRejection.DragonChallengeClosed)
        }
        if (!DragonReadinessRules.player(state).isReady) return rejected(GameActionRejection.DragonNotReady)
        if (state.season.playerLastAttemptDay == state.currentDay) {
            return rejected(GameActionRejection.DragonAttemptUsedToday)
        }
        if (state.activeCombat != null || state.activeChallenge != null || state.activeDragonCombat != null) {
            return rejected(GameActionRejection.ActiveDragonCombat)
        }
        return applied(
            state.copy(
                activeDragonCombat = DragonCombatState(
                    dragonCurrentHp = DragonSeasonBalance.DRAGON_MAXIMUM_HP,
                    status = DragonCombatStatus.ACTIVE,
                    recentMessages = listOf(
                        CombatMessage(
                            "The Glass Dragon unfolds from the crystal wastes in a storm of cold light.",
                            CombatMessageKind.ENCOUNTER,
                        ),
                    ),
                    turnNumber = 0,
                    crystalHideBroken = false,
                    attemptRecorded = false,
                ),
            ),
        )
    }

    fun attack(state: GameState, expectedRevision: Long): GameActionResult {
        stale(state, expectedRevision)?.let { return it }
        val combat = state.activeDragonCombat ?: return rejected(GameActionRejection.NoActiveDragonCombat)
        if (combat.status != DragonCombatStatus.ACTIVE) {
            return rejected(GameActionRejection.DragonCombatAlreadyComplete)
        }
        val playerRoll = random.nextInt(state.world, GameBalance.DAMAGE_RANDOM_VALUES)
        val dragonDefense = if (combat.crystalHideBroken) {
            DragonSeasonBalance.DRAGON_FRACTURED_DEFENSE
        } else {
            DragonSeasonBalance.DRAGON_CRYSTAL_DEFENSE
        }
        val playerDamage = max(
            GameBalance.MINIMUM_DAMAGE,
            state.player.attack + playerRoll.value - dragonDefense,
        )
        val dragonHp = (combat.dragonCurrentHp - playerDamage).coerceAtLeast(0)
        var messages = append(
            combat.recentMessages,
            CombatMessage(
                "Your blow strikes crystalline scales for " + playerDamage + " damage!",
                CombatMessageKind.PLAYER_ACTION,
            ),
        )
        if (dragonHp == 0) return playerVictory(state, combat, playerRoll.world, messages)

        val hideBroken = combat.crystalHideBroken ||
            dragonHp <= DragonSeasonBalance.DRAGON_MAXIMUM_HP / 2
        if (hideBroken && !combat.crystalHideBroken) {
            messages = append(
                messages,
                CombatMessage(
                    "*** THE CRYSTAL HIDE FRACTURES! ***",
                    CombatMessageKind.INFO,
                ),
            )
        }
        val nextTurn = combat.turnNumber + 1
        val dragonRoll = random.nextInt(playerRoll.world, GameBalance.DAMAGE_RANDOM_VALUES)
        val breath = if (nextTurn % 3 == 0) DragonSeasonBalance.DRAGON_BREATH_BONUS else 0
        val dragonDamage = max(
            GameBalance.MINIMUM_DAMAGE,
            DragonSeasonBalance.DRAGON_ATTACK + dragonRoll.value + breath - state.player.defense,
        )
        val playerHp = (state.player.currentHp - dragonDamage).coerceAtLeast(0)
        messages = append(
            messages,
            CombatMessage(
                if (breath > 0) {
                    "Refracted fire tears across the wastes for " + dragonDamage + " damage!"
                } else {
                    "A glass-edged talon strikes for " + dragonDamage + " damage!"
                },
                CombatMessageKind.MONSTER_ACTION,
            ),
        )
        if (playerHp == 0) {
            return playerFailure(
                state.copy(world = dragonRoll.world),
                combat.copy(
                    dragonCurrentHp = dragonHp,
                    turnNumber = nextTurn,
                    crystalHideBroken = hideBroken,
                ),
                messages,
                DragonAttemptOutcome.DECISIVE_DEFEAT,
            )
        }
        return applied(
            state.copy(
                world = dragonRoll.world,
                player = state.player.copy(currentHp = playerHp),
                activeDragonCombat = combat.copy(
                    dragonCurrentHp = dragonHp,
                    recentMessages = messages,
                    turnNumber = nextTurn,
                    crystalHideBroken = hideBroken,
                ),
            ),
        )
    }

    fun retreat(state: GameState, expectedRevision: Long): GameActionResult {
        stale(state, expectedRevision)?.let { return it }
        val combat = state.activeDragonCombat ?: return rejected(GameActionRejection.NoActiveDragonCombat)
        if (combat.status != DragonCombatStatus.ACTIVE) {
            return rejected(GameActionRejection.DragonCombatAlreadyComplete)
        }
        return recordPlayerAttempt(
            state = state,
            combat = combat.copy(
                status = DragonCombatStatus.RETREATED,
                recentMessages = append(
                    combat.recentMessages,
                    CombatMessage(
                        "You withdraw before the crystal wastes can become your tomb.",
                        CombatMessageKind.ESCAPE,
                    ),
                ),
            ),
            outcome = DragonAttemptOutcome.RETREAT,
            goldLost = 0,
        )
    }

    fun leave(state: GameState, expectedRevision: Long): GameActionResult {
        stale(state, expectedRevision)?.let { return it }
        val combat = state.activeDragonCombat ?: return rejected(GameActionRejection.NoActiveDragonCombat)
        if (combat.status == DragonCombatStatus.ACTIVE) return rejected(GameActionRejection.ActiveDragonCombat)
        return applied(state.copy(activeDragonCombat = null))
    }

    private fun playerVictory(
        state: GameState,
        combat: DragonCombatState,
        world: WorldState,
        messages: List<CombatMessage>,
    ): GameActionResult {
        val attempt = attempt(
            state,
            DragonAttemptOutcome.VICTORY,
            DragonReadinessRules.player(state).combatPower,
        )
        val event = attemptEvent(state, attempt, WorldEventType.DRAGON_VICTORY)
        val won = state.copy(
            world = world,
            season = state.season.copy(
                stage = DragonStage.RESOLVED,
                attempts = appendAttempt(state.season.attempts, attempt),
                playerLastAttemptDay = state.currentDay,
                victorId = state.player.id,
                resolvedDay = state.currentDay,
                conclusionPhase = SeasonConclusionPhase.VICTORY_NOTICE,
            ),
            activeDragonCombat = combat.copy(
                dragonCurrentHp = 0,
                status = DragonCombatStatus.PLAYER_VICTORY,
                recentMessages = append(
                    messages,
                    CombatMessage(
                        "*** THE GLASS DRAGON SHATTERS INTO COLD STARLIGHT! ***",
                        CombatMessageKind.VICTORY,
                    ),
                ),
                attemptRecorded = true,
            ),
        )
        val withEvent = WorldEventLedger.append(won, event)
        val finalRecord = ChronicleRecord(
            day = state.currentDay,
            events = ChronicleRules.selectChronicleEvents(
                withEvent.community.recentEvents.filter { it.day == state.currentDay },
                state.player.id,
            ),
        )
        return applied(
            withEvent.copy(
                community = withEvent.community.copy(
                    chronicleHistory = ChronicleRules.appendRecord(
                        withEvent.community.chronicleHistory,
                        finalRecord,
                    ),
                ),
            ),
        )
    }

    private fun playerFailure(
        state: GameState,
        combat: DragonCombatState,
        messages: List<CombatMessage>,
        outcome: DragonAttemptOutcome,
    ): GameActionResult {
        val loss = ((state.player.gold.toLong() * GameBalance.DEFEAT_GOLD_LOSS_PERCENT + 99L) / 100L)
            .coerceAtMost(state.player.gold.toLong()).toInt()
        val recoveryHp = max(1, state.player.maximumHp / GameBalance.DEFEAT_RECOVERY_HP_DIVISOR)
        return recordPlayerAttempt(
            state = state.copy(
                player = state.player.copy(currentHp = recoveryHp, gold = state.player.gold - loss),
                playerDay = state.playerDay.copy(fightsRemaining = 0, wasDefeatedToday = true),
            ),
            combat = combat.copy(
                status = DragonCombatStatus.PLAYER_DEFEAT,
                recentMessages = append(
                    messages,
                    CombatMessage(
                        "The Dragon casts you from its territory. You will not return today.",
                        CombatMessageKind.DEFEAT,
                    ),
                ),
                goldLost = loss,
            ),
            outcome = outcome,
            goldLost = loss,
        )
    }

    private fun recordPlayerAttempt(
        state: GameState,
        combat: DragonCombatState,
        outcome: DragonAttemptOutcome,
        goldLost: Int,
    ): GameActionResult {
        val attempt = attempt(state, outcome, DragonReadinessRules.player(state).combatPower)
        val event = attemptEvent(state, attempt, WorldEventType.DRAGON_ATTEMPT)
        val recorded = state.copy(
            season = state.season.copy(
                attempts = appendAttempt(state.season.attempts, attempt),
                playerLastAttemptDay = state.currentDay,
            ),
            activeDragonCombat = combat.copy(attemptRecorded = true, goldLost = goldLost),
        )
        return applied(WorldEventLedger.append(recorded, event))
    }

    private fun attempt(
        state: GameState,
        outcome: DragonAttemptOutcome,
        power: Int,
    ) = DragonAttemptRecord(
        id = "dragon-attempt-day-" + state.currentDay + "-" + state.player.id + "-" + state.revision,
        day = state.currentDay,
        challengerId = state.player.id,
        outcome = outcome,
        challengerPower = power,
        dragonPower = DragonSeasonBalance.ABSTRACT_DRAGON_POWER,
    )

    private fun attemptEvent(
        state: GameState,
        attempt: DragonAttemptRecord,
        type: WorldEventType,
    ) = WorldEvent(
        id = attempt.id,
        day = state.currentDay,
        type = type,
        characterId = state.player.id,
        detail = attempt.id,
        value = attempt.challengerPower,
        importance = 3,
        visibility = WorldEventVisibility.PUBLIC,
        location = WorldEventLocation.FOREST,
        directlyAffectsPlayer = true,
    )

    private fun appendAttempt(
        attempts: List<DragonAttemptRecord>,
        attempt: DragonAttemptRecord,
    ) = (attempts + attempt).takeLast(DragonSeasonBalance.MAX_ATTEMPTS)

    private fun append(messages: List<CombatMessage>, message: CombatMessage) =
        (messages + message).takeLast(GameBalance.MAX_COMBAT_MESSAGES)

    private fun stale(state: GameState, expectedRevision: Long): GameActionResult.Rejected? =
        if (state.revision == expectedRevision) null else rejected(GameActionRejection.StaleState)

    private fun rejected(reason: GameActionRejection) = GameActionResult.Rejected(reason)

    private fun applied(state: GameState) =
        GameActionResult.Applied(state.copy(revision = state.revision + 1))
}

object SeasonNarrator {
    fun victorName(state: GameState): String = when (state.season.victorId) {
        state.player.id -> state.player.name
        else -> state.community.adventurers.firstOrNull { it.id == state.season.victorId }?.name
            ?: "An unknown champion"
    }

    fun finalChronicle(state: GameState): List<String> {
        val victor = victorName(state)
        val failures = state.season.attempts.filter { it.outcome != DragonAttemptOutcome.VICTORY }
        val rival = state.community.adventurers.maxByOrNull { it.rivalry }?.takeIf { it.rivalry >= 3 }
        val leader = CommunityRules.rankings(state).firstOrNull()?.name
        return buildList {
            add("For " + state.season.resolvedDay + " days the Glass Dragon haunted the realm.")
            if (failures.isNotEmpty()) {
                add(failures.size.toString() + " brave attempt" + if (failures.size == 1) " failed" else "s failed" +
                    " before the crystal threat was ended.")
            }
            rival?.let {
                add(state.player.name + " and " + it.name + " became remembered rivals during the season.")
            }
            if (leader != null && leader != victor) {
                add(leader + " led the public rankings when the final crisis came.")
            }
            add("On Day " + state.season.resolvedDay + ", " + victor + " entered the crystal wastes.")
            add("The Glass Dragon did not return.")
        }
    }
}

object DragonEventNarrator {
    fun chronicle(event: WorldEvent, state: GameState): String = when (event.type) {
        WorldEventType.DRAGON_INCIDENT -> incident(event, state, dawn = false)
        WorldEventType.DRAGON_INTENT ->
            name(event.characterId, state) + " publicly declared an intention to face the Glass Dragon."
        WorldEventType.DRAGON_ATTEMPT -> attempt(event, state, victory = false)
        WorldEventType.DRAGON_VICTORY -> attempt(event, state, victory = true)
        WorldEventType.DRAGON_SUPPORT ->
            state.player.name + " gave " + event.value + " gold to help " +
                name(event.relatedCharacterId.orEmpty(), state) + " prepare for the Dragon."
        else -> "The Dragon's shadow lengthened across the realm."
    }

    fun dawn(event: WorldEvent, state: GameState): String = when (event.type) {
        WorldEventType.DRAGON_INCIDENT -> incident(event, state, dawn = true)
        WorldEventType.DRAGON_INTENT ->
            name(event.characterId, state) + " has announced a claim to become the realm's champion."
        WorldEventType.DRAGON_ATTEMPT ->
            name(event.characterId, state) + " returned wounded from the Dragon's territory."
        WorldEventType.DRAGON_VICTORY ->
            "The Glass Dragon is dead. " + name(event.characterId, state) + " is the realm's victor."
        WorldEventType.DRAGON_SUPPORT ->
            "Your support for " + name(event.relatedCharacterId.orEmpty(), state) + " is now known at the Inn."
        else -> chronicle(event, state)
    }

    private fun incident(event: WorldEvent, state: GameState, dawn: Boolean): String {
        val incident = state.season.incidents.firstOrNull { it.id == event.detail }
        return when (incident?.type) {
            DragonIncidentType.DISTANT_CHIMES -> if (dawn) {
                "Travelers report faint chiming beyond the western ridge."
            } else {
                "Something vast moved beyond the western ridge, ringing like distant glass."
            }
            DragonIncidentType.GLASS_FRAGMENTS ->
                "Clear, razor-edged fragments were found fused into the forest road."
            DragonIncidentType.MISSING_TRAVELERS ->
                "Three travelers failed to reach the village; only mirrored footprints remained."
            DragonIncidentType.BURNED_WOODLAND ->
                "A stretch of woodland was found burned white and brittle as glass."
            DragonIncidentType.WOUNDED_SCOUT ->
                "A scout staggered home bearing wounds that reflected the morning light."
            DragonIncidentType.SHATTERED_HOMESTEAD ->
                "A hillside homestead shattered overnight beneath a rain of crystalline scales."
            DragonIncidentType.DRAGON_SIGHTING ->
                "The Glass Dragon was seen openly above the western ridge."
            DragonIncidentType.CHAMPION_CALL ->
                "The village council called upon its strongest adventurers to face the Glass Dragon."
            null -> "A new sign of the Glass Dragon appeared beyond the village walls."
        }
    }

    private fun attempt(event: WorldEvent, state: GameState, victory: Boolean): String {
        val challenger = name(event.characterId, state)
        val attempt = state.season.attempts.firstOrNull { it.id == event.detail }
        if (victory || attempt?.outcome == DragonAttemptOutcome.VICTORY) {
            return challenger + " entered the crystal wastes and slew the Glass Dragon."
        }
        return when (attempt?.outcome) {
            DragonAttemptOutcome.RETREAT ->
                challenger + " reached the Dragon's territory, but wisely withdrew before the final blow."
            DragonAttemptOutcome.NARROW_DEFEAT ->
                challenger + " nearly broke the Dragon's crystalline hide, then returned gravely wounded."
            DragonAttemptOutcome.DECISIVE_DEFEAT ->
                challenger + " challenged the Glass Dragon and was cast broken from the crystal wastes."
            else -> challenger + " attempted to confront the Glass Dragon and did not prevail."
        }
    }

    private fun name(id: String, state: GameState): String = when (id) {
        state.player.id -> state.player.name
        else -> state.community.adventurers.firstOrNull { it.id == id }?.name ?: "An adventurer"
    }
}
