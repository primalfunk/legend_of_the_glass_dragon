package com.legendoftheglassdragon.data

import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.GameStateValidationResult
import com.legendoftheglassdragon.game.GameStateValidator
import com.legendoftheglassdragon.game.PlayerCharacter
import com.legendoftheglassdragon.game.PlayerDayState
import com.legendoftheglassdragon.game.WorldState
import com.legendoftheglassdragon.game.CombatMessage
import com.legendoftheglassdragon.game.CombatMessageKind
import com.legendoftheglassdragon.game.CombatState
import com.legendoftheglassdragon.game.CombatStatus
import com.legendoftheglassdragon.game.GameBalance
import com.legendoftheglassdragon.game.CommunityFactory
import com.legendoftheglassdragon.game.CommunityState
import com.legendoftheglassdragon.game.NpcAdventurer
import com.legendoftheglassdragon.game.NpcPersonality
import com.legendoftheglassdragon.game.NpcStatus
import com.legendoftheglassdragon.game.WorldEvent
import com.legendoftheglassdragon.game.WorldEventLocation
import com.legendoftheglassdragon.game.WorldEventType
import com.legendoftheglassdragon.game.WorldEventVisibility
import com.legendoftheglassdragon.game.ChronicleRecord
import com.legendoftheglassdragon.game.ChronicleRules
import com.legendoftheglassdragon.game.DayTransitionPhase
import com.legendoftheglassdragon.game.DayTransitionState
import com.legendoftheglassdragon.game.DragonAttemptOutcome
import com.legendoftheglassdragon.game.DragonAttemptRecord
import com.legendoftheglassdragon.game.DragonCombatState
import com.legendoftheglassdragon.game.DragonCombatStatus
import com.legendoftheglassdragon.game.DragonIncidentRecord
import com.legendoftheglassdragon.game.DragonIncidentType
import com.legendoftheglassdragon.game.DragonSeasonBalance
import com.legendoftheglassdragon.game.DragonSeasonState
import com.legendoftheglassdragon.game.DragonStage
import com.legendoftheglassdragon.game.SeasonConclusionPhase
import com.legendoftheglassdragon.game.ProgressionRules
import com.legendoftheglassdragon.game.StartingCharacterRules
import com.legendoftheglassdragon.game.EquipmentCatalog
import com.legendoftheglassdragon.game.ChallengeState
import com.legendoftheglassdragon.game.ChallengeStatus
import com.legendoftheglassdragon.game.TrophyCampaignStats
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties

internal sealed interface DecodeResult {
    data class Success(val gameState: GameState) : DecodeResult
    data class Failure(val failure: SaveFailure) : DecodeResult
}

