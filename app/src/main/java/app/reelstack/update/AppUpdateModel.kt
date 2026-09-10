package app.reelstack.update

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.reelstack.BuildConfig
import app.reelstack.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal data class UpdateState(val release: AppRelease? = null, val checking: Boolean = false,
    val downloading: Boolean = false, val progress: Float = 0f, val ready: Boolean = false,
    val message: Int? = null, val open: Boolean = false, val banner: Boolean = false,
    val automatic: Boolean = true, val previews: Boolean = false)

internal class AppUpdateModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val prefs = app.getSharedPreferences("spole_updates", 0)
    private val folder = File(app.cacheDir, "updates").apply { mkdirs() }
    private val apk = File(folder, "spole-update.apk")
    private var job: Job? = null
    private val mutable = MutableStateFlow(UpdateState(
        release = prefs.getString("release", null)?.let(AppRelease::decode)?.takeIf {
            val installed = ReleaseVersion.parse(BuildConfig.VERSION_NAME)
            installed != null && (ReleaseVersion.parse(it.tag)?.let { v -> v > installed } == true)
        }, automatic = prefs.getBoolean("automatic", !BuildConfig.DEBUG),
        previews = prefs.getBoolean("previews", BuildConfig.VERSION_NAME.contains("-alpha") || BuildConfig.VERSION_NAME.contains("-beta"))))
    val state = mutable.asStateFlow()
    init {
        val release = mutable.value.release
        mutable.update { it.copy(banner = release != null && prefs.getString("dismissed", null) != release.tag) }
        if (release != null && apk.exists()) viewModelScope.launch {
            val valid = withContext(Dispatchers.IO) { runCatching { verify(apk, release) }.isSuccess }
            mutable.update { it.copy(ready = valid && it.release == release) }
        }
    }
    fun open() { mutable.update { it.copy(open = true) } }
    fun close() { mutable.update { it.copy(open = false) } }
    fun later() {
        prefs.edit { putString("dismissed", mutable.value.release?.tag) }
        mutable.update { it.copy(banner = false, open = false) }
    }
    fun automatic(value: Boolean) { prefs.edit { putBoolean("automatic", value) }; mutable.update { it.copy(automatic = value) } }
    fun previews(value: Boolean) {
        cancel()
        prefs.edit { putBoolean("previews", value); remove("release"); remove("last_check") }
        mutable.update { it.copy(previews = value, release = null, ready = false, banner = false) }
        check(true)
    }
    fun check(manual: Boolean = false) {
        if (job?.isActive == true) return
        if (!manual && (!mutable.value.automatic || System.currentTimeMillis() - prefs.getLong("last_check", 0) < 12 * 60 * 60 * 1000L)) return
        val previous = job
        job = viewModelScope.launch {
            previous?.join()
            prefs.edit { putLong("last_check", System.currentTimeMillis()) }
            mutable.update { it.copy(checking = true, message = null) }
            try {
                val release = withContext(Dispatchers.IO) {
                    val connection = connect(RELEASE_API, "application/vnd.github+json")
                    try {
                        val bytes = connection.inputStream.use { it.readBytesBounded(4 * 1024 * 1024) }
                        newerRelease(bytes.toString(Charsets.UTF_8), BuildConfig.VERSION_NAME, mutable.value.previews)
                    } finally { connection.disconnect() }
                }
                ensureActive()
                prefs.edit { putLong("last_check", System.currentTimeMillis()); putString("release", release?.encode()) }
                mutable.update { it.copy(checking = false, release = release, ready = it.ready && it.release == release,
                    banner = release != null && prefs.getString("dismissed", null) != release.tag,
                    message = if (release == null) R.string.update_current else null) }
            } catch (e: CancellationException) { throw e
            } catch (e: UpdateFailure) { mutable.update { it.copy(checking = false, message = e.label) }
            } catch (_: Exception) { mutable.update { it.copy(checking = false, message = R.string.update_network_error) } }
        }
    }
    fun cancel() { job?.cancel(); mutable.update { it.copy(checking = false, downloading = false, progress = 0f) } }
    fun download() {
        val release = mutable.value.release ?: return
        if (job?.isActive == true) return
        val previous = job
        job = viewModelScope.launch {
            previous?.join()
            mutable.update { it.copy(downloading = true, ready = false, progress = 0f, message = null) }
            try {
                withContext(Dispatchers.IO) {
                    val part = File(folder, "download.part")
                    try {
                        if (folder.usableSpace < release.size + 4 * 1024 * 1024) throw UpdateFailure(R.string.update_storage_error)
                        val connection = connect("https://api.github.com/repos/$RELEASE_REPO/releases/assets/${release.assetId}", "application/octet-stream")
                        try {
                            connection.inputStream.use { input -> part.outputStream().use { output ->
                                val buffer = ByteArray(64 * 1024); var total = 0L; var lastPercent = -1
                                while (true) {
                                    ensureActive()
                                    val count = input.read(buffer); if (count < 0) break
                                    total += count
                                    if (total > release.size || total > MAX_APK_BYTES) throw UpdateFailure(R.string.update_invalid)
                                    output.write(buffer, 0, count)
                                    val percent = (100 * total / release.size).toInt()
                                    if (percent != lastPercent) { lastPercent = percent; mutable.update { it.copy(progress = total.toFloat() / release.size) } }
                                }
                            } }
                        } finally { connection.disconnect() }
                        verify(part, release)
                        ensureActive()
                        if (apk.exists() && !apk.delete()) throw UpdateFailure(R.string.update_storage_error)
                        if (!part.renameTo(apk)) throw UpdateFailure(R.string.update_storage_error)
                    } finally { part.delete() }
                }
                mutable.update { it.copy(downloading = false, ready = true, progress = 1f) }
            } catch (e: CancellationException) { throw e
            } catch (e: UpdateFailure) { mutable.update { it.copy(downloading = false, message = e.label) }
            } catch (_: Exception) { mutable.update { it.copy(downloading = false, message = R.string.update_network_error) } }
        }
    }
    fun install() {
        val release = mutable.value.release ?: return
        if (!mutable.value.ready || job?.isActive == true) return
        job = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { verify(apk, release) }
                if (!app.packageManager.canRequestPackageInstalls()) {
                    mutable.update { it.copy(message = R.string.update_permission) }
                    app.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${app.packageName}".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } else {
                    val uri = FileProvider.getUriForFile(app, "${app.packageName}.updates", apk)
                    app.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { mutable.update { it.copy(message = R.string.update_install_error) } }
        }
    }
    @Suppress("DEPRECATION")
    private fun verify(file: File, release: AppRelease) {
        if (file.length() != release.size) throw UpdateFailure(R.string.update_invalid)
        val sha = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val buffer = ByteArray(65536); while (true) { val n = input.read(buffer); if (n < 0) break; sha.update(buffer, 0, n) } }
        if (sha.digest().joinToString("") { "%02x".format(it) } != release.digest) throw UpdateFailure(R.string.update_invalid)
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val installed = app.packageManager.getPackageInfo(app.packageName, flags)
        val candidate = app.packageManager.getPackageArchiveInfo(file.path, flags) ?: throw UpdateFailure(R.string.update_invalid)
        fun identity(info: android.content.pm.PackageInfo): UpdatePackageIdentity = UpdatePackageIdentity(
            info.packageName, if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong(),
            info.versionName.orEmpty(), info.applicationInfo?.minSdkVersion ?: Int.MAX_VALUE,
            (info.applicationInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0,
            (if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures)
                .orEmpty().map { it.toCharsString() }.toSet())
        if (!compatibleUpdate(identity(installed), identity(candidate), release.tag, Build.VERSION.SDK_INT)) throw UpdateFailure(R.string.update_invalid)
    }
    private fun connect(initial: String, accept: String): HttpURLConnection {
        var target = initial
        repeat(5) {
            if (!trustedUpdateUrl(target)) throw UpdateFailure(R.string.update_invalid)
            val url = URL(target)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false; connectTimeout = 15000; readTimeout = 20000
                setRequestProperty("Accept", accept); setRequestProperty("User-Agent", "Spole/${BuildConfig.VERSION_NAME}")
                // Public releases only. No media credentials or cookies ever enter this client.
            }
            val code = connection.responseCode
            if (code in listOf(301, 302, 303, 307, 308)) {
                val next = connection.getHeaderField("Location"); connection.disconnect()
                target = next?.let { URL(url, it).toString() } ?: throw UpdateFailure(R.string.update_invalid)
            } else if (code == 200) return connection
            else { connection.disconnect(); throw UpdateFailure(when(code) {
                401, 404 -> R.string.update_access_error; 403, 429 -> R.string.update_rate_error; else -> R.string.update_network_error
            }) }
        }
        throw UpdateFailure(R.string.update_invalid)
    }
}
private class UpdateFailure(val label: Int) : Exception()
private fun java.io.InputStream.readBytesBounded(max: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
    while (true) { val n = read(buffer); if (n < 0) break; if (output.size() + n > max) throw UpdateFailure(R.string.update_invalid); output.write(buffer, 0, n) }
    return output.toByteArray()
}
