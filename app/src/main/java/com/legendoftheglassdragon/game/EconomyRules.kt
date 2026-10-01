package com.legendoftheglassdragon.game

object EconomyRules {
    val BANK_AMOUNTS = listOf(25, 50, 100)

    fun weaponTradeInValue(player: PlayerCharacter): Int =
        EquipmentCatalog.requireWeapon(player.weaponId).resaleValue

    fun armorTradeInValue(player: PlayerCharacter): Int =
        EquipmentCatalog.requireArmor(player.armorId).resaleValue

    fun weaponUpgradeCost(player: PlayerCharacter, weapon: WeaponDefinition): Int =
        (weapon.purchasePrice - weaponTradeInValue(player)).coerceAtLeast(0)

    fun armorUpgradeCost(player: PlayerCharacter, armor: ArmorDefinition): Int =
        (armor.purchasePrice - armorTradeInValue(player)).coerceAtLeast(0)
}

object EconomyEngine {
    fun buyWeapon(
        state: GameState,
        expectedRevision: Long,
        weaponId: String,
    ): GameActionResult {
        commonRejection(state, expectedRevision)?.let { return it }
        val requested = EquipmentCatalog.weapon(weaponId)
            ?: return GameActionResult.Rejected(GameActionRejection.UnknownEquipment)
        val current = EquipmentCatalog.requireWeapon(state.player.weaponId)
        if (requested.tier <= current.tier) {
            return GameActionResult.Rejected(GameActionRejection.EquipmentNotUpgrade)
        }
        if (state.player.level < requested.minimumLevel) {
            return GameActionResult.Rejected(
                GameActionRejection.EquipmentLevelRequired(requested.minimumLevel),
            )
        }
        val cost = EconomyRules.weaponUpgradeCost(state.player, requested)
        if (state.player.gold < cost) {
            return GameActionResult.Rejected(GameActionRejection.NotEnoughCarriedGold(cost))
        }
        val purchased = state.copy(
                player = state.player.copy(
                    gold = state.player.gold - cost,
                    weaponId = requested.id,
                ),
            )
        return applied(
            WorldEventLedger.append(
                purchased,
                equipmentEvent(
                    state = state,
                    itemName = requested.displayName,
                    beforeTier = current.tier,
                    afterTier = requested.tier,
                ),
            ),
        )
    }

    fun buyArmor(
        state: GameState,
        expectedRevision: Long,
        armorId: String,
    ): GameActionResult {
        commonRejection(state, expectedRevision)?.let { return it }
        val requested = EquipmentCatalog.armor(armorId)
            ?: return GameActionResult.Rejected(GameActionRejection.UnknownEquipment)
        val current = EquipmentCatalog.requireArmor(state.player.armorId)
        if (requested.tier <= current.tier) {
            return GameActionResult.Rejected(GameActionRejection.EquipmentNotUpgrade)
        }
        if (state.player.level < requested.minimumLevel) {
            return GameActionResult.Rejected(
                GameActionRejection.EquipmentLevelRequired(requested.minimumLevel),
            )
        }
        val cost = EconomyRules.armorUpgradeCost(state.player, requested)
        if (state.player.gold < cost) {
            return GameActionResult.Rejected(GameActionRejection.NotEnoughCarriedGold(cost))
        }
        val purchased = state.copy(
                player = state.player.copy(
                    gold = state.player.gold - cost,
                    armorId = requested.id,
                ),
            )
        return applied(
            WorldEventLedger.append(
                purchased,
                equipmentEvent(
                    state = state,
                    itemName = requested.displayName,
                    beforeTier = current.tier,
                    afterTier = requested.tier,
                ),
            ),
        )
    }

    fun deposit(
        state: GameState,
        expectedRevision: Long,
        amount: Int,
    ): GameActionResult {
        commonRejection(state, expectedRevision)?.let { return it }
        if (amount <= 0) return GameActionResult.Rejected(GameActionRejection.InvalidGoldAmount)
        if (state.player.gold < amount) {
            return GameActionResult.Rejected(GameActionRejection.NotEnoughCarriedGold(amount))
        }
        if (amount > Int.MAX_VALUE - state.player.bankedGold) {
            return GameActionResult.Rejected(GameActionRejection.InvalidGoldAmount)
        }
        return applied(
            state.copy(
                player = state.player.copy(
                    gold = state.player.gold - amount,
                    bankedGold = safeAdd(state.player.bankedGold, amount),
                ),
            ),
        )
    }

    fun withdraw(
        state: GameState,
        expectedRevision: Long,
        amount: Int,
    ): GameActionResult {
        commonRejection(state, expectedRevision)?.let { return it }
        if (amount <= 0) return GameActionResult.Rejected(GameActionRejection.InvalidGoldAmount)
        if (state.player.bankedGold < amount) {
            return GameActionResult.Rejected(GameActionRejection.NotEnoughBankedGold(amount))
        }
        if (amount > Int.MAX_VALUE - state.player.gold) {
            return GameActionResult.Rejected(GameActionRejection.InvalidGoldAmount)
        }
        return applied(
            state.copy(
                player = state.player.copy(
                    gold = safeAdd(state.player.gold, amount),
                    bankedGold = state.player.bankedGold - amount,
                ),
            ),
        )
    }

    private fun commonRejection(
        state: GameState,
        expectedRevision: Long,
    ): GameActionResult.Rejected? = when {
        state.revision != expectedRevision ->
            GameActionResult.Rejected(GameActionRejection.StaleState)
        state.activeCombat != null ->
            GameActionResult.Rejected(GameActionRejection.ActiveCombat)
        state.activeChallenge != null ->
            GameActionResult.Rejected(GameActionRejection.ActiveChallenge)
        state.activeDragonCombat != null ->
            GameActionResult.Rejected(GameActionRejection.ActiveDragonCombat)
        else -> null
    }

    private fun applied(state: GameState): GameActionResult.Applied =
        GameActionResult.Applied(state.copy(revision = state.revision + 1))

    private fun equipmentEvent(
        state: GameState,
        itemName: String,
        beforeTier: Int,
        afterTier: Int,
    ): WorldEvent = WorldEvent(
        id = "day-" + state.currentDay + "-player-equipment-" + state.revision,
        day = state.currentDay,
        type = WorldEventType.EQUIPMENT,
        characterId = state.player.id,
        value = afterTier,
        detail = itemName,
        importance = if (afterTier >= 4) 3 else 2,
        visibility = WorldEventVisibility.PUBLIC,
        location = WorldEventLocation.MARKET,
        beforeValue = beforeTier,
        afterValue = afterTier,
        directlyAffectsPlayer = true,
    )

    private fun safeAdd(value: Int, amount: Int): Int =
        (value.toLong() + amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