internal class GameStateCodec {
    fun encode(state: GameState): String {
        require(GameStateValidator.validate(state) is GameStateValidationResult.Valid) {
            "Refusing to encode an invalid game state"
        }

        val values = Properties().apply {
            setProperty(KEY_VERSION, state.saveVersion.toString())
            setProperty(KEY_REVISION, state.revision.toString())
            setProperty(KEY_CURRENT_DAY, state.currentDay.toString())
            setProperty(KEY_WORLD_SEED, state.world.seed.toString())
            setProperty(KEY_WORLD_RANDOM_STEP, state.world.randomStep.toString())
            setProperty(KEY_PLAYER_ID, state.player.id)
            setProperty(KEY_PLAYER_NAME, state.player.name)
            setProperty(KEY_PLAYER_LEVEL, state.player.level.toString())
            setProperty(KEY_PLAYER_EXPERIENCE, state.player.experience.toString())
            setProperty(KEY_PLAYER_CURRENT_HP, state.player.currentHp.toString())
            setProperty(KEY_PLAYER_MAXIMUM_HP, state.player.maximumHp.toString())
            setProperty(KEY_PLAYER_BASE_ATTACK, state.player.baseAttack.toString())
            setProperty(KEY_PLAYER_BASE_DEFENSE, state.player.baseDefense.toString())
            setProperty(KEY_PLAYER_GOLD, state.player.gold.toString())
            setProperty(KEY_PLAYER_BANKED_GOLD, state.player.bankedGold.toString())
            setProperty(KEY_PLAYER_WEAPON_ID, state.player.weaponId)
            setProperty(KEY_PLAYER_ARMOR_ID, state.player.armorId)
            setProperty(KEY_PLAYER_CHALLENGE_WINS, state.player.challengeWins.toString())
            setProperty(KEY_PLAYER_CHALLENGE_LOSSES, state.player.challengeLosses.toString())
            setProperty(KEY_DAY_CURRENT, state.playerDay.currentDay.toString())
            setProperty(KEY_DAY_FIGHTS, state.playerDay.fightsRemaining.toString())
            setProperty(KEY_DAY_ENDED, state.playerDay.hasEndedCurrentDay.toString())
            setProperty(KEY_DAY_DEFEATED, state.playerDay.wasDefeatedToday.toString())
            setProperty(KEY_DAY_CHALLENGES, state.playerDay.challengesRemaining.toString())
            setProperty(KEY_DAY_TRAINING_ATTEMPTED, state.playerDay.trainingAttemptedToday.toString())
            setProperty(KEY_DAY_TRAINING_SUCCEEDED, state.playerDay.lastTrainingSucceeded?.toString().orEmpty())
            setProperty(KEY_WELCOME_SHOWN, state.welcomeShown.toString())
            writeTrophyStats(state.trophyStats)
            setProperty(KEY_COMMUNITY_COUNT, state.community.adventurers.size.toString())
            setProperty(KEY_COMMUNITY_LAST_DAY, state.community.lastSimulatedDay.toString())
            state.community.adventurers.forEachIndexed { index, npc ->
                val prefix = KEY_NPC_PREFIX + index + "."
                setProperty(prefix + "id", npc.id)
                setProperty(prefix + "name", npc.name)
                setProperty(prefix + "level", npc.level.toString())
                setProperty(prefix + "experience", npc.experience.toString())
                setProperty(prefix + "maximumHp", npc.maximumHp.toString())
                setProperty(prefix + "baseAttack", npc.baseAttack.toString())
                setProperty(prefix + "baseDefense", npc.baseDefense.toString())
                setProperty(prefix + "gold", npc.gold.toString())
                setProperty(prefix + "weaponId", npc.weaponId)
                setProperty(prefix + "armorId", npc.armorId)
                setProperty(prefix + "status", npc.status.name)
                setProperty(prefix + "recoveryDays", npc.recoveryDaysRemaining.toString())
                setProperty(prefix + "personality", npc.personality.name)
                setProperty(prefix + "familiarity", npc.familiarity.toString())
                setProperty(prefix + "lastMetDay", npc.lastMetDay.toString())
                setProperty(prefix + "lastAdvancedDay", npc.lastAdvancedDay.toString())
                setProperty(prefix + "rivalry", npc.rivalry.toString())
                setProperty(prefix + "playerWinsAgainst", npc.playerWinsAgainst.toString())
                setProperty(prefix + "playerLossesAgainst", npc.playerLossesAgainst.toString())
                setProperty(prefix + "challengeWins", npc.challengeWins.toString())
                setProperty(prefix + "challengeLosses", npc.challengeLosses.toString())
                setProperty(prefix + "lastChallengeDay", npc.lastChallengeDay.toString())
            }
            setProperty(KEY_EVENT_COUNT, state.community.recentEvents.size.toString())
            state.community.recentEvents.forEachIndexed { index, event ->
                writeEvent(KEY_EVENT_PREFIX + index + ".", event)
            }
            setProperty(KEY_CHRONICLE_COUNT, state.community.chronicleHistory.size.toString())
            state.community.chronicleHistory.forEachIndexed { recordIndex, record ->
                val prefix = KEY_CHRONICLE_PREFIX + recordIndex + "."
                setProperty(prefix + "day", record.day.toString())
                setProperty(prefix + "events.count", record.events.size.toString())
                record.events.forEachIndexed { eventIndex, event ->
                    writeEvent(prefix + "events." + eventIndex + ".", event)
                }
            }
            setProperty(KEY_TRANSITION_PRESENT, (state.pendingDayTransition != null).toString())
            state.pendingDayTransition?.let { transition ->
                setProperty(KEY_TRANSITION_RESOLVED_DAY, transition.resolvedDay.toString())
                setProperty(KEY_TRANSITION_NEXT_DAY, transition.nextDay.toString())
                setProperty(KEY_TRANSITION_PHASE, transition.phase.name)
            }
            setProperty(KEY_SEASON_STAGE, state.season.stage.name)
            setProperty(KEY_SEASON_PRESSURE, state.season.pressure.toString())
            setProperty(KEY_SEASON_STAGE_STARTED_DAY, state.season.stageStartedDay.toString())
            setProperty(KEY_SEASON_LAST_INCIDENT_DAY, state.season.lastIncidentDay.toString())
            setProperty(KEY_SEASON_PLAYER_LAST_ATTEMPT_DAY, state.season.playerLastAttemptDay.toString())
            setProperty(KEY_SEASON_VICTOR_ID, state.season.victorId.orEmpty())
            setProperty(KEY_SEASON_RESOLVED_DAY, state.season.resolvedDay.toString())
            setProperty(KEY_SEASON_CONCLUSION_PHASE, state.season.conclusionPhase?.name.orEmpty())
            setProperty(KEY_SEASON_DECLARED_COUNT, state.season.declaredChallengerIds.size.toString())
            state.season.declaredChallengerIds.forEachIndexed { index, id ->
                setProperty(KEY_SEASON_DECLARED_PREFIX + index, id)
            }
            setProperty(KEY_SEASON_INCIDENT_COUNT, state.season.incidents.size.toString())
            state.season.incidents.forEachIndexed { index, incident ->
                val prefix = KEY_SEASON_INCIDENT_PREFIX + index + "."
                setProperty(prefix + "id", incident.id)
                setProperty(prefix + "day", incident.day.toString())
                setProperty(prefix + "type", incident.type.name)
                setProperty(prefix + "stage", incident.stage.name)
                setProperty(prefix + "visibility", incident.visibility.name)
                setProperty(prefix + "residentId", incident.residentId.orEmpty())
            }
            setProperty(KEY_SEASON_ATTEMPT_COUNT, state.season.attempts.size.toString())
            state.season.attempts.forEachIndexed { index, attempt ->
                val prefix = KEY_SEASON_ATTEMPT_PREFIX + index + "."
                setProperty(prefix + "id", attempt.id)
                setProperty(prefix + "day", attempt.day.toString())
                setProperty(prefix + "challengerId", attempt.challengerId)
                setProperty(prefix + "outcome", attempt.outcome.name)
                setProperty(prefix + "challengerPower", attempt.challengerPower.toString())
                setProperty(prefix + "dragonPower", attempt.dragonPower.toString())
            }
            setProperty(KEY_COMBAT_PRESENT, (state.activeCombat != null).toString())
            state.activeCombat?.let { combat ->
                setProperty(KEY_COMBAT_MONSTER_ID, combat.monsterId)
                setProperty(KEY_COMBAT_MONSTER_HP, combat.monsterCurrentHp.toString())
                setProperty(KEY_COMBAT_STATUS, combat.status.name)
                setProperty(KEY_COMBAT_FIGHT_CONSUMED, combat.fightConsumed.toString())
                setProperty(KEY_COMBAT_ESCAPE_AVAILABLE, combat.escapeAvailable.toString())
                setProperty(KEY_COMBAT_REWARDS_GRANTED, combat.rewardsGranted.toString())
                setProperty(KEY_COMBAT_EXPERIENCE_REWARD, combat.experienceReward.toString())
                setProperty(KEY_COMBAT_GOLD_REWARD, combat.goldReward.toString())
                setProperty(KEY_COMBAT_GOLD_LOST, combat.goldLost.toString())
                setProperty(KEY_COMBAT_MESSAGE_COUNT, combat.recentMessages.size.toString())
                combat.recentMessages.forEachIndexed { index, message ->
                    setProperty(KEY_COMBAT_MESSAGE_PREFIX + index + ".text", message.text)
                    setProperty(KEY_COMBAT_MESSAGE_PREFIX + index + ".kind", message.kind.name)
                }
            }
            setProperty(KEY_CHALLENGE_PRESENT, (state.activeChallenge != null).toString())
            state.activeChallenge?.let { challenge ->
                setProperty(KEY_CHALLENGE_OPPONENT_ID, challenge.opponentId)
                setProperty(KEY_CHALLENGE_OPPONENT_HP, challenge.opponentCurrentHp.toString())
                setProperty(KEY_CHALLENGE_STATUS, challenge.status.name)
                setProperty(KEY_CHALLENGE_CONSUMED, challenge.opportunityConsumed.toString())
                setProperty(KEY_CHALLENGE_REWARDS_GRANTED, challenge.rewardsGranted.toString())
                setProperty(KEY_CHALLENGE_EXPERIENCE_REWARD, challenge.experienceReward.toString())
                setProperty(KEY_CHALLENGE_GOLD_TRANSFERRED, challenge.goldTransferred.toString())
                setProperty(KEY_CHALLENGE_MESSAGE_COUNT, challenge.recentMessages.size.toString())
                challenge.recentMessages.forEachIndexed { index, message ->
                    setProperty(KEY_CHALLENGE_MESSAGE_PREFIX + index + ".text", message.text)
                    setProperty(KEY_CHALLENGE_MESSAGE_PREFIX + index + ".kind", message.kind.name)
                }
            }
            setProperty(KEY_DRAGON_COMBAT_PRESENT, (state.activeDragonCombat != null).toString())
            state.activeDragonCombat?.let { combat ->
                setProperty(KEY_DRAGON_COMBAT_HP, combat.dragonCurrentHp.toString())
                setProperty(KEY_DRAGON_COMBAT_STATUS, combat.status.name)
                setProperty(KEY_DRAGON_COMBAT_TURN, combat.turnNumber.toString())
                setProperty(KEY_DRAGON_COMBAT_HIDE_BROKEN, combat.crystalHideBroken.toString())
                setProperty(KEY_DRAGON_COMBAT_ATTEMPT_RECORDED, combat.attemptRecorded.toString())
                setProperty(KEY_DRAGON_COMBAT_GOLD_LOST, combat.goldLost.toString())
                setProperty(KEY_DRAGON_COMBAT_MESSAGE_COUNT, combat.recentMessages.size.toString())
                combat.recentMessages.forEachIndexed { index, message ->
                    setProperty(KEY_DRAGON_COMBAT_MESSAGE_PREFIX + index + ".text", message.text)
                    setProperty(KEY_DRAGON_COMBAT_MESSAGE_PREFIX + index + ".kind", message.kind.name)
                }
            }
        }

        return StringWriter().also { writer -> values.store(writer, null) }.toString()
    }

