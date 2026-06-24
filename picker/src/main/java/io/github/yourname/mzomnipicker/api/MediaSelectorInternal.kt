package io.github.yourname.mzomnipicker.api

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import io.github.yourname.mzomnipicker.R
import io.github.yourname.mzomnipicker.camera.CameraHelper
import io.github.yourname.mzomnipicker.compress.CompressCallback
import io.github.yourname.mzomnipicker.compress.IImageCompressor
import io.github.yourname.mzomnipicker.compress.IVideoCompressor
import io.github.yourname.mzomnipicker.data.MediaRepository
import io.github.yourname.mzomnipicker.loader.IImageEngine
import io.github.yourname.mzomnipicker.model.MediaEntity
import io.github.yourname.mzomnipicker.model.MediaFilter
import io.github.yourname.mzomnipicker.model.MediaType
import io.github.yourname.mzomnipicker.preview.IOtherPreviewProvider
import io.github.yourname.mzomnipicker.ui.CropImageActivity
import io.github.yourname.mzomnipicker.ui.LoadingDialog
import io.github.yourname.mzomnipicker.ui.MediaPickerActivity
import io.github.yourname.mzomnipicker.ui.PermissionHelper
import io.github.yourname.mzomnipicker.util.StorageAccess
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

internal object MediaSelectorInternal {

    const val MAX_SYSTEM_PICKER_ITEMS = 100
    val sysPickerPool = Executors.newSingleThreadExecutor()

    @Volatile
    var pendingConfig: SelectionConfig? = null

    @Volatile
    var pendingListener: OnPickResultListener? = null

    @Volatile
    var globalEngine: IImageEngine? = null

    @Volatile
    var activeEngine: IImageEngine? = null

    @Volatile
    var globalImageCompressor: IImageCompressor? = null

    @Volatile
    var globalVideoCompressor: IVideoCompressor? = null

    @Volatile
    var activeImageCompressor: IImageCompressor? = null

    @Volatile
    var activeVideoCompressor: IVideoCompressor? = null

    @Volatile
    var globalOtherPreviewProvider: IOtherPreviewProvider? = null

    @Volatile
    var globalTheme: PickerTheme? = null

    private data class CacheKey(
        val type: MediaType,
        val allFilesAccess: Boolean,
    )

    private val preloadCache = ConcurrentHashMap<CacheKey, List<MediaEntity>>()

    fun preload(context: Context, types: Array<out MediaType>, pageSize: Int) {
        val app = context.applicationContext
        types.forEach { t ->
            if (!PermissionHelper.anyUsable(app, t)) return@forEach
            val key = cacheKey(t)
            MediaRepository.queryAsync(
                app, MediaFilter.of(t), offset = 0, limit = pageSize
            ) { list ->
                if (list.isNotEmpty()) preloadCache[key] = list
            }
        }
    }

    fun cached(type: MediaType): List<MediaEntity>? =
        preloadCache[cacheKey(type)]?.takeIf { it.isNotEmpty() }

    fun putCache(type: MediaType, list: List<MediaEntity>) {
        if (list.isNotEmpty()) preloadCache[cacheKey(type)] = list
    }

    fun invalidateCache() {
        preloadCache.clear()
        MediaRepository.invalidateFileScanCache()
    }

    private fun cacheKey(type: MediaType): CacheKey =
        CacheKey(type, StorageAccess.hasAllFilesAccess())

    fun launchCameraPicker(
        activity: ComponentActivity,
        cfg: SelectionConfig,
        listener: OnPickResultListener,
    ) {
        pendingListener = listener
        pendingConfig = cfg
        CameraHelper.take(activity) { ok, path, uri ->
            if (!ok || path == null || uri == null) {
                clearRuntimeState()
                listener.onResult(emptyList())
                return@take
            }

            invalidateCache()
            val item = CameraHelper.makeEntity(path, uri)
            if (cfg.needsImageProcessing) {
                launchCameraCrop(activity, item, cfg, listener)
            } else {
                deliverCameraResult(activity, listOf(item), listener)
            }
        }
    }

