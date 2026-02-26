package com.hyun.robot.ui

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ficat.easyble.BleDevice
import com.ficat.easyble.gatt.callback.BleConnectCallback
import com.hyun.robot.MyApplication
import com.hyun.robot.R
import com.hyun.robot.utils.BaseBlueManager
import com.hyun.robot.utils.BleCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private var connectCallback: BleConnectCallback? = null
private var baseHeight: Byte = BleCommand.BASE_HEIGHT
private var roll: Byte = BleCommand.EMPTY_BYTE
private var linearVel: Byte = BleCommand.EMPTY_BYTE
private var angularVel: Byte = BleCommand.EMPTY_BYTE
private var leftJoyY: Byte = BleCommand.EMPTY_BYTE
private var rightJoyY: Byte = BleCommand.EMPTY_BYTE
private var stableStatus: Byte = BleCommand.BYTE_01
private var isToggleOn = false
private var showDialog = false
private var showLoading = true
private var bDevice: BluetoothDevice? = null
private var connectCount = 0
private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
private var isReady = false

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDetailScreen(content: DeviceDetailActivity) {
    var showSplash by remember { mutableStateOf(true) }
    val image = painterResource(R.drawable.bg_detail)
    var gotoHome by remember { mutableStateOf(false) }
    var deviceName by remember { mutableStateOf(content.deviceName ?: "") }
    var loadingText by remember { mutableStateOf("Connecting") }
    var isConnectDevice by remember { mutableStateOf(false) }
    var isToggleReady by remember { mutableStateOf(false) }

    connectCallback = object : BleConnectCallback {
        override fun onFailure(failureCode: Int, info: String?, device: BleDevice?) {
            showLoading = false
            MyApplication.bleManager.connectDev = null
            connectCount++
            showSplash = false
            isConnectDevice = false
            isToggleOn = false
        }

        override fun onStart(startSuccess: Boolean, info: String?, device: BleDevice?) {
            loadingText = "Connecting"
            showSplash = true
        }

        override fun onConnected(device: BleDevice?) {
            showLoading = false
            if (device != null) {
                isConnectDevice = true
                isToggleOn = true
                bDevice = device.bluetoothDevice
                MyApplication.bleManager.connectingDevice = bDevice
                MyApplication.bleManager.connectDev = device
                MyApplication.bleManager.connectedDevices.remove(device)
                MyApplication.bleManager.connectedDevices.add(device)
            }
            showSplash = false
            connectCount = 0
            leftJoyY = 0x00
            rightJoyY = 0x00
            sendBleJoyXYData()
            showToast(content, "Connected")
        }

        override fun onDisconnected(info: String?, status: Int, device: BleDevice?) {
            isConnectDevice = false
            isToggleOn = false
            showToast(content, "Disconnected")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    navigationIcon = {
                        IconButton(
                            modifier = Modifier.size(40.dp),
                            onClick = { gotoHome = true },
                        ) {
                            Icon(
                                modifier = Modifier.size(40.dp),
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "BACK"
                            )
                        }
                    },
                    title = {
                        Row(horizontalArrangement = Arrangement.Start) {
                            Text(
                                modifier = Modifier.padding(top = 10.dp),
                                text = deviceName,
                                color = Color.Black,
                                fontSize = 18.sp,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Box(modifier = Modifier.padding(start = 40.dp, end = 8.dp)) {
                                Text(
                                    fontSize = 14.sp,
                                    text = "ROBOT GO",
                                    color = Color.Black,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            Switch(checked = isToggleReady, onCheckedChange = { status ->
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        isToggleReady = status
                                        isReady = status
                                        if (isToggleOn) {
                                            sendBleSettingData()
                                        } else {
                                            isToggleReady = false
                                            isReady = false
                                            showToast(content, "Please Connect 'Navbot_en01' first.")
                                        }
                                    }
                                }
                            }, colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF007AFF),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.Gray,
                            ))
                        }
                    },
                    actions = {
                        Row(
                            modifier = Modifier.size(40.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            isToggleOn = !isToggleOn
                                            if (isToggleOn) {
                                                toConnectDevice(content)
                                            } else {
                                                MyApplication.bleManager.disconnectDevice(content.deviceAddress)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.size(40.dp).padding(end = 10.dp),
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (isConnectDevice) R.drawable.ic_close
                                        else R.drawable.ic_open
                                    ),
                                    contentDescription = "disconnect",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Left Tank Joystick
                    Box(modifier = Modifier.weight(4f).padding(16.dp)) {
                        Joystick(
                            context = content,
                            onValueChange = { offset ->
                                if (isToggleOn && bDevice != null) {
                                    leftJoyY = ((-offset.y).coerceIn(-100f, 100f)).toInt().toByte()
                                    sendBleJoyXYData()
                                }
                            },
                            onRelease = {
                                leftJoyY = 0x00
                                sendBleJoyXYData()
                            }
                        )
                    }

                    // Middle Sliders
                    Row(modifier = Modifier.weight(10f)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
                            VerticalSliderView(content, "Base Height", "mm", 0.0f, 53, 32, true)
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
                            VerticalSliderView(content, "roll", "°", 0.5f, 60, -30, false)
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
                            VerticalSliderView(content, "Linear Vel", "mm/s", 0.5f, 400, -200, false)
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
                            VerticalSliderView(content, "Angular Vel", "°/s", 0.5f, 200, -100, false)
                        }
                    }

                    // Reset Button
                    Box(modifier = Modifier.padding(bottom = 40.dp).weight(3f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        IconButton(
                            onClick = {
                                bDevice = MyApplication.bleManager.connectingDevice
                                if (!isReady) {
                                    showToast(content, "Please turn on the ‘ROBOT GO’ button first.")
                                } else if (isToggleOn && bDevice != null) {
                                    Handler(Looper.getMainLooper()).postDelayed({ sendBleJumpData() }, 100)
                                } else if (!isToggleOn) {
                                    toConnectDevice(content)
                                }
                            },
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_reset),
                                contentDescription = "Reset",
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }

                    // Right Tank Joystick
                    Box(modifier = Modifier.weight(4f).padding(16.dp)) {
                        Joystick(
                            context = content,
                            onValueChange = { offset ->
                                if (isToggleOn && bDevice != null) {
                                    rightJoyY = ((-offset.y).coerceIn(-100f, 100f)).toInt().toByte()
                                    sendBleJoyXYData()
                                }
                            },
                            onRelease = {
                                rightJoyY = 0x00
                                sendBleJoyXYData()
                            }
                        )
                    }
                }
            }
        }
        if (gotoHome) content.BackToHome()
        if (showDialog) content.WifiListDialog(onDismiss = { showDialog = false }, onConnect = {})
    }
    if (showSplash) {
        if (MyApplication.bleManager.connectDev?.isConnected != true) {
            content.LoadingConnectingScreen(isConnecting = showSplash, loadingText = loadingText)
            toConnectDevice(content)
        } else {
            bDevice = MyApplication.bleManager.connectDev!!.bluetoothDevice
            showLoading = false
            isToggleOn = true
            isConnectDevice = true
        }
    }
}

