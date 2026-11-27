package com.tiendatv.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.tiendatv.player.config.AppConfig
import com.tiendatv.player.data.local.AppLanguage
import com.tiendatv.player.data.local.ProfileEntity
import com.tiendatv.player.data.local.UserSettings
import com.tiendatv.player.player.BufferPreset
import com.tiendatv.player.player.VideoAspectMode
import com.tiendatv.player.ui.components.GlassCard
import com.tiendatv.player.ui.components.GlassPillButton
import com.tiendatv.player.ui.components.PinEntryDialog
import com.tiendatv.player.ui.components.SectionHeader
import com.tiendatv.player.ui.components.tvFocusable
import com.tiendatv.player.ui.i18n.AppStrings
import com.tiendatv.player.ui.theme.LocalTiendaColors

@Composable
fun SettingsScreen(
    settings: UserSettings,
    profiles: List<ProfileEntity>,
    strings: AppStrings,
    onToggleDarkCinema: (Boolean) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectBuffer: (BufferPreset) -> Unit,
    onToggleSoftwareDecoder: (Boolean) -> Unit,
    onSelectAspectMode: (VideoAspectMode) -> Unit,
    onUpdateParental: (enabled: Boolean, pin: String) -> Unit,
    onClearHistory: () -> Unit,
    onSyncActiveProfile: () -> Unit,
    onSwitchProfile: (ProfileEntity) -> Unit,
    onDeleteProfile: (ProfileEntity) -> Unit,
    onOpenAddProfile: () -> Unit
) {
    val c = LocalTiendaColors.current
    var showChangePinDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 14.dp, bottom = 36.dp, start = 8.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.settingsTitle,
                    color = c.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${AppConfig.APP_NAME} v${AppConfig.APP_VERSION}",
                    color = c.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 1. Appearance & Language (Dark Cinema Mode + ES / EN toggle)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionHeader(title = strings.appearanceSection)

                    SettingsToggleRow(
                        title = strings.darkCinemaMode,
                        subtitle = strings.darkCinemaSubtitle,
                        checked = settings.darkCinemaMode,
                        onCheckedChange = onToggleDarkCinema
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.languageLabel,
                            color = c.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassPillButton(
                                text = "Español (ES)",
                                primary = settings.language == AppLanguage.ES,
                                onClick = { onSelectLanguage(AppLanguage.ES) }
                            )
                            GlassPillButton(
                                text = "English (EN)",
                                primary = settings.language == AppLanguage.EN,
                                onClick = { onSelectLanguage(AppLanguage.EN) }
                            )
                        }
                    }
                }
            }
        }

        // 2. Playback & Stability
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionHeader(title = strings.playbackSection)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.bufferSizeLabel,
                            color = c.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassPillButton(
                                text = strings.bufferLow,
                                primary = settings.bufferPreset == BufferPreset.LOW,
                                onClick = { onSelectBuffer(BufferPreset.LOW) }
                            )
                            GlassPillButton(
                                text = strings.bufferBalanced,
                                primary = settings.bufferPreset == BufferPreset.BALANCED,
                                onClick = { onSelectBuffer(BufferPreset.BALANCED) }
                            )
                            GlassPillButton(
                                text = strings.bufferHigh,
                                primary = settings.bufferPreset == BufferPreset.HIGH,
                                onClick = { onSelectBuffer(BufferPreset.HIGH) }
                            )
                            GlassPillButton(
                                text = strings.bufferMax,
                                primary = settings.bufferPreset == BufferPreset.MAX,
                                onClick = { onSelectBuffer(BufferPreset.MAX) }
                            )
                        }
                    }

                    SettingsToggleRow(
                        title = strings.softwareDecoderLabel,
                        subtitle = strings.softwareDecoderSubtitle,
                        checked = settings.preferSoftwareDecoder,
                        onCheckedChange = onToggleSoftwareDecoder
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.defaultAspectLabel,
                            color = c.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassPillButton(
                                text = strings.aspectFit,
                                primary = settings.defaultAspectMode == VideoAspectMode.FIT,
                                onClick = { onSelectAspectMode(VideoAspectMode.FIT) }
                            )
                            GlassPillButton(
                                text = strings.aspectZoom,
                                primary = settings.defaultAspectMode == VideoAspectMode.ZOOM,
                                onClick = { onSelectAspectMode(VideoAspectMode.ZOOM) }
                            )
                            GlassPillButton(
                                text = strings.aspectFill,
                                primary = settings.defaultAspectMode == VideoAspectMode.FILL,
                                onClick = { onSelectAspectMode(VideoAspectMode.FILL) }
                            )
                            GlassPillButton(
                                text = strings.aspectWidth,
                                primary = settings.defaultAspectMode == VideoAspectMode.FIXED_WIDTH,
                                onClick = { onSelectAspectMode(VideoAspectMode.FIXED_WIDTH) }
                            )
                        }
                    }
                }
            }
        }

        // 3. Parental Control PIN & History
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionHeader(title = strings.parentalSection)

                    SettingsToggleRow(
                        title = strings.parentalEnabledLabel,
                        subtitle = "${strings.parentalPinLabel}: ••••",
                        checked = settings.parentalEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateParental(enabled, settings.parentalPin)
                        }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassPillButton(
                            text = strings.changePin,
                            icon = Icons.Rounded.Lock,
                            onClick = { showChangePinDialog = true }
                        )
                        GlassPillButton(
                            text = strings.clearWatchHistory,
                            icon = Icons.Rounded.Delete,
                            onClick = onClearHistory
                        )
                    }
                }
            }
        }

        // 4. Profiles & Playlists
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(title = strings.profilesSection)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassPillButton(
                                text = strings.syncCatalog,
                                icon = Icons.Rounded.Refresh,
                                onClick = onSyncActiveProfile
                            )
                            GlassPillButton(
                                text = strings.addProfile,
                                icon = Icons.Rounded.Add,
                                primary = true,
                                onClick = onOpenAddProfile
                            )
                        }
                    }

                    profiles.forEach { profile ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (profile.isActive) c.accentSoft else c.glassSurface)
                                .border(
                                    1.dp,
                                    if (profile.isActive) c.accent else c.glassBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.name,
                                    color = c.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val exp = if (profile.expirationDate.isNotBlank()) {
                                    " • ${strings.expiresLabel} ${profile.expirationDate}"
                                } else ""
                                Text(
                                    text = "${profile.type} • ${profile.serverUrl}$exp",
                                    color = c.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!profile.isActive) {
                                    GlassPillButton(
                                        text = strings.switchProfile,
                                        primary = true,
                                        horizontalPadding = 12.dp,
                                        verticalPadding = 7.dp,
                                        onClick = { onSwitchProfile(profile) }
                                    )
                                } else {
                                    Text(
                                        text = strings.activeProfile,
                                        color = c.accent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    )
                                }
                                GlassPillButton(
                                    text = strings.deleteProfile,
                                    horizontalPadding = 12.dp,
                                    verticalPadding = 7.dp,
                                    onClick = { onDeleteProfile(profile) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showChangePinDialog) {
        PinEntryDialog(
            title = strings.enterNewPinTitle,
            strings = strings,
            onConfirm = { newPin ->
                onUpdateParental(true, newPin)
                showChangePinDialog = false
            },
            onDismiss = { showChangePinDialog = false }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val c = LocalTiendaColors.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(
                shape = shape,
                focusScale = 1.01f,
                onClick = { onCheckedChange(!checked) }
            )
            .clip(shape)
            .background(c.glassSurface)
            .border(1.dp, c.glassBorder, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = c.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = c.textSecondary,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = c.accent
            )
        )
    }
}