    fun decode(serialized: String): DecodeResult {
        val values = try {
            Properties().apply { load(StringReader(serialized)) }
        } catch (_: Exception) {
            return DecodeResult.Failure(SaveFailure.Corrupt("Save record cannot be parsed"))
        }

        val rawVersion = values.getProperty(KEY_VERSION)
            ?: return DecodeResult.Failure(SaveFailure.Corrupt("Save version is missing"))
        val version = rawVersion.toIntOrNull()
            ?: return DecodeResult.Failure(SaveFailure.Corrupt("Save version is unreadable"))
        if (version !in LEGACY_SAVE_VERSION..GameState.CURRENT_SAVE_VERSION) {
            return DecodeResult.Failure(SaveFailure.UnsupportedVersion(version))
        }

        val state = try {
            val currentDay = values.requiredInt(KEY_CURRENT_DAY)
            val worldSeed = values.requiredLong(KEY_WORLD_SEED)
            val playerWeaponId = if (version < ECONOMY_SAVE_VERSION) {
                EquipmentCatalog.weaponIdForLegacyValue(values.required(KEY_PLAYER_WEAPON))
                    ?: throw InvalidSaveValue()
            } else {
                values.required(KEY_PLAYER_WEAPON_ID)
            }
            val playerArmorId = if (version < ECONOMY_SAVE_VERSION) {
                EquipmentCatalog.armorIdForLegacyValue(values.required(KEY_PLAYER_ARMOR))
                    ?: throw InvalidSaveValue()
            } else {
                values.required(KEY_PLAYER_ARMOR_ID)
            }
            val playerBaseAttack = if (version < ECONOMY_SAVE_VERSION) {
                values.requiredInt(KEY_PLAYER_ATTACK) -
                    EquipmentCatalog.requireWeapon(playerWeaponId).attackContribution
            } else {
                values.requiredInt(KEY_PLAYER_BASE_ATTACK)
            }
            val playerBaseDefense = if (version < ECONOMY_SAVE_VERSION) {
                values.requiredInt(KEY_PLAYER_DEFENSE) -
                    EquipmentCatalog.requireArmor(playerArmorId).defenseContribution
            } else {
                values.requiredInt(KEY_PLAYER_BASE_DEFENSE)
            }
            val playerId = values.required(KEY_PLAYER_ID)
            val decodedCommunity = if (version < COMMUNITY_SAVE_VERSION) {
                CommunityFactory.create(
                    worldSeed = worldSeed,
                    lastSimulatedDay = currentDay - 1,
                )
            } else {
                values.communityState(version)
            }
            val legacyMorningPending = version in COMMUNITY_SAVE_VERSION until CHRONICLE_SAVE_VERSION &&
                values.optionalBoolean(KEY_COMMUNITY_MORNING_PENDING, default = false)
            val legacyRecord = if (legacyMorningPending && currentDay > 1) {
                ChronicleRecord(
                    day = currentDay - 1,
                    events = ChronicleRules.selectChronicleEvents(
                        decodedCommunity.recentEvents.filter { it.day == currentDay - 1 },
                        playerId,
                    ),
                )
            } else {
                null
            }
            val migratedCommunity = legacyRecord?.let { record ->
                decodedCommunity.copy(
                    chronicleHistory = ChronicleRules.appendRecord(
                        decodedCommunity.chronicleHistory,
                        record,
                    ),
                )
            } ?: decodedCommunity
            val decoded = GameState(
                saveVersion = GameState.CURRENT_SAVE_VERSION,
                revision = if (version == LEGACY_SAVE_VERSION) 0 else values.requiredLong(KEY_REVISION),
                currentDay = currentDay,
                player = PlayerCharacter(
                    id = playerId,
                    name = values.required(KEY_PLAYER_NAME),
                    level = values.requiredInt(KEY_PLAYER_LEVEL),
                    experience = values.requiredInt(KEY_PLAYER_EXPERIENCE),
                    currentHp = values.requiredInt(KEY_PLAYER_CURRENT_HP),
                    maximumHp = values.requiredInt(KEY_PLAYER_MAXIMUM_HP),
                    baseAttack = playerBaseAttack,
                    baseDefense = playerBaseDefense,
                    gold = values.requiredInt(KEY_PLAYER_GOLD),
                    bankedGold = if (version < ECONOMY_SAVE_VERSION) 0 else {
                        values.requiredInt(KEY_PLAYER_BANKED_GOLD)
                    },
                    weaponId = playerWeaponId,
                    armorId = playerArmorId,
                    challengeWins = if (version < ECONOMY_SAVE_VERSION) 0 else {
                        values.requiredInt(KEY_PLAYER_CHALLENGE_WINS)
                    },
                    challengeLosses = if (version < ECONOMY_SAVE_VERSION) 0 else {
                        values.requiredInt(KEY_PLAYER_CHALLENGE_LOSSES)
                    },
                ),
                playerDay = PlayerDayState(
                    currentDay = values.requiredInt(KEY_DAY_CURRENT),
                    fightsRemaining = values.requiredInt(KEY_DAY_FIGHTS),
                    hasEndedCurrentDay = values.requiredBoolean(KEY_DAY_ENDED),
                    wasDefeatedToday = if (version < DEFEAT_STATE_SAVE_VERSION) {
                        false
                    } else {
                        values.requiredBoolean(KEY_DAY_DEFEATED)
                    },
                    challengesRemaining = if (version < ECONOMY_SAVE_VERSION) {
                        GameBalance.CHALLENGES_PER_DAY
                    } else {
                        values.requiredInt(KEY_DAY_CHALLENGES)
                    },
                    trainingAttemptedToday = if (version < TRAINING_SAVE_VERSION) {
                        false
                    } else {
                        values.requiredBoolean(KEY_DAY_TRAINING_ATTEMPTED)
                    },
                    lastTrainingSucceeded = if (version < TRAINING_SAVE_VERSION) {
                        null
                    } else {
                        values.getProperty(KEY_DAY_TRAINING_SUCCEEDED)
                            ?.takeIf { it.isNotBlank() }
                            ?.let { encoded ->
                                encoded.toBooleanStrictOrNull() ?: throw InvalidSaveValue()
                            }
                    },
                ),
                world = WorldState(
                    seed = worldSeed,
                    randomStep = if (version == LEGACY_SAVE_VERSION) {
                        0
                    } else {
                        values.requiredLong(KEY_WORLD_RANDOM_STEP)
                    },
                ),
                community = migratedCommunity,
                season = if (version < DRAGON_SEASON_SAVE_VERSION) {
                    migratedSeason(currentDay)
                } else {
                    values.dragonSeasonState()
                },
                activeCombat = if (version == LEGACY_SAVE_VERSION) null else values.combatState(),
                activeChallenge = if (version < ECONOMY_SAVE_VERSION) null else {
                    values.challengeState()
                },
                activeDragonCombat = if (version < DRAGON_SEASON_SAVE_VERSION) null else {
                    values.dragonCombatState()
                },
                pendingDayTransition = if (version >= CHRONICLE_SAVE_VERSION) {
                    values.dayTransitionState()
                } else if (legacyRecord != null) {
                    DayTransitionState(
                        resolvedDay = currentDay - 1,
                        nextDay = currentDay,
                        phase = DayTransitionPhase.CHRONICLE,
                    )
                } else {
                    null
                },
                trophyStats = if (version >= TROPHY_STATS_SAVE_VERSION && values.getProperty(KEY_TROPHY_FOREST_WON) != null) values.trophyStats() else TrophyCampaignStats(
                    maxGoldHeld = values.requiredInt(KEY_PLAYER_GOLD),
                    bestExperience = values.requiredInt(KEY_PLAYER_EXPERIENCE),
                    weaponIdsOwned = listOf(playerWeaponId),
                    armorIdsOwned = listOf(playerArmorId),
                ),
                welcomeShown = if (version >= TROPHY_STATS_SAVE_VERSION && values.getProperty(KEY_WELCOME_SHOWN) != null) {
                    values.optionalBoolean(KEY_WELCOME_SHOWN, false)
                } else {
                    // Existing campaigns are not treated as newly created campaigns on upgrade.
                    true
                },
            )
            if (version < DEFEAT_STATE_SAVE_VERSION) decoded.migrateToBalanceV4() else decoded
        } catch (_: InvalidSaveValue) {
            return DecodeResult.Failure(SaveFailure.Corrupt("Save record is incomplete or unreadable"))
        }

        return when (val validation = GameStateValidator.validate(state)) {
            GameStateValidationResult.Valid -> DecodeResult.Success(state)
            is GameStateValidationResult.Invalid -> DecodeResult.Failure(
                SaveFailure.InvalidState(validation.reason),
            )
        }
    }

    private fun Properties.required(key: String): String =
        getProperty(key) ?: throw InvalidSaveValue()

    private fun Properties.requiredInt(key: String): Int =
        required(key).toIntOrNull() ?: throw InvalidSaveValue()

