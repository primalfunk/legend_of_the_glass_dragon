package com.legendoftheglassdragon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legendoftheglassdragon.data.TrophyStoreState
import com.legendoftheglassdragon.game.GameState
import com.legendoftheglassdragon.game.TrophyCatalog
import com.legendoftheglassdragon.game.TrophyCategory
import com.legendoftheglassdragon.game.TrophyMetagameSnapshot
import com.legendoftheglassdragon.game.TrophyUnlock

@Composable
fun PlayerLayerScreen(
    characterName: String?,
    day: Int?,
    trophies: TrophyStoreState,
    onContinue: () -> Unit,
    onGuide: () -> Unit,
    onTrophies: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("PLAYER LAYER", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("A modern utility around an old game.", style = MaterialTheme.typography.headlineSmall)
            Text(
                if (characterName == null) "Step into the realm when you are ready."
                else "$characterName · Day ${day ?: 1}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                Text(if (characterName == null) "Return to menu" else "Continue to game")
            }
            OutlinedButton(onClick = onGuide, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                Text("Guide")
            }
            OutlinedButton(onClick = onTrophies, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                Text("Trophies  ${trophies.earnedCount} / 50")
            }
        }
    }
}

@Composable
fun WelcomeScreen(onDismiss: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("WELCOME", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("Welcome to Legend of the Glass Dragon.", style = MaterialTheme.typography.headlineSmall)
            Text("You are an adventurer building a name, strength, and fortune in a living old-school RPG. The long-term goal is to become strong enough to face the Glass Dragon.")
            Text("The game moves in days. Each day gives you a limited number of actions, and fighting in the forest is the basic route to experience and strength.")
            Text("Level up and train, spend gold on better equipment, and remember that defeat is possible. Other adventurers are moving through the simulated realm too.")
            Text("You can play another day immediately. There is no real-world waiting period.")
            Spacer(Modifier.height(6.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(15.dp)) {
                Text("Start playing")
            }
        }
    }
}

private val guideSections = listOf(
    "Welcome" to "You are an adventurer in a simulated old-school RPG. Build strength and fortune until you can face the Glass Dragon.",
    "Playing a Day" to "Each day gives you a limited number of actions. Fight, visit town services, use the Inn, or end the day to move the realm forward.",
    "Fighting & Healing" to "Forest fights are your main source of experience and gold. The healer can restore you, but healing costs gold. A defeat ends that day's forest fighting.",
    "Training & Levels" to "Experience qualifies you for the Training Hall. Successful training raises your level and improves your underlying combat ability.",
    "Equipment & Gold" to "Weapons and armor add directly to attack and defense. Carry gold, use the bank, and choose when spending is worth the risk.",
    "Other Adventurers" to "The community is simulated locally. Rankings, news, rivalries, challenges, and Dragon attempts change as days pass.",
    "The Glass Dragon" to "The Dragon arrives through a season of incidents. Prepare carefully, then use Dragon Watch when the challenge window opens.",
)

@Composable
fun GuideScreen(onClose: () -> Unit) {
    var expanded by remember { mutableStateOf("Welcome") }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Guide", style = MaterialTheme.typography.headlineMedium)
                    Text("A quick orientation outside the BBS game", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onClose) { Text("Close") }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(guideSections) { (title, body) ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { expanded = if (expanded == title) "" else title },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(title, fontWeight = FontWeight.SemiBold)
                            if (expanded == title) {
                                Spacer(Modifier.height(8.dp))
                                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrophyCabinetScreen(
    gameState: GameState?,
    store: TrophyStoreState,
    onClose: () -> Unit,
) {
    val snapshot = TrophyMetagameSnapshot(store.earned.keys, store.dragonVictories)
    var category by remember { mutableStateOf<TrophyCategory?>(null) }
    val trophies = TrophyCatalog.all.filter { category == null || it.category == category }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Trophies", style = MaterialTheme.typography.headlineMedium)
                    Text("${store.earnedCount} / 50 earned", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onClose) { Text("Close") }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        TextButton(onClick = { category = null }) {
                            Text("All", fontSize = 14.sp)
                        }
                        TrophyCategory.values().take(3).forEach { item ->
                            TextButton(onClick = { category = item }) {
                                Text(item.label.substringBefore(' '), fontSize = 14.sp)
                            }
                        }
                    }
                    Divider()
                }
                items(trophies) { trophy ->
                    val earned = store.earned[trophy.id]
                    val visible = earned != null || !trophy.secret
                    val progress = if (visible && earned == null) gameState?.let { trophy.progress?.invoke(it, snapshot) } else null
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (earned != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Text(if (earned != null) "🏆" else if (visible) "◇" else "?", modifier = Modifier.size(32.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (visible) trophy.name else "HIDDEN TROPHY", fontWeight = FontWeight.Bold)
                                Text(if (visible) trophy.description else "The requirement will be revealed when earned.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (earned == null && progress != null) Text("${progress.current} / ${progress.target}", color = MaterialTheme.colorScheme.primary)
                                if (earned != null) Text("Earned", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrophyPopup(unlock: TrophyUnlock, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 26.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF243B53)),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("TROPHY EARNED", style = MaterialTheme.typography.labelLarge, color = Color(0xFFFFD166), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(unlock.trophy.name, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(unlock.trophy.description, color = Color.White.copy(alpha = 0.82f))
            }
        }
    }
}
