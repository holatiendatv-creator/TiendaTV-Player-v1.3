package com.tiendatv.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tiendatv.player.config.AppConfig
import com.tiendatv.player.data.local.ProfileEntity
import com.tiendatv.player.ui.components.GlassCard
import com.tiendatv.player.ui.components.GlassPillButton
import com.tiendatv.player.ui.components.tvFocusable
import com.tiendatv.player.ui.i18n.AppStrings
import com.tiendatv.player.ui.theme.LocalTiendaColors

@Composable
fun LoginScreen(
    profiles: List<ProfileEntity>,
    isBusy: Boolean,
    errorMessage: String?,
    strings: AppStrings,
    onAddXtream: (name: String, serverUrl: String, username: String, password: String) -> Unit,
    onAddM3u: (name: String, playlistUrl: String, epgUrl: String) -> Unit,
    onSelectProfile: (ProfileEntity) -> Unit,
    onCancel: (() -> Unit)? = null
) {
    val c = LocalTiendaColors.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.textPrimary,
        unfocusedTextColor = c.textPrimary,
        focusedBorderColor = c.accent,
        unfocusedBorderColor = c.glassBorder,
        focusedLabelColor = c.accent,
        unfocusedLabelColor = c.textSecondary,
        cursorColor = c.accent
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .width(520.dp)
                .verticalScroll(rememberScrollState()),
            elevated = true
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(c.accent, c.accentLight))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LiveTv,
                            contentDescription = "TiendaTV",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column {
                        Text(
                            text = strings.welcomeTitle,
                            color = c.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (strings.navHome == "Inicio") {
                                "Ingresa tu usuario y contraseña para comenzar."
                            } else {
                                "Sign in with your username and password."
                            },
                            color = c.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(strings.username) },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(strings.password) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = c.danger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            color = c.accent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = strings.syncingCatalog,
                            color = c.textSecondary,
                            fontSize = 14.sp
                        )
                    } else {
                        GlassPillButton(
                            text = strings.connectAndSync,
                            icon = Icons.Rounded.Login,
                            primary = true,
                            onClick = {
                                val label = username.trim().ifBlank { "TiendaTV" }
                                onAddXtream(label, AppConfig.DEFAULT_SERVER_URL, username, password)
                            }
                        )
                        if (onCancel != null) {
                            GlassPillButton(
                                text = strings.cancel,
                                onClick = onCancel
                            )
                        }
                    }
                }

                if (profiles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.savedProfiles,
                        color = c.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    profiles.forEach { prof ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusable(
                                    shape = RoundedCornerShape(14.dp),
                                    onClick = { onSelectProfile(prof) }
                                )
                                .clip(RoundedCornerShape(14.dp))
                                .background(c.glassSurface)
                                .border(1.dp, c.glassBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = prof.name,
                                    color = c.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (prof.expirationDate.isNotBlank()) {
                                    Text(
                                        text = "${strings.expiresLabel} ${prof.expirationDate}",
                                        color = c.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            if (prof.isActive) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = strings.activeProfile,
                                    tint = c.success
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