    private fun Properties.requiredLong(key: String): Long =
        required(key).toLongOrNull() ?: throw InvalidSaveValue()

    private fun Properties.requiredBoolean(key: String): Boolean = when (required(key)) {
        "true" -> true
        "false" -> false
        else -> throw InvalidSaveValue()
    }

    private fun Properties.optionalBoolean(key: String, default: Boolean): Boolean =
        when (val value = getProperty(key)) {
            null -> default
            "true" -> true
            "false" -> false
            else -> throw InvalidSaveValue()
        }

    private fun Properties.writeTrophyStats(stats: TrophyCampaignStats) {
        setProperty(KEY_TROPHY_FOREST_WON, stats.forestBattlesWon.toString())
        setProperty(KEY_TROPHY_FOREST_LOST, stats.forestBattlesLost.toString())
        setProperty(KEY_TROPHY_STREAK, stats.forestWinStreak.toString())
        setProperty(KEY_TROPHY_BEST_STREAK, stats.bestForestWinStreak.toString())
        setProperty(KEY_TROPHY_DAYS, stats.daysCompleted.toString())
        setProperty(KEY_TROPHY_CLEAN_DAYS, stats.consecutiveDaysWithoutDefeat.toString())
        setProperty(KEY_TROPHY_BEST_CLEAN_DAYS, stats.bestConsecutiveDaysWithoutDefeat.toString())
        setProperty(KEY_TROPHY_TRAINING, stats.trainingWins.toString())
        setProperty(KEY_TROPHY_OVERQUALIFIED, stats.overqualifiedAdvancement.toString())
        setProperty(KEY_TROPHY_MAX_GOLD, stats.maxGoldHeld.toString())
        setProperty(KEY_TROPHY_SPENT, stats.cumulativeGoldSpent.toString())
        setProperty(KEY_TROPHY_LARGEST_PURCHASE, stats.largestPurchase.toString())
        setProperty(KEY_TROPHY_WEAPONS, stats.weaponIdsOwned.joinToString(","))
        setProperty(KEY_TROPHY_ARMORS, stats.armorIdsOwned.joinToString(","))
        setProperty(KEY_TROPHY_ENCOUNTERED, stats.encounteredEnemyIds.joinToString(","))
        setProperty(KEY_TROPHY_DEFEATED, stats.defeatedEnemyIds.joinToString(","))
        setProperty(KEY_TROPHY_DEFEATED_BY, stats.enemyIdsThatDefeatedPlayer.joinToString(","))
        setProperty(KEY_TROPHY_LOW_HP, stats.lowHpForestVictories.toString())
        setProperty(KEY_TROPHY_DANGEROUS, stats.dangerousBattlesWon.toString())
        setProperty(KEY_TROPHY_FINAL_FIGHT, stats.finalFightVictories.toString())
        setProperty(KEY_TROPHY_DANGEROUS_STREAK, stats.dangerousSequenceWins.toString())
        setProperty(KEY_TROPHY_PERFECT_DAYS, stats.perfectDays.toString())
        setProperty(KEY_TROPHY_DEFEATS, stats.meaningfulDefeats.toString())
        setProperty(KEY_TROPHY_MAX_XP_DEFEAT, stats.maxExperienceBeforeDefeat.toString())
        setProperty(KEY_TROPHY_MAX_XP_AFTER, stats.maxExperienceAfterDefeat.toString())
        setProperty(KEY_TROPHY_GIANT_KILLS, stats.giantKills.toString())
        setProperty(KEY_TROPHY_REDEFEATED, stats.defeatedAfterBeingDefeatedIds.joinToString(","))
        setProperty(KEY_TROPHY_BEST_RANK, stats.bestRank.toString())
        setProperty(KEY_TROPHY_FIRST_STREAK, stats.firstPlaceStreak.toString())
        setProperty(KEY_TROPHY_BEST_FIRST_STREAK, stats.bestFirstPlaceStreak.toString())
        setProperty(KEY_TROPHY_START_RANK, stats.rankAtCampaignStart.toString())
        setProperty(KEY_TROPHY_CHALLENGE_WINS, stats.challengeWinNpcIds.joinToString(","))
        setProperty(KEY_TROPHY_CHALLENGE_LOSSES, stats.challengeLossNpcIds.joinToString(","))
        setProperty(KEY_TROPHY_NOTABLE, stats.notableCommunityEvents.toString())
        setProperty(KEY_TROPHY_RARE, stats.rareEncounterIds.joinToString(","))
        setProperty(KEY_TROPHY_DANGEROUS_OPTIONAL, stats.dangerousOptionalSuccess.toString())
        setProperty(KEY_TROPHY_RARE_PATH, stats.rarePathFound.toString())
        setProperty(KEY_TROPHY_DRAGON_WON, stats.dragonVictoryInCampaign.toString())
        setProperty(KEY_TROPHY_DRAGON_DAY, stats.dragonVictoryDay.toString())
        setProperty(KEY_TROPHY_DRAGON_LOW_HP, stats.lowHpDragonVictory.toString())
        setProperty(KEY_TROPHY_DRAGON_READY, stats.dragonReady.toString())
        setProperty(KEY_TROPHY_BEST_XP, stats.bestExperience.toString())
        setProperty(KEY_TROPHY_LAST_DEFEAT_XP, stats.lastDefeatHighWaterExperience.toString())
        setProperty(KEY_TROPHY_START_GOLD, stats.campaignStartGold.toString())
    }

    private fun Properties.trophyStats(): TrophyCampaignStats {
        fun ints(key: String) = getProperty(key)?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
        fun int(key: String) = requiredInt(key)
        return TrophyCampaignStats(
            forestBattlesWon = int(KEY_TROPHY_FOREST_WON), forestBattlesLost = int(KEY_TROPHY_FOREST_LOST),
            forestWinStreak = int(KEY_TROPHY_STREAK), bestForestWinStreak = int(KEY_TROPHY_BEST_STREAK),
            daysCompleted = int(KEY_TROPHY_DAYS), consecutiveDaysWithoutDefeat = int(KEY_TROPHY_CLEAN_DAYS),
            bestConsecutiveDaysWithoutDefeat = int(KEY_TROPHY_BEST_CLEAN_DAYS), trainingWins = int(KEY_TROPHY_TRAINING),
            overqualifiedAdvancement = requiredBoolean(KEY_TROPHY_OVERQUALIFIED), maxGoldHeld = int(KEY_TROPHY_MAX_GOLD),
            cumulativeGoldSpent = int(KEY_TROPHY_SPENT), largestPurchase = int(KEY_TROPHY_LARGEST_PURCHASE),
            weaponIdsOwned = ints(KEY_TROPHY_WEAPONS), armorIdsOwned = ints(KEY_TROPHY_ARMORS),
            encounteredEnemyIds = ints(KEY_TROPHY_ENCOUNTERED), defeatedEnemyIds = ints(KEY_TROPHY_DEFEATED),
            enemyIdsThatDefeatedPlayer = ints(KEY_TROPHY_DEFEATED_BY),
            lowHpForestVictories = int(KEY_TROPHY_LOW_HP), dangerousBattlesWon = int(KEY_TROPHY_DANGEROUS),
            finalFightVictories = int(KEY_TROPHY_FINAL_FIGHT), dangerousSequenceWins = int(KEY_TROPHY_DANGEROUS_STREAK),
            perfectDays = int(KEY_TROPHY_PERFECT_DAYS), meaningfulDefeats = int(KEY_TROPHY_DEFEATS),
            maxExperienceBeforeDefeat = int(KEY_TROPHY_MAX_XP_DEFEAT), maxExperienceAfterDefeat = int(KEY_TROPHY_MAX_XP_AFTER),
            giantKills = int(KEY_TROPHY_GIANT_KILLS), defeatedAfterBeingDefeatedIds = ints(KEY_TROPHY_REDEFEATED),
            bestRank = int(KEY_TROPHY_BEST_RANK), firstPlaceStreak = int(KEY_TROPHY_FIRST_STREAK),
            bestFirstPlaceStreak = int(KEY_TROPHY_BEST_FIRST_STREAK), rankAtCampaignStart = int(KEY_TROPHY_START_RANK),
            challengeWinNpcIds = ints(KEY_TROPHY_CHALLENGE_WINS), challengeLossNpcIds = ints(KEY_TROPHY_CHALLENGE_LOSSES),
            notableCommunityEvents = int(KEY_TROPHY_NOTABLE), rareEncounterIds = ints(KEY_TROPHY_RARE),
            dangerousOptionalSuccess = requiredBoolean(KEY_TROPHY_DANGEROUS_OPTIONAL), rarePathFound = requiredBoolean(KEY_TROPHY_RARE_PATH),
            dragonVictoryInCampaign = requiredBoolean(KEY_TROPHY_DRAGON_WON), dragonVictoryDay = int(KEY_TROPHY_DRAGON_DAY),
            lowHpDragonVictory = requiredBoolean(KEY_TROPHY_DRAGON_LOW_HP), dragonReady = requiredBoolean(KEY_TROPHY_DRAGON_READY),
            bestExperience = int(KEY_TROPHY_BEST_XP), lastDefeatHighWaterExperience = int(KEY_TROPHY_LAST_DEFEAT_XP),
            campaignStartGold = int(KEY_TROPHY_START_GOLD),
        )
    }

