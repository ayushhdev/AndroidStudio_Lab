package com.example.notificationapp

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.notificationapp.ui.theme.AccentLavender
import com.example.notificationapp.ui.theme.CardSurface
import com.example.notificationapp.ui.theme.GradientBottom
import com.example.notificationapp.ui.theme.GradientMid
import com.example.notificationapp.ui.theme.GradientTop
import com.example.notificationapp.ui.theme.NotificationAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One row in the demo list: what to show and which helper call posts it. */
private data class NotificationDemo(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onSend: () -> Unit
)

class NotificationsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createChannels(this)

        val email = intent.getStringExtra(IntentKeys.EXTRA_EMAIL).orEmpty()

        setContent {
            NotificationAppTheme(dynamicColor = false) {
                NotificationsScreen(
                    userName = email.substringBefore("@"),
                    onOpenSettings = { openNotificationSettings() }
                )
            }
        }
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}

@Composable
fun NotificationsScreen(userName: String, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var visible by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(hasNotificationPermission(context)) }
    var lastSent by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) { visible = true }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    /** Every demo goes through here so the permission is never assumed. */
    fun send(label: String, action: () -> Unit) {
        if (!permissionGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onOpenSettings()
            }
            return
        }
        action()
        lastSent = label
    }

    val demos = listOf(
        NotificationDemo(
            title = "Simple notification",
            subtitle = "Title, one line of text, tap to open a screen",
            icon = Icons.Filled.Notifications,
            onSend = { send("Simple notification") { NotificationHelper.showSimple(context, userName) } }
        ),
        NotificationDemo(
            title = "Expandable (BigTextStyle)",
            subtitle = "Collapses to one line, expands to a paragraph",
            icon = Icons.Filled.UnfoldMore,
            onSend = { send("Expandable notification") { NotificationHelper.showBigText(context) } }
        ),
        NotificationDemo(
            title = "Inbox style",
            subtitle = "Groups three updates under one summary",
            icon = Icons.Filled.Inbox,
            onSend = { send("Inbox notification") { NotificationHelper.showInbox(context) } }
        ),
        NotificationDemo(
            title = "With action buttons",
            subtitle = "Mark done / Snooze handled by a BroadcastReceiver",
            icon = Icons.Filled.TouchApp,
            onSend = { send("Notification with actions") { NotificationHelper.showWithActions(context) } }
        ),
        NotificationDemo(
            title = "Progress notification",
            subtitle = "Ongoing download bar that updates to 100%",
            icon = Icons.Filled.Download,
            onSend = {
                send("Progress notification") {
                    scope.launch {
                        for (p in 0..100 step 20) {
                            NotificationHelper.showProgress(context, p)
                            delay(600)
                        }
                        NotificationHelper.showProgressComplete(context)
                    }
                }
            }
        ),
        NotificationDemo(
            title = "Heads-up alert",
            subtitle = "High importance channel, pops over the current screen",
            icon = Icons.Filled.Campaign,
            onSend = { send("Heads-up alert") { NotificationHelper.showHeadsUp(context) } }
        )
    )

    Scaffold(containerColor = Color.Transparent) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(GradientTop, GradientMid, GradientBottom)))
                .padding(padding)
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(600)
                )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, bottom = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(AccentLavender.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = AccentLavender,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Notifications",
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Send a notification and pull down the shade",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    if (!permissionGranted) {
                        item {
                            PermissionCard(
                                onRequest = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        onOpenSettings()
                                    }
                                }
                            )
                        }
                    }

                    lastSent?.let { label ->
                        item {
                            Text(
                                "Last sent: $label",
                                color = AccentLavender,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    items(demos) { demo ->
                        DemoCard(demo)
                    }

                    item {
                        OutlinedButton(
                            onClick = {
                                NotificationHelper.cancelAll(context)
                                lastSent = null
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentLavender),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(top = 4.dp)
                        ) {
                            Icon(
                                Icons.Filled.NotificationsOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear all notifications", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DemoCard(demo: NotificationDemo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(AccentLavender.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    demo.icon,
                    contentDescription = null,
                    tint = AccentLavender,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(demo.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    demo.subtitle,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Button(
                onClick = demo.onSend,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentLavender,
                    contentColor = Color(0xFF1A0B2E)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                Text("Send", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PermissionCard(onRequest: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "Notifications are turned off",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Android 13 and above needs the POST_NOTIFICATIONS permission before the app " +
                    "can post anything to the shade.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )
            Button(
                onClick = onRequest,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentLavender,
                    contentColor = Color(0xFF1A0B2E)
                )
            ) {
                Text("Allow notifications", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

private fun hasNotificationPermission(context: android.content.Context): Boolean {
    val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
}
