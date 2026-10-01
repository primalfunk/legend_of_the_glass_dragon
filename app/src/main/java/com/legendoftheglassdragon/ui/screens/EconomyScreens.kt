package com.legendoftheglassdragon.ui.screens

import androidx.compose.runtime.Composable
import com.legendoftheglassdragon.game.ArmorDefinition
import com.legendoftheglassdragon.game.EconomyRules
import com.legendoftheglassdragon.game.EquipmentCatalog
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.WeaponDefinition
import com.legendoftheglassdragon.ui.components.TerminalGap
import com.legendoftheglassdragon.ui.components.TerminalMenuRow
import com.legendoftheglassdragon.ui.components.TerminalMessageSlot
import com.legendoftheglassdragon.ui.components.TerminalSurface
import com.legendoftheglassdragon.ui.components.TerminalText
import com.legendoftheglassdragon.ui.theme.AnsiColor

@Composable
fun WeaponShopScreen(
    gameState: GameState,
    message: String?,
    onBuy: (String, Long) -> Unit,
    onReturnToTown: () -> Unit,
) {
    val player = gameState.player
    val current = EquipmentCatalog.requireWeapon(player.weaponId)
    TerminalSurface {
        ScreenHeader("YE OLDE WEAPON SHOPPE", color = AnsiColor.BrightRed)
        TerminalGap()
        TerminalText("You carry: " + player.gold + " gold", color = AnsiColor.Yellow)
        TerminalText("Your weapon: " + current.displayName, color = AnsiColor.BrightGreen)
        TerminalText(
            "Attack: " + player.attack + " (" + player.baseAttack + " + " + current.attackContribution + ")",
            color = AnsiColor.Cyan,
        )
        TerminalText("Trade-in value: " + current.resaleValue + " gold", color = AnsiColor.DimGray)
        TerminalMessageSlot(message)
        TerminalGap(lines = 2)
        EquipmentCatalog.weapons.forEachIndexed { index, weapon ->
            WeaponRow(index + 1, player.level, player.gold, current, weapon, gameState.revision, onBuy)
        }
        TerminalGap()
        TerminalMenuRow(number = 0, label = "Return to Town", onClick = onReturnToTown)
    }
}

@Composable
private fun WeaponRow(
    number: Int,
    level: Int,
    gold: Int,
    current: WeaponDefinition,
    weapon: WeaponDefinition,
    revision: Long,
    onBuy: (String, Long) -> Unit,
) {
    val cost = (weapon.purchasePrice - current.resaleValue).coerceAtLeast(0)
    val upgrade = weapon.tier > current.tier
    val enabled = upgrade && level >= weapon.minimumLevel && gold >= cost
    TerminalMenuRow(
        number = number,
        label = weapon.displayName.padEnd(18, '.') + " " + cost + "g  ATK +" + weapon.attackContribution,
        enabled = enabled,
        note = when {
            weapon.id == current.id -> "<EQUIPPED>"
            !upgrade -> "<LOWER TIER>"
            level < weapon.minimumLevel -> "<LEVEL " + weapon.minimumLevel + ">"
            gold < cost -> "<NEED " + cost + "g>"
            else -> null
        },
        onClick = { onBuy(weapon.id, revision) },
    )
}

@Composable
fun ArmorShopScreen(
    gameState: GameState,
    message: String?,
    onBuy: (String, Long) -> Unit,
    onReturnToTown: () -> Unit,
) {
    val player = gameState.player
    val current = EquipmentCatalog.requireArmor(player.armorId)
    TerminalSurface {
        ScreenHeader("THE VILLAGE ARMORER", color = AnsiColor.BrightBlue)
        TerminalGap()
        TerminalText("You carry: " + player.gold + " gold", color = AnsiColor.Yellow)
        TerminalText("Your armor: " + current.displayName, color = AnsiColor.BrightGreen)
        TerminalText(
            "Defense: " + player.defense + " (" + player.baseDefense + " + " + current.defenseContribution + ")",
            color = AnsiColor.Cyan,
        )
        TerminalText("Trade-in value: " + current.resaleValue + " gold", color = AnsiColor.DimGray)
        TerminalMessageSlot(message)
        TerminalGap(lines = 2)
        EquipmentCatalog.armors.forEachIndexed { index, armor ->
            ArmorRow(index + 1, player.level, player.gold, current, armor, gameState.revision, onBuy)
        }
        TerminalGap()
        TerminalMenuRow(number = 0, label = "Return to Town", onClick = onReturnToTown)
    }
}