    private fun Properties.writeEvent(prefix: String, event: WorldEvent) {
        setProperty(prefix + "id", event.id)
        setProperty(prefix + "day", event.day.toString())
        setProperty(prefix + "type", event.type.name)
        setProperty(prefix + "characterId", event.characterId)
        setProperty(prefix + "relatedCharacterId", event.relatedCharacterId.orEmpty())
        setProperty(prefix + "value", event.value.toString())
        setProperty(prefix + "detail", event.detail.orEmpty())
        setProperty(prefix + "previousRank", event.previousRank.toString())
        setProperty(prefix + "newRank", event.newRank.toString())
        setProperty(prefix + "importance", event.importance.toString())
        setProperty(prefix + "visibility", event.visibility.name)
        setProperty(prefix + "location", event.location.name)
        setProperty(prefix + "beforeValue", event.beforeValue.toString())
        setProperty(prefix + "afterValue", event.afterValue.toString())
        setProperty(prefix + "relatedPriorEventId", event.relatedPriorEventId.orEmpty())
        setProperty(prefix + "relationshipDelta", event.relationshipDelta.toString())
        setProperty(prefix + "directlyAffectsPlayer", event.directlyAffectsPlayer.toString())
    }

    private fun Properties.worldEvent(prefix: String, version: Int): WorldEvent = WorldEvent(
        id = required(prefix + "id"),
        day = requiredInt(prefix + "day"),
        type = requiredEnum(prefix + "type"),
        characterId = required(prefix + "characterId"),
        relatedCharacterId = required(prefix + "relatedCharacterId").ifBlank { null },
        value = requiredInt(prefix + "value"),
        detail = required(prefix + "detail").ifBlank { null },
        previousRank = requiredInt(prefix + "previousRank"),
        newRank = requiredInt(prefix + "newRank"),
        importance = requiredInt(prefix + "importance"),
        visibility = if (version < CHRONICLE_SAVE_VERSION) {
            WorldEventVisibility.PUBLIC
        } else {
            requiredEnum(prefix + "visibility")
        },
        location = if (version < CHRONICLE_SAVE_VERSION) {
            WorldEventLocation.UNKNOWN
        } else {
            requiredEnum(prefix + "location")
        },
        beforeValue = if (version < CHRONICLE_SAVE_VERSION) 0 else requiredInt(prefix + "beforeValue"),
        afterValue = if (version < CHRONICLE_SAVE_VERSION) 0 else requiredInt(prefix + "afterValue"),
        relatedPriorEventId = if (version < CHRONICLE_SAVE_VERSION) null else {
            required(prefix + "relatedPriorEventId").ifBlank { null }
        },
        relationshipDelta = if (version < CHRONICLE_SAVE_VERSION) 0 else {
            requiredInt(prefix + "relationshipDelta")
        },
        directlyAffectsPlayer = if (version < CHRONICLE_SAVE_VERSION) false else {
            requiredBoolean(prefix + "directlyAffectsPlayer")
        },
    )

    private fun Properties.dayTransitionState(): DayTransitionState? {
        if (!requiredBoolean(KEY_TRANSITION_PRESENT)) return null
        return DayTransitionState(
            resolvedDay = requiredInt(KEY_TRANSITION_RESOLVED_DAY),
            nextDay = requiredInt(KEY_TRANSITION_NEXT_DAY),
            phase = requiredEnum(KEY_TRANSITION_PHASE),
        )
    }

    private fun Properties.dragonSeasonState(): DragonSeasonState {
        val declaredCount = requiredInt(KEY_SEASON_DECLARED_COUNT)
        val incidentCount = requiredInt(KEY_SEASON_INCIDENT_COUNT)
        val attemptCount = requiredInt(KEY_SEASON_ATTEMPT_COUNT)
        if (declaredCount !in 0..20 ||
            incidentCount !in 0..DragonSeasonBalance.MAX_INCIDENTS ||
            attemptCount !in 0..DragonSeasonBalance.MAX_ATTEMPTS
        ) {
            throw InvalidSaveValue()
        }
        return DragonSeasonState(
            stage = requiredEnum(KEY_SEASON_STAGE),
            pressure = requiredInt(KEY_SEASON_PRESSURE),
            stageStartedDay = requiredInt(KEY_SEASON_STAGE_STARTED_DAY),
            lastIncidentDay = requiredInt(KEY_SEASON_LAST_INCIDENT_DAY),
            incidents = (0 until incidentCount).map { index ->
                val prefix = KEY_SEASON_INCIDENT_PREFIX + index + "."
                DragonIncidentRecord(
                    id = required(prefix + "id"),
                    day = requiredInt(prefix + "day"),
                    type = requiredEnum(prefix + "type"),
                    stage = requiredEnum(prefix + "stage"),
                    visibility = requiredEnum(prefix + "visibility"),
                    residentId = required(prefix + "residentId").ifBlank { null },
                )
            },
            declaredChallengerIds = (0 until declaredCount).map { index ->
                required(KEY_SEASON_DECLARED_PREFIX + index)
            },
            attempts = (0 until attemptCount).map { index ->
                val prefix = KEY_SEASON_ATTEMPT_PREFIX + index + "."
                DragonAttemptRecord(
                    id = required(prefix + "id"),
                    day = requiredInt(prefix + "day"),
                    challengerId = required(prefix + "challengerId"),
                    outcome = requiredEnum(prefix + "outcome"),
                    challengerPower = requiredInt(prefix + "challengerPower"),
                    dragonPower = requiredInt(prefix + "dragonPower"),
                )
            },
            playerLastAttemptDay = requiredInt(KEY_SEASON_PLAYER_LAST_ATTEMPT_DAY),
            victorId = required(KEY_SEASON_VICTOR_ID).ifBlank { null },
            resolvedDay = requiredInt(KEY_SEASON_RESOLVED_DAY),
            conclusionPhase = required(KEY_SEASON_CONCLUSION_PHASE).ifBlank { null }?.let { name ->
                enumValues<SeasonConclusionPhase>().firstOrNull { it.name == name }
                    ?: throw InvalidSaveValue()
            },
        )
    }