private fun sendBleJoyXYData() {
    MyApplication.bleManager.sendDataToDevice(
        device = bDevice!!,
        dataToSend = BleCommand.getBleCommand(
            height = baseHeight,
            roll = roll,
            linearH = linearVel,
            linearW = linearVel,
            angular = angularVel,
            stable = stableStatus,
            joyY = leftJoyY,   // Tank Drive: Left Track
            joyX = rightJoyY   // Tank Drive: Right Track
        )
    )
}

fun sendBleSettingData() {
    MyApplication.bleManager.sendDataToDevice(
        device = bDevice!!,
        dataToSend = BleCommand.getBleCommand(
            height = baseHeight,
            roll = roll,
            linearH = linearVel,
            linearW = linearVel,
            angular = angularVel,
            stable = stableStatus,
        )
    )
}

fun sendBleJumpData() {
    MyApplication.bleManager.sendDataToDevice(
        device = bDevice!!,
        dataToSend = BleCommand.getBleCommand(
            height = baseHeight,
            roll = roll,
            linearH = linearVel,
            linearW = linearVel,
            angular = angularVel,
            stable = stableStatus,
            dir = 0x01
        )
    )
}

private fun toConnectDevice(activity: DeviceDetailActivity) {
    scope.launch {
        withContext(Dispatchers.IO) {
            MyApplication.bleManager.connectToDevice(activity.deviceAddress, connectCallback!!)
        }
    }
}

private fun showToast(context: Context, message: String) {
    CoroutineScope(Dispatchers.IO).launch {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}

class JoystickFilter(private val alpha: Float = 0.3f) {
    private var filteredX = 0f
    private var filteredY = 0f
    fun filter(input: Offset): Offset {
        filteredX = alpha * input.x + (1 - alpha) * filteredX
        filteredY = alpha * input.y + (1 - alpha) * filteredY
        return Offset(filteredX, filteredY)
    }
}

@Composable
fun PermissionHandler(onPermissionsGranted: () -> Unit, onPermissionsDenied: () -> Unit) {
    val context = LocalContext.current
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) onPermissionsGranted() else onPermissionsDenied()
    }

    val requiredPermissions = remember {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
            else -> arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }
    }
    val currentActivity = LocalContext.current as Activity
    var showRationale by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        when {
            missingPermissions.isEmpty() -> onPermissionsGranted()
            missingPermissions.any { perm -> ActivityCompat.shouldShowRequestPermissionRationale(currentActivity, perm) } -> showRationale = true
            else -> permissionsLauncher.launch(requiredPermissions)
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Bluetooth permission is required") },
            text = { Text("This feature requires Bluetooth permission to discover nearby devices") },
            confirmButton = {
                TextButton(onClick = {
                    permissionsLauncher.launch(requiredPermissions)
                    showRationale = false
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) { Text("Cancel") }
            }
        )
    }

    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter
    if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        currentActivity.startActivityForResult(enableBtIntent, BaseBlueManager.REQUEST_ENABLE_BT)
    }
}

