package com.legendoftheglassdragon.game

import java.util.Random
import kotlin.math.abs
import kotlin.math.max

object GameBalance {
    const val FOREST_FIGHTS_PER_DAY = 10
    const val DAMAGE_RANDOM_VALUES = 3
    const val MINIMUM_DAMAGE = 1
    const val ESCAPE_CHANCE_PERCENT = 65
    const val DEFEAT_GOLD_LOSS_PERCENT = 25
    const val DEFEAT_RECOVERY_HP_DIVISOR = 2
    const val HEALING_GOLD_PER_HP = 1
    const val LEVEL_MAX_HP_GAIN = 5
    const val LEVEL_ATTACK_GAIN = 2
    const val LEVEL_DEFENSE_GAIN = 1
    const val MAX_COMBAT_MESSAGES = 6
    const val CHALLENGES_PER_DAY = 1
    const val CHALLENGE_DEFEAT_RECOVERY_HP_DIVISOR = 2
    const val CHALLENGE_RIVALRY_WIN_GAIN = 3
    const val CHALLENGE_RIVALRY_LOSS_GAIN = 2
    const val CHALLENGE_RIVALRY_YIELD_GAIN = 1
    const val MAX_RIVALRY = 100
    const val TRAINING_BASE_SUCCESS_PERCENT = 80
    const val TRAINING_FULL_HEALTH_BONUS_PERCENT = 5
    const val TRAINING_EQUIPMENT_BONUS_PERCENT = 3
    const val TRAINING_FAILURE_HP_DIVISOR = 4
}

object ProgressionRules {
    /**
     * Cumulative thresholds tuned around ten daily fights and the intended 30-day campaign arc.
     * Early promotions teach the loop quickly; later gaps broaden without a sudden final wall.
     */
    private val campaignThresholds = intArrayOf(
        0,      // unused level zero
        0,      // Level 1
        50,     // Level 2: Day 1-2
        250,    // Level 3: around Day 3
        600,    // Level 4: around Day 5
        1_250,  // Level 5: around Day 8
        2_800,  // Level 6: around Day 11-13
        5_600,  // Level 7: around Day 16-18
        9_500,  // Level 8: around Day 21-23
        14_500, // Level 9: around Day 26-29
        20_500, // Level 10: exceptional late-campaign promotion
    )

    fun experienceRequiredForLevel(level: Int): Int {
        require(level >= 1)
        if (level < campaignThresholds.size) return campaignThresholds[level]

        val levelsBeyondCampaign = (level - 10).toLong()
        val continuedProgression = 20_500L +
            7_000L * levelsBeyondCampaign +
            500L * levelsBeyondCampaign * (levelsBeyondCampaign - 1L)
        return continuedProgression.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun experienceRequiredForNextLevel(player: PlayerCharacter): Int =
        experienceRequiredForLevel(player.level + 1)

    fun isTrainingEligible(player: PlayerCharacter): Boolean =
        player.experience >= experienceRequiredForNextLevel(player)

    fun experienceStillNeeded(player: PlayerCharacter): Int =
        (experienceRequiredForNextLevel(player) - player.experience).coerceAtLeast(0)
}

data class RandomRoll(
    val value: Int,
    val world: WorldState,
)

fun interface GameRandom {
    fun nextInt(world: WorldState, bound: Int): RandomRoll
}

/**
 * Every roll is derived from the saved seed and counter. Advancing the counter in GameState makes
 * encounters and combat deterministic across process death without scattering Random calls.
 */
object PersistedGameRandom : GameRandom {
    override fun nextInt(world: WorldState, bound: Int): RandomRoll {
        require(bound > 0)
        val mixedSeed = world.seed xor (world.randomStep * -7046029254386353131L)
        return RandomRoll(
            value = Random(mixedSeed).nextInt(bound),
            world = world.copy(randomStep = world.randomStep + 1),
        )
    }
}

object MonsterSelector {
    fun candidatesForLevel(playerLevel: Int): List<MonsterDefinition> {
        val exact = MonsterCatalog.all.filter { it.intendedLevel == playerLevel }
        if (exact.isNotEmpty()) {
            if (playerLevel == 1) return exact
            val priorTier = MonsterCatalog.all
                .filter { it.intendedLevel == playerLevel - 1 }
                .take(exact.size)
            // Current foes are twice as common; old foes remain as visibly easier encounters.
            return exact + exact + priorTier
        }

        val nearestDistance = MonsterCatalog.all.minOf { abs(it.intendedLevel - playerLevel) }
        return MonsterCatalog.all.filter { abs(it.intendedLevel - playerLevel) == nearestDistance }
    }