    fun launchVideoCameraPicker(
        activity: ComponentActivity,
        cfg: SelectionConfig,
        listener: OnPickResultListener,
    ) {
        pendingListener = listener
        pendingConfig = cfg
        CameraHelper.record(
            activity,
            cfg.cameraRecordDurationMs,
            cfg.cameraRecordCountDown,
            cfg.cameraRecordTrigger,
        ) { ok, path, uri, mirrorHorizontal ->
            if (!ok || path == null || uri == null) {
                clearRuntimeState()
                listener.onResult(emptyList())
                return@record
            }

            invalidateCache()
            val item = CameraHelper.makeVideoEntity(path, uri, mirrorHorizontal)
            deliverCameraResult(activity, listOf(item), listener)
        }
    }

    private fun launchCameraCrop(
        activity: ComponentActivity,
        item: MediaEntity,
        cfg: SelectionConfig,
        listener: OnPickResultListener,
    ) {
        val processor = when {
            cfg.imageEditEnabled -> cfg.imageEditProcessor
            cfg.cropConfig.enabled -> cfg.imageCropProcessor
            else -> null
        }
        if (processor != null) {
            try {
                processor.process(
                    activity = activity,
                    items = listOf(item),
                    cropConfig = cfg.cropConfig,
                    callback = object : ImageProcessCallback {
                        override fun onSuccess(result: List<MediaEntity>) {
                            activity.runOnUiThread {
                                if (result.isEmpty()) {
                                    clearRuntimeState()
                                    listener.onResult(emptyList())
                                } else {
                                    deliverCameraResult(activity, result, listener)
                                }
                            }
                        }

                        override fun onCancel() {
                            activity.runOnUiThread {
                                clearRuntimeState()
                                listener.onResult(emptyList())
                            }
                        }

                        override fun onError(error: Throwable) {
                            onCancel()
                        }
                    },
                )
            } catch (_: Throwable) {
                activity.runOnUiThread {
                    clearRuntimeState()
                    listener.onResult(emptyList())
                }
            }
            return
        }
        lateinit var launcher: ActivityResultLauncher<Intent>
        launcher = activity.activityResultRegistry.register(
            "media_camera_crop_${System.currentTimeMillis()}",
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            launcher.unregister()
            val list = if (result.resultCode == Activity.RESULT_OK) {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra<MediaEntity>(
                    CropImageActivity.EXTRA_RESULT
                )?.let { listOf(it) } ?: emptyList()
            } else {
                emptyList()
            }
            deliverCameraResult(activity, list, listener)
        }
        launcher.launch(Intent(activity, CropImageActivity::class.java).apply {
            putExtra(CropImageActivity.EXTRA_SOURCE, item)
        })
    }

    private fun deliverCameraResult(
        activity: ComponentActivity,
        list: List<MediaEntity>,
        listener: OnPickResultListener,
    ) {
        if (list.isEmpty()) {
            clearRuntimeState()
            listener.onResult(emptyList())
            return
        }

        val imageC = activeImageCompressor ?: globalImageCompressor
        val videoC = activeVideoCompressor ?: globalVideoCompressor
        val item = list.first()
        val needsImageCompress = item.isImage && imageC != null && imageC.needsCompress(item)
        val needsVideoCompress = item.isVideo && videoC != null && videoC.needsCompress(item)
        if (!needsImageCompress && !needsVideoCompress) {
            clearRuntimeState()
            listener.onResult(list)
            return
        }

        var loadingDialog: LoadingDialog? = null

        fun showLoading(text: String) {
            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                val dialog = loadingDialog ?: LoadingDialog(activity).also {
                    loadingDialog = it
                    it.setBackCancelEnabled(enable = false)
                }
                dialog.setText(text)
                if (!dialog.isShowing) dialog.show()
            }
        }

