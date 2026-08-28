package com.example.speedlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SpeedLabTheme { SpeedLabHome() } }
    }
}

private val Navy = Color(0xFF071B33)
private val Aqua = Color(0xFF28D7C0)
private val Mist = Color(0xFFF4F7FB)

@Composable
private fun SpeedLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Navy,
            secondary = Aqua,
            background = Mist,
            surface = Color.White,
        ),
        content = content,
    )
}

@Composable
private fun SpeedLabHome() {
    Surface(modifier = Modifier.fillMaxSize(), color = Mist) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(Navy, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text("S", color = Aqua, fontWeight = FontWeight.Black, fontSize = 21.sp) }
                Text("SpeedLab", modifier = Modifier.padding(start = 12.dp), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Navy)
            }
            Spacer(Modifier.height(58.dp))
            Text("Ready to test", color = Color(0xFF637084), fontSize = 15.sp)
            Spacer(Modifier.height(16.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(252.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(Color(0xFFDCE6EF), 145f, 250f, false, style = Stroke(18.dp.toPx(), cap = StrokeCap.Round), size = Size(size.width, size.height))
                    drawArc(Brush.sweepGradient(listOf(Aqua, Color(0xFF4B8FFF), Aqua)), 145f, 170f, false, style = Stroke(18.dp.toPx(), cap = StrokeCap.Round), size = Size(size.width, size.height))
                    drawLine(Navy, center, Offset(center.x + 50.dp.toPx(), center.y - 48.dp.toPx()), 7.dp.toPx(), StrokeCap.Round)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("0.0", fontWeight = FontWeight.Black, fontSize = 52.sp, color = Navy)
                    Text("Mbps", color = Color(0xFF637084))
                }
            }
            Spacer(Modifier.height(26.dp))
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy),
                shape = RoundedCornerShape(18.dp),
            ) { Text("START TEST", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
            Spacer(Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Text("TEST", color = Navy, fontWeight = FontWeight.Bold)
                Text("HISTORY", color = Color(0xFF8A96A7))
                Text("SETTINGS", color = Color(0xFF8A96A7))
            }
        }
    }
}