    fun select(
        playerLevel: Int,
        world: WorldState,
        random: GameRandom,
    ): Pair<MonsterDefinition, WorldState> {
        val candidates = candidatesForLevel(playerLevel)
        val roll = random.nextInt(world, candidates.size)
        return candidates[roll.value] to roll.world
    }
}

sealed interface GameActionResult {
    data class Applied(val gameState: GameState) : GameActionResult
    data class Rejected(val reason: GameActionRejection) : GameActionResult
}

sealed interface GameActionRejection {
    data object StaleState : GameActionRejection
    data object ActiveCombat : GameActionRejection
    data object NoActiveCombat : GameActionRejection
    data object CombatAlreadyComplete : GameActionRejection
    data object NoForestFights : GameActionRejection
    data object NoHealingNeeded : GameActionRejection
    data class NotEnoughGold(val required: Int) : GameActionRejection
    data class TrainingNotReady(val experienceNeeded: Int) : GameActionRejection
    data object TrainingAttemptUsedToday : GameActionRejection
    data object NoCommunityChange : GameActionRejection
    data object UnknownEquipment : GameActionRejection
    data object EquipmentNotUpgrade : GameActionRejection
    data class EquipmentLevelRequired(val level: Int) : GameActionRejection
    data class NotEnoughCarriedGold(val required: Int) : GameActionRejection
    data class NotEnoughBankedGold(val required: Int) : GameActionRejection
    data object InvalidGoldAmount : GameActionRejection
    data object ActiveChallenge : GameActionRejection
    data object NoActiveChallenge : GameActionRejection
    data object ChallengeAlreadyComplete : GameActionRejection
    data object NoChallengesRemaining : GameActionRejection
    data object ChallengeOpponentUnavailable : GameActionRejection
    data object PendingDayTransition : GameActionRejection
    data object NoPendingDayTransition : GameActionRejection
    data object InvalidDayTransitionPhase : GameActionRejection
    data object SeasonResolved : GameActionRejection
    data object DragonChallengeClosed : GameActionRejection
    data object DragonNotReady : GameActionRejection
    data object DragonAttemptUsedToday : GameActionRejection
    data object ActiveDragonCombat : GameActionRejection
    data object NoActiveDragonCombat : GameActionRejection
    data object DragonCombatAlreadyComplete : GameActionRejection
    data object InvalidDragonSupport : GameActionRejection
}

class GameEngine(
    private val random: GameRandom = PersistedGameRandom,
) {
    fun startForestEncounter(state: GameState, expectedRevision: Long): GameActionResult {
        stale(state, expectedRevision)?.let { return it }
        if (state.activeCombat != null) return GameActionResult.Rejected(GameActionRejection.ActiveCombat)
        if (state.activeChallenge != null) return GameActionResult.Rejected(GameActionRejection.ActiveChallenge)
        if (state.activeDragonCombat != null) return GameActionResult.Rejected(GameActionRejection.ActiveDragonCombat)
        if (state.playerDay.fightsRemaining <= 0 || state.playerDay.hasEndedCurrentDay ||
            state.playerDay.wasDefeatedToday
        ) {
            return GameActionResult.Rejected(GameActionRejection.NoForestFights)
        }
        return startEncounter(state)
    }

    fun attack(state: GameState, expectedRevision: Long): GameActionResult {
        stale(state, expectedRevision)?.let { return it }
        val combat = state.activeCombat
            ?: return GameActionResult.Rejected(GameActionRejection.NoActiveCombat)
        if (combat.status != CombatStatus.ACTIVE) {
            return GameActionResult.Rejected(GameActionRejection.CombatAlreadyComplete)
        }
        val monster = MonsterCatalog.require(combat.monsterId)

        val playerRoll = random.nextInt(state.world, GameBalance.DAMAGE_RANDOM_VALUES)
        val playerDamage = damage(state.player.attack, monster.defense, playerRoll.value)
        val monsterHp = (combat.monsterCurrentHp - playerDamage).coerceAtLeast(0)
        var messages = appendMessages(
            combat.recentMessages,
            CombatMessage(
                "You strike the " + monster.displayName + " for " + playerDamage + " damage!",
                CombatMessageKind.PLAYER_ACTION,
            ),
        )

        if (monsterHp == 0) {
            val goldRange = monster.maximumGoldReward - monster.minimumGoldReward + 1
            val goldRoll = random.nextInt(playerRoll.world, goldRange)
            val goldReward = monster.minimumGoldReward + goldRoll.value
            val rewardedPlayer = state.player.copy(
                experience = safeAdd(state.player.experience, monster.experienceReward),
                gold = safeAdd(state.player.gold, goldReward),
            )
            messages = appendMessages(
                messages,
                CombatMessage(
                    "*** VICTORY! The " + monster.displayName + " falls! ***",
                    CombatMessageKind.VICTORY,
                ),
            )
            return applied(
                state.copy(
                    player = rewardedPlayer,
                    world = goldRoll.world,
                    activeCombat = combat.copy(
                        monsterCurrentHp = 0,
                        status = CombatStatus.VICTORY,
                        recentMessages = messages,
                        escapeAvailable = false,
                        rewardsGranted = true,
                        experienceReward = monster.experienceReward,
                        goldReward = goldReward,
                    ),
                ),
            )
        }

        val monsterRoll = random.nextInt(playerRoll.world, GameBalance.DAMAGE_RANDOM_VALUES)
        val monsterDamage = damage(monster.attack, state.player.defense, monsterRoll.value)
        val playerHp = (state.player.currentHp - monsterDamage).coerceAtLeast(0)
        messages = appendMessages(
            messages,
            CombatMessage(
                "The " + monster.displayName + " hits you for " + monsterDamage + " damage!",
                CombatMessageKind.MONSTER_ACTION,
            ),
        )

        if (playerHp == 0) {
            return defeat(
                state = state.copy(world = monsterRoll.world),
                combat = combat.copy(monsterCurrentHp = monsterHp),
                messages = messages,
            )
        }

        return applied(
            state.copy(
                world = monsterRoll.world,
                player = state.player.copy(currentHp = playerHp),
                activeCombat = combat.copy(
                    monsterCurrentHp = monsterHp,
                    recentMessages = messages,
                ),
            ),
        )
    }

    fun runAway(state: GameState, expectedRevision: Long): GameActionResult {
        stale(state, expectedRevision)?.let { return it }
        val combat = state.activeCombat
            ?: return GameActionResult.Rejected(GameActionRejection.NoActiveCombat)
        if (combat.status != CombatStatus.ACTIVE || !combat.escapeAvailable) {
            return GameActionResult.Rejected(GameActionRejection.CombatAlreadyComplete)
        }
        val monster = MonsterCatalog.require(combat.monsterId)
        val escapeRoll = random.nextInt(state.world, 100)
        if (escapeRoll.value < GameBalance.ESCAPE_CHANCE_PERCENT) {
            return applied(
                state.copy(
                    world = escapeRoll.world,
                    activeCombat = combat.copy(
                        status = CombatStatus.ESCAPED,
                        recentMessages = appendMessages(
                            combat.recentMessages,
                            CombatMessage(
                                "You escape into the trees. No reward is earned.",
                                CombatMessageKind.ESCAPE,
                            ),
                        ),
                        escapeAvailable = false,
                    ),
                ),
            )
        }

        val monsterRoll = random.nextInt(escapeRoll.world, GameBalance.DAMAGE_RANDOM_VALUES)
        val monsterDamage = damage(monster.attack, state.player.defense, monsterRoll.value)
        val playerHp = (state.player.currentHp - monsterDamage).coerceAtLeast(0)
        var messages = appendMessages(
            combat.recentMessages,
            CombatMessage("You fail to escape!", CombatMessageKind.ESCAPE),
        )
        messages = appendMessages(
            messages,
            CombatMessage(
                "The " + monster.displayName + " hits you for " + monsterDamage + " damage!",
                CombatMessageKind.MONSTER_ACTION,
            ),
        )
        if (playerHp == 0) {
            return defeat(
                state = state.copy(world = monsterRoll.world),
                combat = combat,
                messages = messages,
            )
        }
        return applied(
            state.copy(
                world = monsterRoll.world,
                player = state.player.copy(currentHp = playerHp),
                activeCombat = combat.copy(recentMessages = messages),
            ),
        )
    }

    fun searchAgain(state: GameState, expectedRevision: Long): GameActionResult {
        stale(state, expectedRevision)?.let { return it }
        val combat = state.activeCombat
            ?: return GameActionResult.Rejected(GameActionRejection.NoActiveCombat)
        if (combat.status == CombatStatus.ACTIVE) {
            return GameActionResult.Rejected(GameActionRejection.ActiveCombat)
        }
        if (state.playerDay.fightsRemaining <= 0) {
            return GameActionResult.Rejected(GameActionRejection.NoForestFights)
        }
        return startEncounter(state.copy(activeCombat = null))
    }

    fun leaveCompletedCombat(state: GameState, expectedRevision: Long): GameActionResult {
        stale(state, expectedRevision)?.let { return it }
        val combat = state.activeCombat
            ?: return GameActionResult.Rejected(GameActionRejection.NoActiveCombat)
        if (combat.status == CombatStatus.ACTIVE) {
            return GameActionResult.Rejected(GameActionRejection.ActiveCombat)
        }
        return applied(state.copy(activeCombat = null))
    }

    fun healFully(state: GameState, expectedRevision: Long): GameActionResult {
        stale(state, expectedRevision)?.let { return it }
        if (state.activeCombat != null) return GameActionResult.Rejected(GameActionRejection.ActiveCombat)
        if (state.activeChallenge != null) return GameActionResult.Rejected(GameActionRejection.ActiveChallenge)
        if (state.activeDragonCombat != null) return GameActionResult.Rejected(GameActionRejection.ActiveDragonCombat)
        val missingHp = state.player.maximumHp - state.player.currentHp
        if (missingHp == 0) return GameActionResult.Rejected(GameActionRejection.NoHealingNeeded)
        val cost = missingHp * GameBalance.HEALING_GOLD_PER_HP
        if (state.player.gold < cost) {
            return GameActionResult.Rejected(GameActionRejection.NotEnoughGold(cost))
        }
        return applied(
            state.copy(
                player = state.player.copy(
                    currentHp = state.player.maximumHp,
                    gold = state.player.gold - cost,
                ),
            ),
        )
    }

    fun train(state: GameState, expectedRevision: Long): GameActionResult {
        stale(state, expectedRevision)?.let { return it }
        if (state.activeCombat != null) return GameActionResult.Rejected(GameActionRejection.ActiveCombat)
        if (state.activeChallenge != null) return GameActionResult.Rejected(GameActionRejection.ActiveChallenge)
        if (state.activeDragonCombat != null) return GameActionResult.Rejected(GameActionRejection.ActiveDragonCombat)
        if (state.playerDay.trainingAttemptedToday) {
            return GameActionResult.Rejected(GameActionRejection.TrainingAttemptUsedToday)
        }
        if (!ProgressionRules.isTrainingEligible(state.player)) {
            return GameActionResult.Rejected(
                GameActionRejection.TrainingNotReady(
                    ProgressionRules.experienceStillNeeded(state.player),
                ),
            )
        }
        val weaponTier = EquipmentCatalog.requireWeapon(state.player.weaponId).tier
        val armorTier = EquipmentCatalog.requireArmor(state.player.armorId).tier
        val fullHealthBonus = if (state.player.currentHp * 4 >= state.player.maximumHp * 3) {
            GameBalance.TRAINING_FULL_HEALTH_BONUS_PERCENT
        } else {
            0
        }
        val equipmentBonus = if (weaponTier + armorTier >= minOf(10, state.player.level + 2)) {
            GameBalance.TRAINING_EQUIPMENT_BONUS_PERCENT
        } else {
            0
        }
        val successChance = (
            GameBalance.TRAINING_BASE_SUCCESS_PERCENT + fullHealthBonus + equipmentBonus
            ).coerceIn(1, 95)
        val trainingRoll = random.nextInt(state.world, 100)
        if (trainingRoll.value >= successChance) {
            val healthLoss = max(1, state.player.maximumHp / GameBalance.TRAINING_FAILURE_HP_DIVISOR)
            return applied(
                state.copy(
                    world = trainingRoll.world,
                    player = state.player.copy(
                        currentHp = (state.player.currentHp - healthLoss).coerceAtLeast(1),
                    ),
                    playerDay = state.playerDay.copy(
                        trainingAttemptedToday = true,
                        lastTrainingSucceeded = false,
                    ),
                ),
            )
        }
        val newMaximumHp = safeAdd(state.player.maximumHp, GameBalance.LEVEL_MAX_HP_GAIN)
        val promoted = state.copy(
            world = trainingRoll.world,
            player = state.player.copy(
                level = safeAdd(state.player.level, 1),
                currentHp = newMaximumHp,
                maximumHp = newMaximumHp,
                baseAttack = safeAdd(state.player.baseAttack, GameBalance.LEVEL_ATTACK_GAIN),
                baseDefense = safeAdd(state.player.baseDefense, GameBalance.LEVEL_DEFENSE_GAIN),
            ),
            playerDay = state.playerDay.copy(
                trainingAttemptedToday = true,
                lastTrainingSucceeded = true,
            ),
        )
        return applied(
            WorldEventLedger.append(
                promoted,
                WorldEvent(
                    id = "day-" + state.currentDay + "-player-level-" + state.revision,
                    day = state.currentDay,
                    type = WorldEventType.LEVEL_GAIN,
                    characterId = state.player.id,
                    value = promoted.player.level,
                    importance = 3,
                    visibility = WorldEventVisibility.PUBLIC,
                    location = WorldEventLocation.TRAINING_HALL,
                    beforeValue = state.player.level,
                    afterValue = promoted.player.level,
                    directlyAffectsPlayer = true,
                ),
            ),
        )
    }

    private fun startEncounter(state: GameState): GameActionResult {
        val (monster, nextWorld) = MonsterSelector.select(state.player.level, state.world, random)
        return applied(
            state.copy(
                world = nextWorld,
                playerDay = state.playerDay.copy(
                    fightsRemaining = (state.playerDay.fightsRemaining - 1).coerceAtLeast(0),
                ),
                activeCombat = CombatState(
                    monsterId = monster.id,
                    monsterCurrentHp = monster.maximumHp,
                    status = CombatStatus.ACTIVE,
                    fightConsumed = true,
                    recentMessages = listOf(
                        CombatMessage(monster.encounterText, CombatMessageKind.ENCOUNTER),
                    ),
                    escapeAvailable = true,
                    rewardsGranted = false,
                ),
            ),
        )
    }

    private fun defeat(
        state: GameState,
        combat: CombatState,
        messages: List<CombatMessage>,
    ): GameActionResult {
        val loss = ((state.player.gold.toLong() * GameBalance.DEFEAT_GOLD_LOSS_PERCENT + 99) / 100)
            .coerceAtMost(state.player.gold.toLong())
            .toInt()
        val recoveryHp = max(1, state.player.maximumHp / GameBalance.DEFEAT_RECOVERY_HP_DIVISOR)
        val defeated = state.copy(
                player = state.player.copy(
                    currentHp = recoveryHp,
                    gold = state.player.gold - loss,
                ),
                playerDay = state.playerDay.copy(
                    fightsRemaining = 0,
                    wasDefeatedToday = true,
                ),
                activeCombat = combat.copy(
                    status = CombatStatus.DEFEAT,
                    recentMessages = appendMessages(
                        messages,
                        CombatMessage(
                            "Darkness takes you. Your fighting day is over.",
                            CombatMessageKind.DEFEAT,
                        ),
                    ),
                    escapeAvailable = false,
                    rewardsGranted = false,
                    experienceReward = 0,
                    goldReward = 0,
                    goldLost = loss,
                ),
            )
        return applied(
            WorldEventLedger.append(
                defeated,
                WorldEvent(
                    id = "day-" + state.currentDay + "-player-defeat-" + state.revision,
                    day = state.currentDay,
                    type = WorldEventType.DEFEAT,
                    characterId = state.player.id,
                    value = loss,
                    detail = MonsterCatalog.require(combat.monsterId).displayName,
                    importance = 3,
                    visibility = WorldEventVisibility.PUBLIC,
                    location = WorldEventLocation.FOREST,
                    beforeValue = state.player.gold,
                    afterValue = defeated.player.gold,
                    directlyAffectsPlayer = true,
                ),
            ),
        )
    }

    private fun stale(state: GameState, expectedRevision: Long): GameActionResult.Rejected? =
        if (state.revision == expectedRevision) null
        else GameActionResult.Rejected(GameActionRejection.StaleState)

    private fun applied(state: GameState): GameActionResult.Applied =
        GameActionResult.Applied(state.copy(revision = state.revision + 1))

    private fun damage(strength: Int, defense: Int, variation: Int): Int =
        max(GameBalance.MINIMUM_DAMAGE, strength + variation - defense)

    private fun appendMessages(
        existing: List<CombatMessage>,
        message: CombatMessage,
    ): List<CombatMessage> = (existing + message).takeLast(GameBalance.MAX_COMBAT_MESSAGES)

    private fun safeAdd(value: Int, amount: Int): Int =
        (value.toLong() + amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
