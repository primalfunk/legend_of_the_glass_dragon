package com.legendoftheglassdragon.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EconomyEngineTest {
    @Test
    fun canonicalCatalogHasUniqueLawfulIncreasingTiers() {
        assertEquals(EquipmentCatalog.weapons.size, EquipmentCatalog.weapons.map { it.id }.distinct().size)
        assertEquals(EquipmentCatalog.armors.size, EquipmentCatalog.armors.map { it.id }.distinct().size)
        assertTrue(EquipmentCatalog.weapons.zipWithNext().all { (a, b) ->
            b.tier > a.tier && b.attackContribution > a.attackContribution &&
                b.purchasePrice > a.purchasePrice
        })
        assertTrue(EquipmentCatalog.armors.zipWithNext().all { (a, b) ->
            b.tier > a.tier && b.defenseContribution > a.defenseContribution &&
                b.purchasePrice > a.purchasePrice
        })
        assertTrue((EquipmentCatalog.weapons + emptyList()).all {
            it.purchasePrice >= 0 && it.resaleValue in 0..it.purchasePrice && it.minimumLevel >= 1
        })
        assertTrue(EquipmentCatalog.armors.all {
            it.purchasePrice >= 0 && it.resaleValue in 0..it.purchasePrice && it.minimumLevel >= 1
        })
    }

    @Test
    fun weaponPurchaseUsesTradeInChangesDerivedAttackAndCannotApplyTwice() {
        val original = newGame().copy(player = newGame().player.copy(gold = 100))
        val first = EconomyEngine.buyWeapon(original, original.revision, "iron_sword").applied()

        assertEquals(65, first.player.gold)
        assertEquals("iron_sword", first.player.weaponId)
        assertEquals(original.player.baseAttack, first.player.baseAttack)
        assertEquals(original.player.attack + 1, first.player.attack)

        assertEquals(
            GameActionResult.Rejected(GameActionRejection.StaleState),
            EconomyEngine.buyWeapon(first, original.revision, "iron_sword"),
        )
        val upgraded = EconomyEngine.buyWeapon(first, first.revision, "steel_blade").applied()
        assertEquals(0, upgraded.player.gold)
        assertEquals("steel_blade", upgraded.player.weaponId)
        assertEquals(original.player.attack + 3, upgraded.player.attack)
    }

    @Test
    fun armorPurchaseHonorsAffordabilityLevelAndOneWayProgression() {
        val poor = newGame()
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.NotEnoughCarriedGold(35)),
            EconomyEngine.buyArmor(poor, poor.revision, "studded_leather"),
        )
        val funded = poor.copy(player = poor.player.copy(gold = 200))
        val bought = EconomyEngine.buyArmor(funded, funded.revision, "studded_leather").applied()
        assertEquals(funded.player.defense + 1, bought.player.defense)
        assertEquals(165, bought.player.gold)
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.EquipmentNotUpgrade),
            EconomyEngine.buyArmor(bought, bought.revision, EquipmentCatalog.STARTER_ARMOR_ID),
        )
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.EquipmentLevelRequired(2)),
            EconomyEngine.buyArmor(bought, bought.revision, "plate_armor"),
        )
    }

    @Test
    fun depositsWithdrawalsAndAllAmountsConserveGoldExactlyOnce() {
        val original = newGame().copy(player = newGame().player.copy(gold = 125, bankedGold = 40))
        val deposited = EconomyEngine.deposit(original, original.revision, 100).applied()
        assertEquals(25, deposited.player.gold)
        assertEquals(140, deposited.player.bankedGold)
        assertEquals(165, deposited.player.gold + deposited.player.bankedGold)

        assertEquals(
            GameActionResult.Rejected(GameActionRejection.StaleState),
            EconomyEngine.deposit(deposited, original.revision, 100),
        )
        val withdrawn = EconomyEngine.withdraw(
            deposited,
            deposited.revision,
            deposited.player.bankedGold,
        ).applied()
        assertEquals(165, withdrawn.player.gold)
        assertEquals(0, withdrawn.player.bankedGold)
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.NotEnoughBankedGold(1)),
            EconomyEngine.withdraw(withdrawn, withdrawn.revision, 1),
        )
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.InvalidGoldAmount),
            EconomyEngine.deposit(withdrawn, withdrawn.revision, 0),
        )
    }

    @Test
    fun bankRejectsIntegerOverflowWithoutChangingEitherBalance() {
        val depositOverflow = newGame().let { state ->
            state.copy(player = state.player.copy(gold = 10, bankedGold = Int.MAX_VALUE - 5))
        }
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.InvalidGoldAmount),
            EconomyEngine.deposit(depositOverflow, depositOverflow.revision, 10),
        )
        val withdrawalOverflow = newGame().let { state ->
            state.copy(player = state.player.copy(gold = Int.MAX_VALUE - 5, bankedGold = 10))
        }
        assertEquals(
            GameActionResult.Rejected(GameActionRejection.InvalidGoldAmount),
            EconomyEngine.withdraw(withdrawalOverflow, withdrawalOverflow.revision, 10),
        )
    }

    @Test
    fun ordinaryForestDefeatRisksOnlyCarriedGold() {
        val original = newGame().let { state ->
            state.copy(player = state.player.copy(currentHp = 1, gold = 100, bankedGold = 500))
        }
        val zeroRandom = GameRandom { world, _ ->
            RandomRoll(0, world.copy(randomStep = world.randomStep + 1))
        }
        val engine = GameEngine(zeroRandom)
        val encounter = engine.startForestEncounter(original, original.revision).applied()
        val defeated = engine.attack(encounter, encounter.revision).applied()

        assertEquals(75, defeated.player.gold)
        assertEquals(500, defeated.player.bankedGold)
        assertEquals(0, defeated.playerDay.fightsRemaining)
        assertTrue(defeated.playerDay.wasDefeatedToday)
        assertEquals(GameStateValidationResult.Valid, GameStateValidator.validate(defeated))
    }

    private fun newGame(): GameState = (
        NewGameFactory(
            idProvider = { "economy-player" },
            seedProvider = { 42L },
        ).create("Arcturus") as NewGameCreationResult.Created
    ).gameState

    private fun GameActionResult.applied(): GameState =
        (this as GameActionResult.Applied).gameState
}