@Composable
fun VerticalSlider(context: Context, maxHeight: Dp = 200.dp, initialProgress: Float = 0.5f, onProgressChanged: (Float) -> Unit, canDrag: Boolean) {
    val cornerRadius = 30.dp
    val maxHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
    val cornerRadiusPx = with(LocalDensity.current) { cornerRadius.toPx() }
    var progressHeight by remember { mutableFloatStateOf(initialProgress.coerceIn(0f, 1f) * maxHeightPx) }
    val animatedHeight by animateFloatAsState(targetValue = progressHeight, label = "")

    Box(
        modifier = Modifier
            .size(width = 30.dp, height = maxHeight)
            .background(Color(0xFFE1E9F0), RoundedCornerShape(cornerRadius))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (!isReady) {
                        showToast(context, "Please turn on the ‘ROBOT GO’ button first.")
                    } else if (canDrag) {
                        val newHeight = (progressHeight - dragAmount).coerceIn(0f, maxHeightPx)
                        progressHeight = newHeight
                        onProgressChanged(newHeight / maxHeightPx)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color(0xFF007AFF),
                topLeft = Offset(0f, size.height - animatedHeight),
                size = Size(size.width, animatedHeight),
                cornerRadius = CornerRadius(cornerRadiusPx)
            )
        }
    }
}

@Composable
fun VerticalSliderView(context: Context, verticalName: String, verticalUnit: String, baseVolume: Float, verticalPlusValue: Int, verticalAddValue: Int, canDrag: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        var volume by remember { mutableFloatStateOf(baseVolume) }
        VerticalSlider(
            context = context,
            initialProgress = volume,
            onProgressChanged = {
                if (isReady) {
                    volume = it
                    baseHeight = BleCommand.intToUnsignedByte((volume * verticalPlusValue + verticalAddValue).toInt())
                    if (isToggleOn) {
                        if (showLoading) toConnectDevice(context as DeviceDetailActivity)
                        else {
                            bDevice = MyApplication.bleManager.connectingDevice
                            sendBleSettingData()
                        }
                    } else toConnectDevice(context as DeviceDetailActivity)
                } else showToast(context, "Please turn on the ‘ROBOT GO’ button first.")
            },
            canDrag = canDrag
        )
        Text(text = "${(volume * verticalPlusValue + verticalAddValue).toInt()}${verticalUnit}", textAlign = TextAlign.Center, fontSize = 8.sp, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        Text(text = verticalName, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
    }
}

fun Offset.applyDeadZone(threshold: Float = 2f): Offset {
    val distance = getDistance()
    return when {
        distance < threshold -> Offset.Zero
        distance < threshold * 2 -> this * 0.5f
        else -> this
    }
}

@Composable
fun Joystick(
    context: Context,
    modifier: Modifier = Modifier,
    joystickSize: Dp = 130.dp,
    onValueChange: (Offset) -> Unit,
    onRelease: () -> Unit
) {
    val radius = joystickSize / 2
    val center = remember { Offset(0.5f, 0.5f) }
    var buttonOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    val kalmanFilter = remember { BaseActivity.KalmanFilterProcessor() }
    val filter = remember { JoystickFilter() }

    Box(
        modifier = modifier
            .size(joystickSize)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { _, dragAmount ->
                        if (!isReady) {
                            showToast(context, "Please turn on the ‘ROBOT GO’ button first.")
                        } else {
                            val newOffset = Offset(
                                x = (buttonOffset.x + dragAmount.x).coerceIn(-radius.toPx(), radius.toPx()),
                                y = (buttonOffset.y + dragAmount.y).coerceIn(-radius.toPx(), radius.toPx())
                            )
                            buttonOffset = newOffset
                            val normalizedX = (buttonOffset.x / radius.toPx()).coerceIn(-1f..1f) * 100
                            val normalizedY = (buttonOffset.y / radius.toPx()).coerceIn(-1f..1f) * 100
                            
                            val filteredRaw = kalmanFilter.process(Offset(normalizedX, normalizedY))
                            val processed = filteredRaw.applyDeadZone(5f).let { filter.filter(it) }
                            onValueChange(processed)
                        }
                    },
                    onDragEnd = {
                        buttonOffset = Offset(0f, 0f)
                        onRelease()
                    }
                )
            }
            .border(Dp(2f), Color(0xFF007AFF), CircleShape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size
            val absoluteCenter = Offset(x = canvasSize.width * center.x, y = canvasSize.height * center.y)
            val finalPosition = absoluteCenter + buttonOffset
            drawCircle(color = Color(0xFF007AFF), radius = radius.toPx() * 0.4f, center = finalPosition)
        }
    }
}