        fun dismissLoading() {
            val block = {
                loadingDialog?.takeIf { it.isShowing }?.dismiss()
                loadingDialog = null
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                block()
            } else {
                activity.runOnUiThread(block)
            }
        }

        val isVideo = needsVideoCompress
        showLoading(buildCameraCompressProgressText(activity, item.isImage, isVideo, 0))

        val callback = CompressCallback(
            originalItem = item,
            delivery = { result ->
                activity.runOnUiThread {
                    dismissLoading()
                    clearRuntimeState()
                    listener.onResult(listOf(result))
                }
            },
            progressDelivery = { percent ->
                showLoading(
                    buildCameraCompressProgressText(
                        activity,
                        item.isImage,
                        isVideo,
                        percent
                    )
                )
            },
        )
        val pool = Executors.newSingleThreadExecutor()
        try {
            pool.execute {
                try {
                    when {
                        needsImageCompress && imageC != null ->
                            imageC.compress(activity.applicationContext, item, callback)

                        needsVideoCompress && videoC != null ->
                            videoC.compress(activity.applicationContext, item, callback)

                        else -> callback.onSuccess(item)
                    }
                } catch (e: Throwable) {
                    callback.onError(e)
                } finally {
                    pool.shutdown()
                }
            }
        } catch (e: Throwable) {
            pool.shutdownNow()
            callback.onError(e)
        }
    }

    private fun buildCameraCompressProgressText(
        activity: ComponentActivity,
        isImage: Boolean,
        isVideo: Boolean,
        percent: Int,
    ): String {
        val safePercent = percent.coerceIn(0, 100)
        val done = if (safePercent >= 100) 1 else 0
        return buildString {
            append(
                activity.getString(
                    R.string.picker_compress_progress,
                    done,
                    1,
                )
            )
            append("\n")
            if (isImage) {
                append(activity.getString(R.string.picker_compress_image_progress, done, 1))
                append("\n")
                append(activity.getString(R.string.picker_compress_image_percent, safePercent))
            } else if (isVideo) {
                append(activity.getString(R.string.picker_compress_video_progress, done, 1))
                append("\n")
                append(activity.getString(R.string.picker_compress_video_percent, safePercent))
            }
        }
    }

    private fun clearRuntimeState() {
        pendingListener = null
        pendingConfig = null
        activeEngine = null
        activeImageCompressor = null
        activeVideoCompressor = null
    }

    fun launchSystemPicker(
        activity: ComponentActivity,
        cfg: SelectionConfig,
        listener: OnPickResultListener,
    ) {
        pendingConfig = cfg
        pendingListener = listener
        val maxItems = if (cfg.enableMultiSelect) {
            cfg.maxCount.coerceAtMost(MAX_SYSTEM_PICKER_ITEMS)
        } else {
            1
        }
        val mediaType = when (cfg.filter.type) {
            MediaType.IMAGE -> ActivityResultContracts.PickVisualMedia.ImageOnly
            MediaType.VIDEO -> ActivityResultContracts.PickVisualMedia.VideoOnly
            else -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
        }
        val fallbackType = cfg.filter.type

        fun dispatchResult(uris: List<Uri>) {
            val app = activity.applicationContext
            sysPickerPool.execute {
                val list = uris.mapNotNull { systemUriToEntity(app, it, fallbackType) }
                activity.runOnUiThread { finishSystemPickedResult(activity, cfg, list, listener) }
            }
        }

        if (maxItems == 1) {
            lateinit var launcher: ActivityResultLauncher<PickVisualMediaRequest>
            launcher = activity.activityResultRegistry.register(
                "media_picker_sys_${System.currentTimeMillis()}",
                ActivityResultContracts.PickVisualMedia(),
            ) { uri: Uri? ->
                launcher.unregister()
                dispatchResult(listOfNotNull(uri))
            }
            launcher.launch(PickVisualMediaRequest(mediaType))
        } else {
            lateinit var launcher: ActivityResultLauncher<PickVisualMediaRequest>
            launcher = activity.activityResultRegistry.register(
                "media_picker_sys_${System.currentTimeMillis()}",
                ActivityResultContracts.PickMultipleVisualMedia(maxItems),
            ) { uris: List<Uri> ->
                launcher.unregister()
                dispatchResult(uris)
            }
            launcher.launch(PickVisualMediaRequest(mediaType))
        }
    }

    private fun finishSystemPickedResult(
        activity: ComponentActivity,
        cfg: SelectionConfig,
        list: List<MediaEntity>,
        listener: OnPickResultListener,
    ) {
        if (list.isEmpty()) {
            deliverResultAndClear(emptyList(), listener)
            return
        }
        val canProcessImages = list.all { it.isImage } &&
            (cfg.imageEditEnabled || (cfg.cropConfig.enabled && list.size == 1))
        if (!canProcessImages) {
            deliverSystemResultWithCompress(activity, list, listener)
            return
        }

        val processor = if (cfg.imageEditEnabled) cfg.imageEditProcessor else cfg.imageCropProcessor
        if (processor != null) {
            val callback = object : ImageProcessCallback {
                override fun onSuccess(result: List<MediaEntity>) {
                    activity.runOnUiThread {
                        deliverSystemResultWithCompress(activity, result, listener)
                    }
                }

                override fun onCancel() {
                    activity.runOnUiThread { deliverResultAndClear(emptyList(), listener) }
                }

                override fun onError(error: Throwable) {
                    onCancel()
                }
            }
            try {
                processor.process(activity, list, cfg.cropConfig, callback)
            } catch (_: Throwable) {
                callback.onCancel()
            }
            return
        }

        lateinit var launcher: ActivityResultLauncher<Intent>
        launcher = activity.activityResultRegistry.register(
            "media_system_crop_${System.currentTimeMillis()}",
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            launcher.unregister()
            val processed = if (result.resultCode == Activity.RESULT_OK) {
                @Suppress("DEPRECATION")
                result.data?.getParcelableArrayListExtra<MediaEntity>(
                    CropImageActivity.EXTRA_RESULTS
                )?.takeIf { it.isNotEmpty() }
                    ?: run {
                        @Suppress("DEPRECATION")
                        result.data?.getParcelableExtra<MediaEntity>(
                            CropImageActivity.EXTRA_RESULT
                        )?.let { arrayListOf(it) }
                    }
                    ?: arrayListOf()
            } else {
                arrayListOf()
            }
            deliverSystemResultWithCompress(activity, processed, listener)
        }
        launcher.launch(Intent(activity, CropImageActivity::class.java).apply {
            putParcelableArrayListExtra(CropImageActivity.EXTRA_SOURCES, ArrayList(list))
            putExtra(CropImageActivity.EXTRA_SOURCE, list.first())
        })
    }

    private fun deliverSystemResultWithCompress(
        activity: ComponentActivity,
        list: List<MediaEntity>,
        listener: OnPickResultListener,
    ) {
        if (list.isEmpty()) {
            deliverResultAndClear(emptyList(), listener)
            return
        }
        val imageC = activeImageCompressor ?: globalImageCompressor
        val videoC = activeVideoCompressor ?: globalVideoCompressor
        val needCompress = list.any { item ->
            (item.isImage && imageC != null && imageC.needsCompress(item)) ||
                (item.isVideo && videoC != null && videoC.needsCompress(item))
        }
        if (!needCompress) {
            deliverResultAndClear(list, listener)
            return
        }

        var loadingDialog: LoadingDialog? = null
        fun showLoading(done: Int) {
            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                val dialog = loadingDialog ?: LoadingDialog(activity).also {
                    loadingDialog = it
                    it.setBackCancelEnabled(enable = false)
                }
                dialog.setText(activity.getString(R.string.picker_compress_progress, done, list.size))
                if (!dialog.isShowing) dialog.show()
            }
        }
        fun dismissLoading() {
            activity.runOnUiThread {
                loadingDialog?.takeIf { it.isShowing }?.dismiss()
                loadingDialog = null
            }
        }

        showLoading(0)
        val pool = Executors.newFixedThreadPool(
            (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4).coerceAtMost(list.size)
        )
        val done = AtomicInteger(0)
        val results = arrayOfNulls<MediaEntity>(list.size)
        list.forEachIndexed { index, item ->
            val callback = CompressCallback(
                originalItem = item,
                delivery = { result ->
                    results[index] = result
                    val count = done.incrementAndGet()
                    showLoading(count)
                    if (count == list.size) {
                        pool.shutdown()
                        dismissLoading()
                        activity.runOnUiThread {
                            deliverResultAndClear(results.filterNotNull(), listener)
                        }
                    }
                },
            )
            pool.execute {
                try {
                    when {
                        item.isImage && imageC != null && imageC.needsCompress(item) ->
                            imageC.compress(activity.applicationContext, item, callback)
                        item.isVideo && videoC != null && videoC.needsCompress(item) ->
                            videoC.compress(activity.applicationContext, item, callback)
                        else -> callback.onSuccess(item)
                    }
                } catch (error: Throwable) {
                    callback.onError(error)
                }
            }
        }
    }

    fun launchInternalPicker(
        activity: ComponentActivity,
        cfg: SelectionConfig,
        listener: OnPickResultListener,
    ) {
        pendingListener = listener
        pendingConfig = cfg
        lateinit var launcher: ActivityResultLauncher<Intent>
        launcher = activity.activityResultRegistry.register(
            "media_picker_${System.currentTimeMillis()}",
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            launcher.unregister()
            if (result.resultCode == Activity.RESULT_OK) {
                @Suppress("DEPRECATION")
                val list =
                    result.data?.getParcelableArrayListExtra<MediaEntity>(MediaSelector.EXTRA_RESULT)
                        ?: arrayListOf()
                listener.onResult(list)
            }
            pendingListener = null
            pendingConfig = null
        }
        launcher.launch(Intent(activity, MediaPickerActivity::class.java))
    }

    fun launchDocumentPicker(
        activity: ComponentActivity,
        mimeTypes: Array<String>,
        allowMultiple: Boolean,
        maxCount: Int = Int.MAX_VALUE,
        listener: OnPickResultListener,
    ) {
        val types = mimeTypes.takeIf { it.isNotEmpty() } ?: arrayOf("*/*")

        fun dispatchResult(uris: List<Uri>) {
            val app = activity.applicationContext
            uris.forEach { uri ->
                runCatching {
                    app.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }
            sysPickerPool.execute {
                val limit = maxCount.coerceAtLeast(1)
                val list = uris.take(limit).map { documentUriToEntity(app, it) }
                activity.runOnUiThread { deliverResultAndClear(list, listener) }
            }
        }

        if (allowMultiple) {
            lateinit var launcher: ActivityResultLauncher<Array<String>>
            launcher = activity.activityResultRegistry.register(
                "file_picker_${System.currentTimeMillis()}",
                ActivityResultContracts.OpenMultipleDocuments(),
            ) { uris: List<Uri> ->
                launcher.unregister()
                dispatchResult(uris)
            }
            launcher.launch(types)
        } else {
            lateinit var launcher: ActivityResultLauncher<Array<String>>
            launcher = activity.activityResultRegistry.register(
                "file_picker_${System.currentTimeMillis()}",
                ActivityResultContracts.OpenDocument(),
            ) { uri: Uri? ->
                launcher.unregister()
                dispatchResult(listOfNotNull(uri))
            }
            launcher.launch(types)
        }
    }

    private fun systemUriToEntity(ctx: Context, uri: Uri, fallbackType: MediaType): MediaEntity? {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            @Suppress("DEPRECATION") MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
        )
        return runCatching {
            ctx.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                if (!c.moveToFirst()) return null
                val rawMime = c.optString(MediaStore.MediaColumns.MIME_TYPE)
                val mime = rawMime ?: when (fallbackType) {
                    MediaType.IMAGE -> "image/*"
                    MediaType.VIDEO -> "video/*"
                    MediaType.AUDIO -> "audio/*"
                    MediaType.IMAGE_VIDEO, MediaType.ALL -> return null
                }
                val mediaType = when {
                    mime.startsWith("image/") -> MediaType.IMAGE
                    mime.startsWith("video/") -> MediaType.VIDEO
                    else -> MediaType.ALL
                }
                MediaEntity(
                    id = c.optLong(MediaStore.MediaColumns._ID).takeIf { it > 0 }
                        ?: uri.toString().hashCode().toLong(),
                    uri = uri,
                    filePath = c.optString(@Suppress("DEPRECATION") MediaStore.MediaColumns.DATA),
                    displayName = c.optString(MediaStore.MediaColumns.DISPLAY_NAME)
                        ?: uri.lastPathSegment.orEmpty(),
                    mimeType = mime,
                    sizeBytes = c.optLong(MediaStore.MediaColumns.SIZE),
                    durationMs = c.optLong(MediaStore.MediaColumns.DURATION),
                    dateAddedSec = c.optLong(MediaStore.MediaColumns.DATE_ADDED),
                    width = c.optInt(MediaStore.MediaColumns.WIDTH),
                    height = c.optInt(MediaStore.MediaColumns.HEIGHT),
                    mediaType = mediaType,
                )
            }
        }.getOrNull() ?: documentUriToEntity(ctx, uri).takeIf {
            when (fallbackType) {
                MediaType.IMAGE -> it.isImage
                MediaType.VIDEO -> it.isVideo
                MediaType.IMAGE_VIDEO -> it.isImage || it.isVideo
                MediaType.AUDIO -> it.isAudio
                MediaType.ALL -> true
            }
        }
    }

    private fun deliverResultAndClear(
        list: List<MediaEntity>,
        listener: OnPickResultListener,
    ) {
        clearRuntimeState()
        listener.onResult(list)
    }

    private fun documentUriToEntity(ctx: Context, uri: Uri): MediaEntity {
        var name = uri.lastPathSegment.orEmpty().ifBlank { "document" }
        var size = 0L
        runCatching {
            ctx.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    c.optString(OpenableColumns.DISPLAY_NAME)?.let { name = it }
                    size = c.optLong(OpenableColumns.SIZE)
                }
            }
        }
        val mime = ctx.contentResolver.getType(uri) ?: "application/octet-stream"
        val mediaType = when {
            mime.startsWith("image/") -> MediaType.IMAGE
            mime.startsWith("video/") -> MediaType.VIDEO
            mime.startsWith("audio/") -> MediaType.AUDIO
            else -> MediaType.ALL
        }
        return MediaEntity(
            id = uri.toString().hashCode().toLong(),
            uri = uri,
            filePath = null,
            displayName = name,
            mimeType = mime,
            sizeBytes = size,
            durationMs = 0L,
            dateAddedSec = 0L,
            width = 0,
            height = 0,
            mediaType = mediaType,
        )
    }

    private fun Cursor.optLong(col: String): Long {
        val idx = getColumnIndex(col); if (idx < 0) return 0
        return getLong(idx)
    }

    private fun Cursor.optInt(col: String): Int {
        val idx = getColumnIndex(col); if (idx < 0) return 0
        return getInt(idx)
    }

    private fun Cursor.optString(col: String): String? {
        val idx = getColumnIndex(col); if (idx < 0) return null
        return getString(idx)
    }
}