    private fun Properties.dragonCombatState(): DragonCombatState? {
        if (!requiredBoolean(KEY_DRAGON_COMBAT_PRESENT)) return null
        val messageCount = requiredInt(KEY_DRAGON_COMBAT_MESSAGE_COUNT)
        if (messageCount !in 1..GameBalance.MAX_COMBAT_MESSAGES) throw InvalidSaveValue()
        return DragonCombatState(
            dragonCurrentHp = requiredInt(KEY_DRAGON_COMBAT_HP),
            status = requiredEnum(KEY_DRAGON_COMBAT_STATUS),
            recentMessages = (0 until messageCount).map { index ->
                CombatMessage(
                    text = required(KEY_DRAGON_COMBAT_MESSAGE_PREFIX + index + ".text"),
                    kind = requiredEnum(KEY_DRAGON_COMBAT_MESSAGE_PREFIX + index + ".kind"),
                )
            },
            turnNumber = requiredInt(KEY_DRAGON_COMBAT_TURN),
            crystalHideBroken = requiredBoolean(KEY_DRAGON_COMBAT_HIDE_BROKEN),
            attemptRecorded = requiredBoolean(KEY_DRAGON_COMBAT_ATTEMPT_RECORDED),
            goldLost = requiredInt(KEY_DRAGON_COMBAT_GOLD_LOST),
        )
    }

    private fun migratedSeason(currentDay: Int): DragonSeasonState {
        val pressure = ((currentDay - 1).toLong() * 5L)
            .coerceIn(0L, DragonSeasonBalance.CHALLENGE_WINDOW_PRESSURE.toLong())
            .toInt()
        val stage = when {
            pressure >= DragonSeasonBalance.CHALLENGE_WINDOW_PRESSURE -> DragonStage.CHALLENGE_WINDOW
            pressure >= DragonSeasonBalance.CRISIS_PRESSURE -> DragonStage.CRISIS
            pressure >= DragonSeasonBalance.FEAR_PRESSURE -> DragonStage.FEAR
            pressure >= DragonSeasonBalance.SIGNS_PRESSURE -> DragonStage.SIGNS
            else -> DragonStage.WHISPERS
        }
        return DragonSeasonState(
            stage = stage,
            pressure = pressure,
            stageStartedDay = currentDay,
        )
    }

    private fun Properties.combatState(): CombatState? {
        if (!requiredBoolean(KEY_COMBAT_PRESENT)) return null
        val messageCount = requiredInt(KEY_COMBAT_MESSAGE_COUNT)
        if (messageCount !in 1..GameBalance.MAX_COMBAT_MESSAGES) throw InvalidSaveValue()
        return CombatState(
            monsterId = required(KEY_COMBAT_MONSTER_ID),
            monsterCurrentHp = requiredInt(KEY_COMBAT_MONSTER_HP),
            status = requiredEnum<CombatStatus>(KEY_COMBAT_STATUS),
            fightConsumed = requiredBoolean(KEY_COMBAT_FIGHT_CONSUMED),
            recentMessages = (0 until messageCount).map { index ->
                CombatMessage(
                    text = required(KEY_COMBAT_MESSAGE_PREFIX + index + ".text"),
                    kind = requiredEnum(KEY_COMBAT_MESSAGE_PREFIX + index + ".kind"),
                )
            },
            escapeAvailable = requiredBoolean(KEY_COMBAT_ESCAPE_AVAILABLE),
            rewardsGranted = requiredBoolean(KEY_COMBAT_REWARDS_GRANTED),
            experienceReward = requiredInt(KEY_COMBAT_EXPERIENCE_REWARD),
            goldReward = requiredInt(KEY_COMBAT_GOLD_REWARD),
            goldLost = requiredInt(KEY_COMBAT_GOLD_LOST),
        )
    }

    private fun Properties.challengeState(): ChallengeState? {
        if (!requiredBoolean(KEY_CHALLENGE_PRESENT)) return null
        val messageCount = requiredInt(KEY_CHALLENGE_MESSAGE_COUNT)
        if (messageCount !in 1..GameBalance.MAX_COMBAT_MESSAGES) throw InvalidSaveValue()
        return ChallengeState(
            opponentId = required(KEY_CHALLENGE_OPPONENT_ID),
            opponentCurrentHp = requiredInt(KEY_CHALLENGE_OPPONENT_HP),
            status = requiredEnum(KEY_CHALLENGE_STATUS),
            recentMessages = (0 until messageCount).map { index ->
                CombatMessage(
                    text = required(KEY_CHALLENGE_MESSAGE_PREFIX + index + ".text"),
                    kind = requiredEnum(KEY_CHALLENGE_MESSAGE_PREFIX + index + ".kind"),
                )
            },
            opportunityConsumed = requiredBoolean(KEY_CHALLENGE_CONSUMED),
            rewardsGranted = requiredBoolean(KEY_CHALLENGE_REWARDS_GRANTED),
            experienceReward = requiredInt(KEY_CHALLENGE_EXPERIENCE_REWARD),
            goldTransferred = requiredInt(KEY_CHALLENGE_GOLD_TRANSFERRED),
        )
    }

    private fun Properties.communityState(version: Int): CommunityState {
        val npcCount = requiredInt(KEY_COMMUNITY_COUNT)
        if (npcCount !in 1..20) throw InvalidSaveValue()
        val eventCount = requiredInt(KEY_EVENT_COUNT)
        if (eventCount !in 0..com.legendoftheglassdragon.game.CommunityRules.MAX_RETAINED_EVENTS) {
            throw InvalidSaveValue()
        }
        return CommunityState(
            adventurers = (0 until npcCount).map { index ->
                val prefix = KEY_NPC_PREFIX + index + "."
                val weaponId = if (version < ECONOMY_SAVE_VERSION) {
                    EquipmentCatalog.weaponIdForLegacyValue(required(prefix + "weapon"))
                        ?: throw InvalidSaveValue()
                } else {
                    required(prefix + "weaponId")
                }
                val armorId = if (version < ECONOMY_SAVE_VERSION) {
                    EquipmentCatalog.armorIdForLegacyValue(required(prefix + "armor"))
                        ?: throw InvalidSaveValue()
                } else {
                    required(prefix + "armorId")
                }
                NpcAdventurer(
                    id = required(prefix + "id"),
                    name = required(prefix + "name"),
                    level = requiredInt(prefix + "level"),
                    experience = requiredInt(prefix + "experience"),
                    maximumHp = requiredInt(prefix + "maximumHp"),
                    baseAttack = if (version < ECONOMY_SAVE_VERSION) {
                        requiredInt(prefix + "attack") -
                            EquipmentCatalog.requireWeapon(weaponId).attackContribution
                    } else {
                        requiredInt(prefix + "baseAttack")
                    },
                    baseDefense = if (version < ECONOMY_SAVE_VERSION) {
                        requiredInt(prefix + "defense") -
                            EquipmentCatalog.requireArmor(armorId).defenseContribution
                    } else {
                        requiredInt(prefix + "baseDefense")
                    },
                    gold = requiredInt(prefix + "gold"),
                    weaponId = weaponId,
                    armorId = armorId,
                    status = requiredEnum(prefix + "status"),
                    recoveryDaysRemaining = requiredInt(prefix + "recoveryDays"),
                    personality = requiredEnum(prefix + "personality"),
                    familiarity = requiredInt(prefix + "familiarity"),
                    lastMetDay = requiredInt(prefix + "lastMetDay"),
                    lastAdvancedDay = requiredInt(prefix + "lastAdvancedDay"),
                    rivalry = if (version < ECONOMY_SAVE_VERSION) 0 else requiredInt(prefix + "rivalry"),
                    playerWinsAgainst = if (version < ECONOMY_SAVE_VERSION) 0 else {
                        requiredInt(prefix + "playerWinsAgainst")
                    },
                    playerLossesAgainst = if (version < ECONOMY_SAVE_VERSION) 0 else {
                        requiredInt(prefix + "playerLossesAgainst")
                    },
                    challengeWins = if (version < ECONOMY_SAVE_VERSION) 0 else {
                        requiredInt(prefix + "challengeWins")
                    },
                    challengeLosses = if (version < ECONOMY_SAVE_VERSION) 0 else {
                        requiredInt(prefix + "challengeLosses")
                    },
                    lastChallengeDay = if (version < ECONOMY_SAVE_VERSION) 0 else {
                        requiredInt(prefix + "lastChallengeDay")
                    },
                )
            },
            recentEvents = (0 until eventCount).map { index ->
                worldEvent(KEY_EVENT_PREFIX + index + ".", version)
            },
            chronicleHistory = if (version < CHRONICLE_SAVE_VERSION) {
                emptyList()
            } else {
                val recordCount = requiredInt(KEY_CHRONICLE_COUNT)
                if (recordCount !in 0..ChronicleRules.MAX_HISTORY_DAYS) throw InvalidSaveValue()
                (0 until recordCount).map { recordIndex ->
                    val prefix = KEY_CHRONICLE_PREFIX + recordIndex + "."
                    val recordEventCount = requiredInt(prefix + "events.count")
                    if (recordEventCount !in 0..ChronicleRules.MAX_ITEMS_PER_DAY) {
                        throw InvalidSaveValue()
                    }
                    ChronicleRecord(
                        day = requiredInt(prefix + "day"),
                        events = (0 until recordEventCount).map { eventIndex ->
                            worldEvent(prefix + "events." + eventIndex + ".", version)
                        },
                    )
                }
            },
            lastSimulatedDay = requiredInt(KEY_COMMUNITY_LAST_DAY),
        )
    }

