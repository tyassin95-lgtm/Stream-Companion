package com.streamcompanion.app.ui.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamcompanion.app.App
import com.streamcompanion.app.data.Pairing
import com.streamcompanion.app.data.PairingException
import com.streamcompanion.app.data.SavedServer
import com.streamcompanion.app.ui.GradientButton
import com.streamcompanion.app.ui.theme.Palette
import kotlinx.coroutines.launch

@Composable
fun PairScreen(host: String, port: Int, onPaired: (SavedServer) -> Unit, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as App
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var serverName by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(host, port) {
        runCatching { Pairing.probe(host, port).first.name }.onSuccess { serverName = it }
        runCatching { focus.requestFocus() }
    }

    fun submit() {
        if (pin.length != 6 || busy) return
        busy = true; error = null
        scope.launch {
            try {
                val server = Pairing.pair(host, port, pin, app.prefs.deviceName, app.prefs.deviceId)
                app.prefs.saveServer(server)
                onPaired(server)
            } catch (e: PairingException) {
                error = e.message; pin = ""
            } finally {
                busy = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Pair with ${serverName ?: host}", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Enter the 6-digit code shown on the Overview page of Stream Companion on your PC.",
            color = Palette.Muted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { v -> pin = v.filter { it.isDigit() }.take(6); error = null; if (pin.length == 6) submit() },
            modifier = Modifier.fillMaxWidth(0.8f).focusRequester(focus),
            singleLine = true,
            enabled = !busy,
            textStyle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 10.sp, textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            isError = error != null,
            placeholder = { Text("······", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 34.sp, letterSpacing = 10.sp, color = Palette.Muted) },
        )
        Spacer(Modifier.height(16.dp))
        if (error != null) {
            Text(error!!, color = Palette.Bad, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
        }
        if (busy) CircularProgressIndicator(color = Palette.Accent)
        else GradientButton("Pair", onClick = ::submit, enabled = pin.length == 6, modifier = Modifier.fillMaxWidth(0.8f))
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text("Cancel") }
    }
}
