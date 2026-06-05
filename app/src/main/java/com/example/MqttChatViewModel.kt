package com.example

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID

class MqttChatViewModel : ViewModel() {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _activeClientId = MutableStateFlow<String?>(null)
    val activeClientId: StateFlow<String?> = _activeClientId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Connection Form Fields
    val broker = MutableStateFlow("broker.emqx.io") // High quality development broker as default
    val port = MutableStateFlow("1883")
    val username = MutableStateFlow("User_${(100..999).random()}")

    // Message Input Field
    val messageInput = MutableStateFlow("")

    private var mqttClient: MqttClient? = null

    fun connect() {
        if (broker.value.isBlank() || port.value.isBlank() || username.value.isBlank()) {
            _errorMessage.value = "Please fill in Broker, Port, and Username."
            return
        }

        val parsedPort = port.value.toIntOrNull()
        if (parsedPort == null) {
            _errorMessage.value = "Invalid Port number format."
            return
        }

        _connectionState.value = ConnectionState.CONNECTING
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Client ID generation: username + current system timestamp
                val timestampSec = System.currentTimeMillis() / 1000
                val cleanUsername = username.value.trim().replace("\\s+".toRegex(), "_")
                val clientId = "${cleanUsername}_$timestampSec"
                _activeClientId.value = clientId

                val brokerUrl = "tcp://${broker.value.trim()}:$parsedPort"
                
                // Clean up previous client
                disconnectExistingClient()

                val persistence = MemoryPersistence()
                val client = MqttClient(brokerUrl, clientId, persistence)

                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 15
                    keepAliveInterval = 60
                }

                client.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        viewModelScope.launch {
                            _connectionState.value = ConnectionState.DISCONNECTED
                            val errorMsg = cause?.message ?: "Broker connection lost"
                            _errorMessage.value = errorMsg
                            Log.e("MqttChat", "Connection lost: $errorMsg", cause)
                        }
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        if (topic == "ubuntu/chat" && message != null) {
                            val payload = String(message.payload)
                            viewModelScope.launch {
                                parseAndAddMessage(payload)
                            }
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        // Optional publish feedback callback
                    }
                })

                client.connect(options)
                
                // Subscribe to topic with QoS 1
                client.subscribe("ubuntu/chat", 1)

                withContext(Dispatchers.Main) {
                    mqttClient = client
                    _connectionState.value = ConnectionState.CONNECTED
                    _errorMessage.value = null
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _errorMessage.value = e.message ?: "Failed to connect"
                    Log.e("MqttChat", "Connection failed", e)
                }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch(Dispatchers.IO) {
            disconnectExistingClient()
            withContext(Dispatchers.Main) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    private fun disconnectExistingClient() {
        try {
            mqttClient?.let { client ->
                if (client.isConnected) {
                    client.disconnect()
                }
            }
        } catch (e: Exception) {
            Log.e("MqttChat", "Error disconnecting client", e)
        } finally {
            mqttClient = null
            _activeClientId.value = null
        }
    }

    fun sendMessage() {
        val text = messageInput.value.trim()
        if (text.isEmpty()) return

        val client = mqttClient
        if (client == null || !client.isConnected) {
            _errorMessage.value = "Not connected to MQTT broker!"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Format: "[用户名]: 消息内容"
                val senderUsername = username.value.trim()
                val payloadString = "[$senderUsername]: $text"
                val message = MqttMessage(payloadString.toByteArray()).apply {
                    qos = 1
                }

                client.publish("ubuntu/chat", message)

                withContext(Dispatchers.Main) {
                    messageInput.value = "" // Clear the message input box on success
                    _errorMessage.value = null
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to publish: ${e.message}"
                }
            }
        }
    }

    private fun parseAndAddMessage(payload: String) {
        // Strict pattern matching: "[Username]: content"
        val regex = """^\[([^\]]+)\]:\s*(.*)$""".toRegex()
        val matchResult = regex.find(payload)

        val chatMessage = if (matchResult != null) {
            val sender = matchResult.groupValues[1]
            val text = matchResult.groupValues[2]
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = sender,
                text = text,
                isMe = sender == username.value.trim()
            )
        } else {
            // Fallback for non-matching payloads
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = "Other User / Raw",
                text = payload,
                isMe = false
            )
        }

        _messages.value = _messages.value + chatMessage
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        disconnectExistingClient()
    }
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

data class ChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMe: Boolean
)