    private inline fun <reified T : Enum<T>> Properties.requiredEnum(key: String): T =
        enumValues<T>().firstOrNull { it.name == required(key) } ?: throw InvalidSaveValue()

    /**
     * Schema v4 lengthened progression and increased promotion attack growth. Preserve every
     * earned level from development saves by raising only values that would otherwise fall below
     * the new lawful minimum; names, days, wealth, equipment, wounds, and community identity stay.
     */
    private fun GameState.migrateToBalanceV4(): GameState {
        fun playerForNewBalance(player: PlayerCharacter): PlayerCharacter {
            val promotions = (player.level - 1).coerceAtLeast(0)
            val minimumMaximumHp = safeBalanceValue(
                StartingCharacterRules.MAXIMUM_HP,
                GameBalance.LEVEL_MAX_HP_GAIN,
                promotions,
            )
            val newMaximumHp = maxOf(player.maximumHp, minimumMaximumHp)
            val addedMaximumHp = newMaximumHp - player.maximumHp
            return player.copy(
                experience = maxOf(
                    player.experience,
                    ProgressionRules.experienceRequiredForLevel(player.level),
                ),
                currentHp = (player.currentHp.toLong() + addedMaximumHp)
                    .coerceAtMost(newMaximumHp.toLong())
                    .toInt(),
                maximumHp = newMaximumHp,
                baseAttack = maxOf(
                    player.baseAttack,
                    safeBalanceValue(
                        StartingCharacterRules.ATTACK,
                        GameBalance.LEVEL_ATTACK_GAIN,
                        promotions,
                    ),
                ),
                baseDefense = maxOf(
                    player.baseDefense,
                    safeBalanceValue(
                        StartingCharacterRules.DEFENSE,
                        GameBalance.LEVEL_DEFENSE_GAIN,
                        promotions,
                    ),
                ),
            )
        }

        fun npcForNewBalance(npc: NpcAdventurer): NpcAdventurer {
            val promotions = (npc.level - 1).coerceAtLeast(0)
            return npc.copy(
                experience = maxOf(
                    npc.experience,
                    ProgressionRules.experienceRequiredForLevel(npc.level),
                ),
                maximumHp = maxOf(
                    npc.maximumHp,
                    safeBalanceValue(
                        StartingCharacterRules.MAXIMUM_HP - 1,
                        GameBalance.LEVEL_MAX_HP_GAIN,
                        promotions,
                    ),
                ),
                baseAttack = maxOf(
                    npc.baseAttack,
                    safeBalanceValue(
                        StartingCharacterRules.ATTACK,
                        GameBalance.LEVEL_ATTACK_GAIN,
                        promotions,
                    ),
                ),
                baseDefense = maxOf(
                    npc.baseDefense,
                    safeBalanceValue(
                        StartingCharacterRules.DEFENSE,
                        GameBalance.LEVEL_DEFENSE_GAIN,
                        promotions,
                    ),
                ),
            )
        }

        return copy(
            player = playerForNewBalance(player),
            community = community.copy(
                adventurers = community.adventurers.map(::npcForNewBalance),
            ),
        )
    }

    private fun safeBalanceValue(start: Int, gain: Int, promotions: Int): Int =
        (start.toLong() + gain.toLong() * promotions)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

    private class InvalidSaveValue : RuntimeException()

