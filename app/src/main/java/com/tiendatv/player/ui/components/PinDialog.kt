package com.tiendatv.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tiendatv.player.ui.i18n.AppStrings
import com.tiendatv.player.ui.theme.LocalTiendaColors

@Composable
fun PinEntryDialog(
    title: String,
    strings: AppStrings,
    errorText: String? = null,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalTiendaColors.current
    var pin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.width(340.dp),
            elevated = true
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = c.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                // 4-dot PIN indicator
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (i in 0 until 4) {
                        val filled = i < pin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (filled) c.accent else c.glassSurface)
                                .border(1.5.dp, if (filled) c.accent else c.glassBorder, CircleShape)
                        )
                    }
                }

                if (!errorText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorText, color = c.danger, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(18.dp))

                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 78.dp, height = 46.dp)
                                        .tvFocusable(
                                            shape = RoundedCornerShape(12.dp),
                                            onClick = {
                                                when (key) {
                                                    "C" -> pin = ""
                                                    "⌫" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                                    else -> if (pin.length < 4) {
                                                        pin += key
                                                        if (pin.length == 4) {
                                                            onConfirm(pin)
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(c.pillBackground)
                                        .border(1.dp, c.glassBorder, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        color = c.textPrimary,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GlassPillButton(text = strings.cancel, onClick = onDismiss)
                    GlassPillButton(
                        text = strings.confirm,
                        primary = true,
                        onClick = { if (pin.length == 4) onConfirm(pin) }
                    )
                }
            }
        }
    }
}
