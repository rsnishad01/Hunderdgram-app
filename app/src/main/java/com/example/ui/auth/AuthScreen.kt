package com.example.ui.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.theme.HundredGramCardBackground
import com.example.ui.theme.HundredGramCardElevated
import com.example.ui.theme.HundredGramDarkBackground
import com.example.ui.theme.HundredGramDivider
import com.example.ui.theme.HundredGramPink
import com.example.ui.theme.HundredGramTextPrimary
import com.example.ui.theme.HundredGramTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isSignUp by remember { mutableStateOf(false) }
    var authMode by remember { mutableStateOf("EMAIL") } // "EMAIL", "PHONE"
    var usernameOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var latestOtpReceived by remember { mutableStateOf("") }
    var otpCountdown by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var showFacebookDialog by remember { mutableStateOf(false) }
    var fbNameInput by remember { mutableStateOf("") }
    var fbEmailInput by remember { mutableStateOf("") }

    // Google Sign-In Popup Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isLoading = true
            coroutineScope.launch {
                val authResult = viewModel.repository.googleAuthManager.handleSignInResult(result.data)
                authResult.onSuccess { user ->
                    viewModel.loginUser(user)
                    Toast.makeText(context, "Signed in as ${user.displayName}", Toast.LENGTH_SHORT).show()
                    viewModel.navigateTo(ScreenDestination.Feed)
                }.onFailure { error ->
                    Toast.makeText(context, "Google Sign-In: ${error.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                }
                isLoading = false
            }
        }
    }

    // OTP Timer
    LaunchedEffect(otpCountdown) {
        if (otpCountdown > 0) {
            delay(1000)
            otpCountdown -= 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HundredGramDarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0xFFFA7E1E))
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.5f),
                                Color(0xFFFF007F).copy(alpha = 0.3f),
                                Color(0xFFFA7E1E).copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                AsyncImage(
                    model = com.example.R.drawable.img_welcome_logo,
                    contentDescription = "HundredGram Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "HundredGram",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HundredGramPink
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isSignUp) "Create your authentic profile" else "Log in to your account",
                color = HundredGramTextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // 🌟 1. PROMINENT FACEBOOK & GOOGLE LOGIN BUTTONS
            // =========================================================================

            // 📘 FACEBOOK LOGIN BUTTON
            Button(
                onClick = {
                    showFacebookDialog = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1877F2),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("f", color = Color(0xFF1877F2), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Log In with Facebook (फेसबुक लॉगिन)",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 🔵 GOOGLE SIGN-IN POPUP BUTTON
            Button(
                onClick = {
                    try {
                        val signInIntent = viewModel.repository.googleAuthManager.getSignInIntent()
                        googleSignInLauncher.launch(signInIntent)
                    } catch (e: Exception) {
                        coroutineScope.launch {
                            val result = viewModel.repository.googleAuthManager.signInWithGoogle()
                            result.onSuccess { user ->
                                viewModel.loginUser(user)
                                viewModel.navigateTo(ScreenDestination.Feed)
                            }.onFailure {
                                Toast.makeText(context, "Google Sign-In: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF242A38),
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", color = Color(0xFF4285F4), fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google (गूगल लॉगिन)",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f), color = HundredGramDivider)
                Text(
                    text = " OR LOGIN WITH ",
                    color = HundredGramTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Divider(modifier = Modifier.weight(1f), color = HundredGramDivider)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Switcher: Email / Phone
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(HundredGramCardBackground)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { authMode = "EMAIL" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (authMode == "EMAIL") HundredGramPink else Color.Transparent,
                        contentColor = if (authMode == "EMAIL") Color.White else HundredGramTextSecondary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Email / Username", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { authMode = "PHONE" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (authMode == "PHONE") HundredGramPink else Color.Transparent,
                        contentColor = if (authMode == "PHONE") Color.White else HundredGramTextSecondary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Phone & OTP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (authMode == "EMAIL") {
                OutlinedTextField(
                    value = usernameOrEmail,
                    onValueChange = { usernameOrEmail = it },
                    label = { Text("Username or Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = HundredGramTextSecondary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HundredGramTextPrimary,
                        unfocusedTextColor = HundredGramTextPrimary,
                        focusedBorderColor = HundredGramPink,
                        unfocusedBorderColor = HundredGramDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = HundredGramTextSecondary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HundredGramTextPrimary,
                        unfocusedTextColor = HundredGramTextPrimary,
                        focusedBorderColor = HundredGramPink,
                        unfocusedBorderColor = HundredGramDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val cleanInput = usernameOrEmail.trim()
                        if (cleanInput.isBlank()) {
                            Toast.makeText(context, "Please enter your username or email", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val userId = "u_${cleanInput.lowercase().replace(" ", "_").replace("@", "_")}"
                        val newUser = com.example.data.UserData(
                            userId = userId,
                            username = cleanInput.substringBefore("@").ifBlank { "creator_${userId.takeLast(4)}" },
                            displayName = cleanInput.substringBefore("@").replace("_", " ").capitalize(),
                            bio = if (isSignUp) "HundredGram Creator ✨" else "HundredGram Member 📸",
                            avatarUrl = "",
                            followersCount = 0,
                            followingCount = 0,
                            postsCount = 0,
                            isVerified = true
                        )
                        viewModel.loginUser(newUser)
                        viewModel.navigateTo(ScreenDestination.Feed)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(if (isSignUp) "Sign Up" else "Log In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            } else {
                // PHONE & OTP FLOW
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Mobile Number (10 Digits)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = HundredGramTextSecondary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HundredGramTextPrimary,
                        unfocusedTextColor = HundredGramTextPrimary,
                        focusedBorderColor = HundredGramPink,
                        unfocusedBorderColor = HundredGramDivider
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (phoneNumber.trim().length < 8) {
                                Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isLoading = true
                            coroutineScope.launch {
                                val res = viewModel.repository.phoneAuthManager.requestOtp(phoneNumber)
                                res.onSuccess { otp ->
                                    isOtpSent = true
                                    latestOtpReceived = otp
                                    otpCode = otp
                                    otpCountdown = 60
                                    Toast.makeText(context, "OTP Sent: $otp", Toast.LENGTH_LONG).show()
                                }.onFailure { err ->
                                    Toast.makeText(context, "Error sending OTP: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                                isLoading = false
                            }
                        },
                        enabled = otpCountdown == 0 && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            if (otpCountdown > 0) "Resend in ${otpCountdown}s" else if (isOtpSent) "Resend OTP" else "Send OTP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isOtpSent) {
                        Text(
                            text = "Code sent via SMS",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                AnimatedVisibility(visible = isOtpSent) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        if (latestOtpReceived.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "✓ Security OTP Generated",
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            "Code: $latestOtpReceived (Auto-filled)",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Button(
                                        onClick = { otpCode = latestOtpReceived },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Auto-fill", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it.take(6) },
                            label = { Text("Enter 6-digit OTP Code") },
                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = HundredGramTextSecondary) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = HundredGramTextPrimary,
                                unfocusedTextColor = HundredGramTextPrimary,
                                focusedBorderColor = HundredGramPink,
                                unfocusedBorderColor = HundredGramDivider
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (otpCode.length != 6) {
                                    Toast.makeText(context, "Please enter complete 6-digit OTP", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isLoading = true
                                coroutineScope.launch {
                                    val result = viewModel.repository.phoneAuthManager.verifyPhoneNumberAndSignIn(phoneNumber, otpCode)
                                    result.onSuccess { user ->
                                        viewModel.loginUser(user)
                                        Toast.makeText(context, "Phone verified! Welcome ${user.displayName}", Toast.LENGTH_SHORT).show()
                                        viewModel.navigateTo(ScreenDestination.Feed)
                                    }.onFailure { error ->
                                        Toast.makeText(context, "Verification Failed: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                    isLoading = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HundredGramPink),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Verify OTP & Continue", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { isSignUp = !isSignUp }) {
                Text(
                    text = if (isSignUp) "Already have an account? Log In" else "Don't have an account? Sign Up",
                    color = HundredGramPink,
                    fontWeight = FontWeight.Medium
                )
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(10.dp))
                CircularProgressIndicator(color = HundredGramPink, modifier = Modifier.size(28.dp))
            }
        }
    }

    // 📘 Facebook Login Dialog
    if (showFacebookDialog) {
        AlertDialog(
            onDismissRequest = { showFacebookDialog = false },
            title = {
                Text("📘 Log in with Facebook", color = HundredGramTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "HundredGram will connect with Facebook to verify your identity and import your profile name securely.",
                        color = HundredGramTextSecondary,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = fbNameInput,
                        onValueChange = { fbNameInput = it },
                        label = { Text("Your Facebook Name / Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fbEmailInput,
                        onValueChange = { fbEmailInput = it },
                        label = { Text("Facebook Email / Phone (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFacebookDialog = false
                        isLoading = true
                        coroutineScope.launch {
                            val result = viewModel.repository.facebookAuthManager.signInWithFacebook(
                                userName = fbNameInput.ifBlank { "Facebook User" },
                                userEmail = fbEmailInput.ifBlank { null }
                            )
                            result.onSuccess { user ->
                                viewModel.loginUser(user)
                                Toast.makeText(context, "Connected with Facebook! Welcome ${user.displayName}", Toast.LENGTH_SHORT).show()
                                viewModel.navigateTo(ScreenDestination.Feed)
                            }
                            isLoading = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                ) {
                    Text("Confirm Facebook Login", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFacebookDialog = false }) {
                    Text("Cancel", color = HundredGramTextSecondary)
                }
            },
            containerColor = HundredGramCardBackground
        )
    }
}