    private companion object {
        const val LEGACY_SAVE_VERSION = 1
        const val COMMUNITY_SAVE_VERSION = 3
        const val DEFEAT_STATE_SAVE_VERSION = 4
        const val ECONOMY_SAVE_VERSION = 5
        const val CHRONICLE_SAVE_VERSION = 6
        const val DRAGON_SEASON_SAVE_VERSION = 7
        const val TRAINING_SAVE_VERSION = 8
        const val TROPHY_STATS_SAVE_VERSION = 8
        const val KEY_VERSION = "save.version"
        const val KEY_REVISION = "game.revision"
        const val KEY_CURRENT_DAY = "game.currentDay"
        const val KEY_WORLD_SEED = "world.seed"
        const val KEY_WORLD_RANDOM_STEP = "world.randomStep"
        const val KEY_PLAYER_ID = "player.id"
        const val KEY_PLAYER_NAME = "player.name"
        const val KEY_PLAYER_LEVEL = "player.level"
        const val KEY_PLAYER_EXPERIENCE = "player.experience"
        const val KEY_PLAYER_CURRENT_HP = "player.currentHp"
        const val KEY_PLAYER_MAXIMUM_HP = "player.maximumHp"
        const val KEY_PLAYER_ATTACK = "player.attack"
        const val KEY_PLAYER_DEFENSE = "player.defense"
        const val KEY_PLAYER_BASE_ATTACK = "player.baseAttack"
        const val KEY_PLAYER_BASE_DEFENSE = "player.baseDefense"
        const val KEY_PLAYER_GOLD = "player.gold"
        const val KEY_PLAYER_BANKED_GOLD = "player.bankedGold"
        const val KEY_PLAYER_WEAPON = "player.weapon"
        const val KEY_PLAYER_ARMOR = "player.armor"
        const val KEY_PLAYER_WEAPON_ID = "player.weaponId"
        const val KEY_PLAYER_ARMOR_ID = "player.armorId"
        const val KEY_PLAYER_CHALLENGE_WINS = "player.challengeWins"
        const val KEY_PLAYER_CHALLENGE_LOSSES = "player.challengeLosses"
        const val KEY_DAY_CURRENT = "day.currentDay"
        const val KEY_DAY_FIGHTS = "day.fightsRemaining"
        const val KEY_DAY_ENDED = "day.ended"
        const val KEY_DAY_DEFEATED = "day.wasDefeated"
        const val KEY_DAY_CHALLENGES = "day.challengesRemaining"
        const val KEY_DAY_TRAINING_ATTEMPTED = "day.trainingAttempted"
        const val KEY_DAY_TRAINING_SUCCEEDED = "day.trainingSucceeded"
        const val KEY_WELCOME_SHOWN = "playerLayer.welcomeShown"
        const val KEY_TROPHY_FOREST_WON = "trophyStats.forestWon"
        const val KEY_TROPHY_FOREST_LOST = "trophyStats.forestLost"
        const val KEY_TROPHY_STREAK = "trophyStats.streak"
        const val KEY_TROPHY_BEST_STREAK = "trophyStats.bestStreak"
        const val KEY_TROPHY_DAYS = "trophyStats.days"
        const val KEY_TROPHY_CLEAN_DAYS = "trophyStats.cleanDays"
        const val KEY_TROPHY_BEST_CLEAN_DAYS = "trophyStats.bestCleanDays"
        const val KEY_TROPHY_TRAINING = "trophyStats.training"
        const val KEY_TROPHY_OVERQUALIFIED = "trophyStats.overqualified"
        const val KEY_TROPHY_MAX_GOLD = "trophyStats.maxGold"
        const val KEY_TROPHY_SPENT = "trophyStats.spent"
        const val KEY_TROPHY_LARGEST_PURCHASE = "trophyStats.largestPurchase"
        const val KEY_TROPHY_WEAPONS = "trophyStats.weapons"
        const val KEY_TROPHY_ARMORS = "trophyStats.armors"
        const val KEY_TROPHY_ENCOUNTERED = "trophyStats.encountered"
        const val KEY_TROPHY_DEFEATED = "trophyStats.defeated"
        const val KEY_TROPHY_DEFEATED_BY = "trophyStats.defeatedBy"
        const val KEY_TROPHY_LOW_HP = "trophyStats.lowHp"
        const val KEY_TROPHY_DANGEROUS = "trophyStats.dangerous"
        const val KEY_TROPHY_FINAL_FIGHT = "trophyStats.finalFight"
        const val KEY_TROPHY_DANGEROUS_STREAK = "trophyStats.dangerousStreak"
        const val KEY_TROPHY_PERFECT_DAYS = "trophyStats.perfectDays"
        const val KEY_TROPHY_DEFEATS = "trophyStats.defeats"
        const val KEY_TROPHY_MAX_XP_DEFEAT = "trophyStats.maxXpDefeat"
        const val KEY_TROPHY_MAX_XP_AFTER = "trophyStats.maxXpAfter"
        const val KEY_TROPHY_GIANT_KILLS = "trophyStats.giantKills"
        const val KEY_TROPHY_REDEFEATED = "trophyStats.redefeated"
        const val KEY_TROPHY_BEST_RANK = "trophyStats.bestRank"
        const val KEY_TROPHY_FIRST_STREAK = "trophyStats.firstStreak"
        const val KEY_TROPHY_BEST_FIRST_STREAK = "trophyStats.bestFirstStreak"
        const val KEY_TROPHY_START_RANK = "trophyStats.startRank"
        const val KEY_TROPHY_CHALLENGE_WINS = "trophyStats.challengeWins"
        const val KEY_TROPHY_CHALLENGE_LOSSES = "trophyStats.challengeLosses"
        const val KEY_TROPHY_NOTABLE = "trophyStats.notable"
        const val KEY_TROPHY_RARE = "trophyStats.rare"
        const val KEY_TROPHY_DANGEROUS_OPTIONAL = "trophyStats.dangerousOptional"
        const val KEY_TROPHY_RARE_PATH = "trophyStats.rarePath"
        const val KEY_TROPHY_DRAGON_WON = "trophyStats.dragonWon"
        const val KEY_TROPHY_DRAGON_DAY = "trophyStats.dragonDay"
        const val KEY_TROPHY_DRAGON_LOW_HP = "trophyStats.dragonLowHp"
        const val KEY_TROPHY_DRAGON_READY = "trophyStats.dragonReady"
        const val KEY_TROPHY_BEST_XP = "trophyStats.bestXp"
        const val KEY_TROPHY_LAST_DEFEAT_XP = "trophyStats.lastDefeatXp"
        const val KEY_TROPHY_START_GOLD = "trophyStats.startGold"
        const val KEY_COMMUNITY_COUNT = "community.npcs.count"
        const val KEY_COMMUNITY_LAST_DAY = "community.lastSimulatedDay"
        const val KEY_COMMUNITY_MORNING_PENDING = "community.morningReportPending"
        const val KEY_NPC_PREFIX = "community.npcs."
        const val KEY_EVENT_COUNT = "community.events.count"
        const val KEY_EVENT_PREFIX = "community.events."
        const val KEY_CHRONICLE_COUNT = "community.chronicle.count"
        const val KEY_CHRONICLE_PREFIX = "community.chronicle."
        const val KEY_TRANSITION_PRESENT = "transition.present"
        const val KEY_TRANSITION_RESOLVED_DAY = "transition.resolvedDay"
        const val KEY_TRANSITION_NEXT_DAY = "transition.nextDay"
        const val KEY_TRANSITION_PHASE = "transition.phase"
        const val KEY_SEASON_STAGE = "season.stage"
        const val KEY_SEASON_PRESSURE = "season.pressure"
        const val KEY_SEASON_STAGE_STARTED_DAY = "season.stageStartedDay"
        const val KEY_SEASON_LAST_INCIDENT_DAY = "season.lastIncidentDay"
        const val KEY_SEASON_PLAYER_LAST_ATTEMPT_DAY = "season.playerLastAttemptDay"
        const val KEY_SEASON_VICTOR_ID = "season.victorId"
        const val KEY_SEASON_RESOLVED_DAY = "season.resolvedDay"
        const val KEY_SEASON_CONCLUSION_PHASE = "season.conclusionPhase"
        const val KEY_SEASON_DECLARED_COUNT = "season.declared.count"
        const val KEY_SEASON_DECLARED_PREFIX = "season.declared."
        const val KEY_SEASON_INCIDENT_COUNT = "season.incidents.count"
        const val KEY_SEASON_INCIDENT_PREFIX = "season.incidents."
        const val KEY_SEASON_ATTEMPT_COUNT = "season.attempts.count"
        const val KEY_SEASON_ATTEMPT_PREFIX = "season.attempts."
        const val KEY_COMBAT_PRESENT = "combat.present"
        const val KEY_COMBAT_MONSTER_ID = "combat.monster.id"
        const val KEY_COMBAT_MONSTER_HP = "combat.monster.currentHp"
        const val KEY_COMBAT_STATUS = "combat.status"
        const val KEY_COMBAT_FIGHT_CONSUMED = "combat.fightConsumed"
        const val KEY_COMBAT_ESCAPE_AVAILABLE = "combat.escapeAvailable"
        const val KEY_COMBAT_REWARDS_GRANTED = "combat.rewardsGranted"
        const val KEY_COMBAT_EXPERIENCE_REWARD = "combat.reward.experience"
        const val KEY_COMBAT_GOLD_REWARD = "combat.reward.gold"
        const val KEY_COMBAT_GOLD_LOST = "combat.goldLost"
        const val KEY_COMBAT_MESSAGE_COUNT = "combat.messages.count"
        const val KEY_COMBAT_MESSAGE_PREFIX = "combat.messages."
        const val KEY_CHALLENGE_PRESENT = "challenge.present"
        const val KEY_CHALLENGE_OPPONENT_ID = "challenge.opponent.id"
        const val KEY_CHALLENGE_OPPONENT_HP = "challenge.opponent.currentHp"
        const val KEY_CHALLENGE_STATUS = "challenge.status"
        const val KEY_CHALLENGE_CONSUMED = "challenge.opportunityConsumed"
        const val KEY_CHALLENGE_REWARDS_GRANTED = "challenge.rewardsGranted"
        const val KEY_CHALLENGE_EXPERIENCE_REWARD = "challenge.reward.experience"
        const val KEY_CHALLENGE_GOLD_TRANSFERRED = "challenge.goldTransferred"
        const val KEY_CHALLENGE_MESSAGE_COUNT = "challenge.messages.count"
        const val KEY_CHALLENGE_MESSAGE_PREFIX = "challenge.messages."
        const val KEY_DRAGON_COMBAT_PRESENT = "dragonCombat.present"
        const val KEY_DRAGON_COMBAT_HP = "dragonCombat.currentHp"
        const val KEY_DRAGON_COMBAT_STATUS = "dragonCombat.status"
        const val KEY_DRAGON_COMBAT_TURN = "dragonCombat.turn"
        const val KEY_DRAGON_COMBAT_HIDE_BROKEN = "dragonCombat.crystalHideBroken"
        const val KEY_DRAGON_COMBAT_ATTEMPT_RECORDED = "dragonCombat.attemptRecorded"
        const val KEY_DRAGON_COMBAT_GOLD_LOST = "dragonCombat.goldLost"
        const val KEY_DRAGON_COMBAT_MESSAGE_COUNT = "dragonCombat.messages.count"
        const val KEY_DRAGON_COMBAT_MESSAGE_PREFIX = "dragonCombat.messages."
    }
}