@Composable
private fun ArmorRow(
    number: Int,
    level: Int,
    gold: Int,
    current: ArmorDefinition,
    armor: ArmorDefinition,
    revision: Long,
    onBuy: (String, Long) -> Unit,
) {
    val cost = (armor.purchasePrice - current.resaleValue).coerceAtLeast(0)
    val upgrade = armor.tier > current.tier
    val enabled = upgrade && level >= armor.minimumLevel && gold >= cost
    TerminalMenuRow(
        number = number,
        label = armor.displayName.padEnd(18, '.') + " " + cost + "g  DEF +" + armor.defenseContribution,
        enabled = enabled,
        note = when {
            armor.id == current.id -> "<EQUIPPED>"
            !upgrade -> "<LOWER TIER>"
            level < armor.minimumLevel -> "<LEVEL " + armor.minimumLevel + ">"
            gold < cost -> "<NEED " + cost + "g>"
            else -> null
        },
        onClick = { onBuy(armor.id, revision) },
    )
}

@Composable
fun BankScreen(
    gameState: GameState,
    message: String?,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onDepositAll: (Long) -> Unit,
    onWithdrawAll: (Long) -> Unit,
    onReturnToTown: () -> Unit,
) {
    val player = gameState.player
    TerminalSurface {
        ScreenHeader("FIRST BANK OF THE REALM", color = AnsiColor.Cyan)
        TerminalGap(lines = 2)
        TerminalText("On hand: " + player.gold + " gold", color = AnsiColor.Yellow)
        TerminalText("In bank: " + player.bankedGold + " gold", color = AnsiColor.BrightGreen)
        TerminalGap()
        TerminalText("Gold in the vault is safe from Forest defeat.", color = AnsiColor.LightGray)
        TerminalMessageSlot(message)
        TerminalGap(lines = 2)
        TerminalMenuRow(number = 1, label = "Deposit Gold", enabled = player.gold > 0, onClick = onDeposit)
        TerminalMenuRow(number = 2, label = "Withdraw Gold", enabled = player.bankedGold > 0, onClick = onWithdraw)
        TerminalMenuRow(
            number = 3,
            label = "Deposit All",
            enabled = player.gold > 0,
            note = if (player.gold == 0) "<EMPTY PURSE>" else null,
            onClick = { onDepositAll(gameState.revision) },
        )
        TerminalMenuRow(
            number = 4,
            label = "Withdraw All",
            enabled = player.bankedGold > 0,
            note = if (player.bankedGold == 0) "<EMPTY VAULT>" else null,
            onClick = { onWithdrawAll(gameState.revision) },
        )
        TerminalMenuRow(number = 5, label = "Return to Town", onClick = onReturnToTown)
    }
}

@Composable
fun BankAmountScreen(
    gameState: GameState,
    depositing: Boolean,
    onChoose: (Int, Long) -> Unit,
    onCancel: () -> Unit,
) {
    val available = if (depositing) gameState.player.gold else gameState.player.bankedGold
    val verb = if (depositing) "deposit" else "withdraw"
    val half = available / 2
    TerminalSurface {
        ScreenHeader(if (depositing) "DEPOSIT GOLD" else "WITHDRAW GOLD", color = AnsiColor.Cyan)
        TerminalGap(lines = 2)
        TerminalText("Available: " + available + " gold", color = AnsiColor.Yellow)
        TerminalText("How much do you wish to " + verb + "?", color = AnsiColor.LightGray)
        TerminalGap(lines = 2)
        EconomyRules.BANK_AMOUNTS.forEachIndexed { index, amount ->
            TerminalMenuRow(
                number = index + 1,
                label = amount.toString() + " gold",
                enabled = amount <= available,
                note = if (amount > available) "<UNAVAILABLE>" else null,
                onClick = { onChoose(amount, gameState.revision) },
            )
        }
        TerminalMenuRow(
            number = 4,
            label = "Half (" + half + " gold)",
            enabled = half > 0,
            onClick = { onChoose(half, gameState.revision) },
        )
        TerminalMenuRow(
            number = 5,
            label = "All (" + available + " gold)",
            enabled = available > 0,
            onClick = { onChoose(available, gameState.revision) },
        )
        TerminalMenuRow(number = 6, label = "Cancel", onClick = onCancel)
    }
}
