package com.rikhotso.studytime.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/** The few places where the UI has to talk to Android directly. */

/** Android 13 and newer ask the user before an app may post notifications. */
fun needsNotificationPermission(): Boolean = Build.VERSION.SDK_INT >= 33

const val NOTIFICATION_PERMISSION = "android.permission.POST_NOTIFICATIONS"

/** Stops the screen from dimming while [enabled] is true, for example during a focus round. */
@Composable
fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

// ------------------------------------------------------------------ profile photo

private const val PHOTO_SIZE = 512

/**
 * Copies the picked photo into the app's private storage as a small square-ish JPEG and returns
 * its path, or null when the photo could not be read. The original is never changed.
 */
private fun copyProfilePhoto(context: Context, uri: Uri): String? = try {
    val resolver = context.contentResolver
    // First pass: read only the size, so a huge photo is never loaded at full size.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= PHOTO_SIZE) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    if (decoded == null) {
        null
    } else {
        val longest = max(decoded.width, decoded.height).coerceAtLeast(1)
        val scale = minOf(1f, PHOTO_SIZE.toFloat() / longest)
        val scaled = Bitmap.createScaledBitmap(decoded, max(1, (decoded.width * scale).toInt()), max(1, (decoded.height * scale).toInt()), true)
        // Phone cameras often save the picture sideways and note the rotation separately.
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val upright = if (degrees == 0f) scaled else Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, Matrix().apply { postRotate(degrees) }, true)
        val file = File(context.filesDir, "profile_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { upright.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        file.absolutePath
    }
} catch (e: Throwable) {
    // Covers unreadable files and photos too large for the phone's memory.
    null
}

/** Returns a function that opens the system photo picker. No storage permission is needed for it. */
@Composable
fun rememberProfilePhotoPicker(onSaved: (String) -> Unit, onFailed: () -> Unit): () -> Unit {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val saved by rememberUpdatedState(onSaved)
    val failed by rememberUpdatedState(onFailed)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val path = withContext(Dispatchers.IO) { copyProfilePhoto(context, uri) }
                if (path != null) saved(path) else failed()
            }
        }
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

/** Loads the saved profile photo, or gives null while loading, when there is none or when it is gone. */
@Composable
fun rememberProfilePhoto(path: String): ImageBitmap? {
    val photo by produceState<ImageBitmap?>(initialValue = null, path) {
        value = if (path.isEmpty()) null else withContext(Dispatchers.IO) {
            try {
                BitmapFactory.decodeFile(path)?.asImageBitmap()
            } catch (e: Throwable) {
                null
            }
        }
    }
    return photo
}

// ------------------------------------------------------------------ export and import

/**
 * Returns a function that asks where to save a file called [suggestedName] and writes [text] to
 * it. The system file picker is used, so the user chooses the folder and no permission is needed.
 */
@Composable
fun rememberTextExporter(mimeType: String, text: () -> String, onDone: (Boolean) -> Unit): (suggestedName: String) -> Unit {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val content by rememberUpdatedState(text)
    val done by rememberUpdatedState(onDone)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mimeType)) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        val stream = context.contentResolver.openOutputStream(uri)
                        stream?.use { it.write(content().toByteArray(Charsets.UTF_8)) }
                        stream != null
                    } catch (e: Exception) {
                        false
                    }
                }
                done(ok)
            }
        }
    }
    return { name -> launcher.launch(name) }
}

private const val MAX_IMPORT_BYTES = 8 * 1024 * 1024

/** Returns a function that lets the user pick a file and hands its text to [onText], or null when it could not be read. */
@Composable
fun rememberTextImporter(onText: (String?) -> Unit): () -> Unit {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val deliver by rememberUpdatedState(onText)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val collected = ByteArrayOutputStream()
                            val buffer = ByteArray(16 * 1024)
                            var read = stream.read(buffer)
                            while (read >= 0 && collected.size() <= MAX_IMPORT_BYTES) {
                                collected.write(buffer, 0, read)
                                read = stream.read(buffer)
                            }
                            if (collected.size() > MAX_IMPORT_BYTES) null else String(collected.toByteArray(), Charsets.UTF_8)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                deliver(text)
            }
        }
    }
    // Some file apps label a .json file as plain text or as unknown, so every type is allowed.
    return { launcher.launch(arrayOf("*/*")) }
}
