package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MqttChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    MqttChatScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MqttChatScreen(
    viewModel: MqttChatViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val broker by viewModel.broker.collectAsState()
    val port by viewModel.port.collectAsState()
    val username by viewModel.username.collectAsState()
    val messageInput by viewModel.messageInput.collectAsState()

    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Auto-scroll to the newest message whenever the list gets updated
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    var isSettingsExpanded by remember { mutableStateOf(true) }

    // When connection is successful, automatically collapse the configuration card to save screen estate
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            isSettingsExpanded = false
        } else if (connectionState == ConnectionState.DISCONNECTED) {
            isSettingsExpanded = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        // App Identity Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "CloudChat-Pro",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val dotColor = when (connectionState) {
                            ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                            ConnectionState.CONNECTING -> Color(0xFFFFB300)
                            ConnectionState.DISCONNECTED -> Color(0xFFF44336)
                        }

                        val statusText = when (connectionState) {
                            ConnectionState.CONNECTED -> "Broker Connected"
                            ConnectionState.CONNECTING -> "Connecting..."
                            ConnectionState.DISCONNECTED -> "Disconnected"
                        }

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )

                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = { isSettingsExpanded = !isSettingsExpanded },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isSettingsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Settings Panel",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Error message banner
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error Icon",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss error",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { viewModel.clearErrorMessage() }
                        )
                    }
                }
            }
        }

        // Configuration card
        AnimatedVisibility(
            visible = isSettingsExpanded,
            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "MQTT BROKER CONNECTION CONFIGURATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Start
                    )

                    OutlinedTextField(
                        value = broker,
                        onValueChange = { viewModel.broker.value = it },
                        label = { Text("Broker IP / Domain") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broker_input"),
                        singleLine = true,
                        placeholder = { Text("e.g. broker.emqx.io") },
                        enabled = connectionState == ConnectionState.DISCONNECTED,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Build,
                                contentDescription = "Broker icon",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = port,
                            onValueChange = { viewModel.port.value = it },
                            label = { Text("Port") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("port_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("1883") },
                            enabled = connectionState == ConnectionState.DISCONNECTED,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Port icon",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = { viewModel.username.value = it },
                            label = { Text("Username") },
                            modifier = Modifier
                                .weight(2.5f)
                                .testTag("username_input"),
                            singleLine = true,
                            enabled = connectionState == ConnectionState.DISCONNECTED,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = "Username icon",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (connectionState == ConnectionState.DISCONNECTED) {
                                viewModel.connect()
                            } else {
                                viewModel.disconnect()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag(if (connectionState == ConnectionState.DISCONNECTED) "connect_button" else "disconnect_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (connectionState) {
                                ConnectionState.CONNECTED -> MaterialTheme.colorScheme.error
                                ConnectionState.CONNECTING -> MaterialTheme.colorScheme.tertiary
                                ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.primary
                            }
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (connectionState == ConnectionState.CONNECTING) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Connect/Disconnect Action",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = when (connectionState) {
                                    ConnectionState.CONNECTED -> "Disconnect"
                                    ConnectionState.CONNECTING -> "Connecting..."
                                    ConnectionState.DISCONNECTED -> "Connect Broker"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Summary bar when collapsed
        AnimatedVisibility(
            visible = !isSettingsExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .clickable { isSettingsExpanded = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Connected to ${broker}:${port} as [$username]",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Show Settings",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // Center section: chat lists (completely blank initially)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = "Empty board logo",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "CloudChat-Pro Stream",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Chat list is clean and blank. Connecting to the broker subscribes you to standard real-time messages broadcasted on \"ubuntu/chat\".",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .testTag("messages_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        MessageBubble(
                            msg = msg,
                            modifier = Modifier.testTag("message_item_${msg.id}")
                        )
                    }
                }
            }
        }

        // Message input bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeContent)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { viewModel.messageInput.value = it },
                    placeholder = {
                        Text(
                            text = if (connectionState == ConnectionState.CONNECTED)
                                "Send message to ubuntu/chat..."
                            else
                                "Connect to broker first to tap..."
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("message_input"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    enabled = connectionState == ConnectionState.CONNECTED,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (messageInput.trim().isNotEmpty()) {
                                viewModel.sendMessage()
                                focusManager.clearFocus()
                            }
                        }
                    )
                )

                FloatingActionButton(
                    onClick = {
                        if (messageInput.trim().isNotEmpty()) {
                            viewModel.sendMessage()
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_button"),
                    containerColor = if (connectionState == ConnectionState.CONNECTED && messageInput.trim().isNotEmpty())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (connectionState == ConnectionState.CONNECTED && messageInput.trim().isNotEmpty())
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send trigger action",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    msg: ChatMessage,
    modifier: Modifier = Modifier
) {
    val alignment = if (msg.isMe) Alignment.End else Alignment.Start
    val bubbleColor = if (msg.isMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = if (msg.isMe) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (msg.isMe) "Me" else "[${msg.sender}]",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (msg.isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
            )

            // Dynamic timestamp
            val timeString = remember(msg.timestamp) {
                try {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    sdf.format(Date(msg.timestamp))
                } catch (e: Exception) {
                    ""
                }
            }
            Text(
                text = timeString,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (msg.isMe) 16.dp else 4.dp,
                bottomEnd = if (msg.isMe) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = msg.text,
                color = textColor,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}
