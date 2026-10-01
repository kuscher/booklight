package io.github.kuscher.booklight.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Test content to put behind the panel (debug builds only): the hard cases for glass side by side.
 * A white document with black text, a dark terminal, and saturated colour. `./bl backdrop` opens it.
 */
class BackdropActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prose = "The quick brown fox jumps over the lazy dog. Pack my box with five dozen liquor jugs. ".repeat(60)
        val shell = buildString { repeat(60) { append("droid@book:~/booklight\$ ./gradlew :core:test --console=plain\nBUILD SUCCESSFUL in 4s  ·  36 tests  ·  0 failures\n") } }
        setContent {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight().background(Color.White).padding(20.dp)) {
                    Text(prose, style = TextStyle(color = Color.Black, fontSize = 15.sp, lineHeight = 24.sp))
                }
                Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF101216)).padding(20.dp)) {
                    Text(shell, style = TextStyle(color = Color(0xFF7CE38B), fontSize = 13.sp, lineHeight = 20.sp, fontFamily = FontFamily.Monospace))
                }
                Column(Modifier.weight(0.7f).fillMaxHeight()) {
                    Box(Modifier.weight(1f).fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFFFF5E3A), Color(0xFFFFC233), Color(0xFF34C759)))))
                    Box(Modifier.weight(1f).fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0A84FF), Color(0xFFBF5AF2), Color(0xFFFF375F))))) {
                        Text("PHOTO", Modifier.padding(20.dp), style = TextStyle(color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black))
                    }
                }
            }
        }
    }
}
