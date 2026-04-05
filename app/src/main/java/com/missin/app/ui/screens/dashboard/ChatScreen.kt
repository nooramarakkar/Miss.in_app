package com.missin.app.ui.screens.dashboard

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.missin.app.data.model.Message
import com.missin.app.ui.viewmodel.ChatViewModel
import com.missin.app.utils.redactName
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale

fun Modifier.bounceClick(interactionSource: MutableInteractionSource) = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, tween(100))
    this.scale(scale)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    claimId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val otherUserName by viewModel.otherUserName.collectAsState()
    val otherUserPhone by viewModel.otherUserPhone.collectAsState()
    val claim by viewModel.claimRequest.collectAsState()
    val isResolving by viewModel.isResolving.collectAsState()
    val isResolvedSuccess by viewModel.isResolvedSuccess.collectAsState()

    var textInput by remember { mutableStateOf("") }
    
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val isOwner = claim?.claimerId == currentUid   // claimerId = person who submitted the claim

    val deepNavy = com.missin.app.ui.theme.MatteCharcoal
    val textColor = com.missin.app.ui.theme.BoneWhite
    val electricBlue = com.missin.app.ui.theme.MutedEmerald

    LaunchedEffect(Unit) {
        viewModel.loadChat(claimId)
    }

    LaunchedEffect(isResolvedSuccess) {
        if (isResolvedSuccess) {
            Toast.makeText(context, "Item Loop Closed. +10 Karma awarded to Finder!", Toast.LENGTH_LONG).show()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(otherUserName.redactName(), color = textColor, style = MaterialTheme.typography.titleMedium)
                        Text(if (claim?.status == "resolved") "Resolved" else "Active Connection", color = electricBlue, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                actions = {
                    if (claim?.status != "resolved" && claim?.callRequested != true) {
                        val intSourceReq = remember { MutableInteractionSource() }
                        TextButton(
                            onClick = { viewModel.requestCall() },
                            interactionSource = intSourceReq,
                            modifier = Modifier.bounceClick(intSourceReq)
                        ) {
                            Text("Request Call", color = electricBlue, fontWeight = FontWeight.Bold)
                        }
                    } else if (claim?.callRequested == true && claim?.callAccepted != true && !isOwner) {
                        val intSourceApprove = remember { MutableInteractionSource() }
                        TextButton(
                            onClick = { viewModel.acceptCall() },
                            interactionSource = intSourceApprove,
                            modifier = Modifier.bounceClick(intSourceApprove)
                        ) {
                            Text("Accept Call", color = electricBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (isOwner && claim?.status != "resolved") {
                        val intSourceConfirm = remember { MutableInteractionSource() }
                        TextButton(
                            onClick = { viewModel.resolveItem() }, 
                            enabled = !isResolving,
                            interactionSource = intSourceConfirm,
                            modifier = Modifier.bounceClick(intSourceConfirm)
                        ) {
                            Text("Claim Complete", color = electricBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = deepNavy)
            )
        },
        containerColor = deepNavy
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = false,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(messages) { msg ->
                    ChatBubble(
                        message = msg, 
                        isMe = msg.senderId == currentUid,
                        textColor = textColor,
                        electricBlue = electricBlue,
                        onRevealApprove = { viewModel.sendMessage("", "reveal_approved") },
                        onCall = {
                            if (otherUserPhone != null) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$otherUserPhone"))
                                context.startActivity(intent)
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            if (claim?.status != "resolved") {
                // Input Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(textColor.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message...", color = textColor.copy(alpha = 0.3f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        singleLine = true
                    )
                    
                    if (textInput.isBlank()) {
                        // Empty field: show Mic only
                        IconButton(onClick = {
                            Toast.makeText(context, "Voice Notes Coming Soon", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Filled.Mic, contentDescription = "Voice", tint = textColor.copy(alpha = 0.7f))
                        }
                    } else {
                        // Text entered: show Send only
                        val intSourceSend = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                            },
                            interactionSource = intSourceSend,
                            modifier = Modifier.bounceClick(intSourceSend)
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Send", tint = electricBlue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: Message, 
    isMe: Boolean, 
    textColor: Color, 
    electricBlue: Color,
    onRevealApprove: () -> Unit,
    onCall: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        val backgroundColor = if (isMe) electricBlue else textColor.copy(alpha = 0.1f)
        val contentColor = if (isMe) Color.White else textColor
        val shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isMe) 16.dp else 4.dp,
            bottomEnd = if (isMe) 4.dp else 16.dp
        )

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(backgroundColor)
                .padding(12.dp)
        ) {
            when (message.type) {
                "text" -> Text(message.content, color = contentColor)
                "image" -> {
                    Column {
                        Text("Image Attachment", color = contentColor, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(message.content, color = contentColor) // In MVP this would be an AsyncImage
                    }
                }
                "voice" -> Text("🎤 Audio Message", color = contentColor)
                "reveal_request" -> {
                    Column {
                        Text("Requested Phone Number Reveal", color = contentColor, fontWeight = FontWeight.Bold)
                    }
                }
                "reveal_approved" -> {
                    Column {
                        Text("Phone Number Revealed", color = contentColor, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        val isrcCall = remember { MutableInteractionSource() }
                        Button(
                            onClick = onCall, 
                            colors = ButtonDefaults.buttonColors(containerColor = electricBlue, contentColor = Color.White),
                            interactionSource = isrcCall,
                            modifier = Modifier.bounceClick(isrcCall)
                        ) {
                            Icon(Icons.Filled.Phone, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Call Now")
                        }
                    }
                }
            }
        }
    }
}
