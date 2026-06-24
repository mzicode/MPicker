# MzOmniPicker

[English](README_EN.md) | [GitHub](https://github.com/mzicode/MzOmniPicker)

MzOmniPicker 是一个面向现代 Android 的媒体选择与处理库。它把图片、视频、音频、任意文件选择、系统 Photo Picker、SAF 文件选择器、拍照、录像、裁切、编辑、压缩和预览扩展整合到同一套链式 API 中，适合需要长期兼容不同 Android 版本和不同厂商 ROM 的项目。

> 核心入口：`MzOmniPicker.with(activity)` / `MzOmniPicker.with(fragment)`

## 目录

- [特性](#特性)
- [适配范围](#适配范围)
- [安装](#安装)
- [权限配置](#权限配置)
- [快速开始](#快速开始)
- [主题](#主题)
- [裁切、编辑与压缩](#裁切编辑与压缩)
- [系统 Photo Picker 与 SAF](#系统-photo-picker-与-saf)
- [相机与录像](#相机与录像)
- [高级扩展](#高级扩展)
- [返回结果](#返回结果)
- [API 速查](#api-速查)
- [混淆配置](#混淆配置)
- [FAQ](#faq)
- [License](#license)

## 特性

- **媒体选择**：支持图片、视频、音频、图片+视频、全部文件。
- **任意文件**：`MediaType.ALL` 可返回图片、视频、音频以及 PDF、Word、Excel、PPT、TXT、ZIP 等其他文件。
- **选择体验**：支持网格/列表、单选/多选、最大数量限制、预选回显、首位相机入口。
- **预览能力**：支持图片预览、视频播放、音频信息展示，其他文件可接入自定义预览 View。
- **系统选择器**：支持 Android Photo Picker 和 SAF 文件选择器，便于降低权限依赖并满足 Google Play 合规要求。
- **相机能力**：支持直接拍照、直接录像、限时录像、点击录像、长按录像、列表内相机入口。
- **裁切能力**：支持自由比例、固定比例、圆形裁切、输出格式、输出质量和最大尺寸控制。
- **图片编辑**：支持多图编辑、裁切、画笔、文字、马赛克、颜色和画笔大小。
- **压缩能力**：内置智能图片压缩和 MediaCodec 视频压缩，也可替换为自定义压缩器。
- **主题能力**：内置 `GREEN`、`WECHAT_DARK`、`SKY`、`AMBER` 四套主题，也支持自定义主题。
- **扩展能力**：支持自定义图片加载、自定义文档封面、自定义文档预览、第三方裁切/编辑 SDK 接入。
- **统一结果**：无论选择、拍照、录像、裁切、编辑还是压缩，最终都返回 `List<MediaEntity>`。

## 适配范围

- minSdk 21+
- compileSdk 36+
- AndroidX
- Java 11
- Kotlin API 兼容 Kotlin 1.8

`compileSdk` 只影响库的编译环境，接入方不需要因为使用本库而同步提高 `targetSdk`。如果应用已适配 Android 13/14 权限模型，建议优先使用系统 Photo Picker 或按媒体类型声明权限。

## 安装

### 1. 添加 JitPack 仓库

如果项目使用 `settings.gradle`：

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

如果项目使用 `settings.gradle.kts`：

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

### 2. 添加依赖

Groovy：

```groovy
dependencies {
    implementation 'io.github.mzicode:mzomnipicker:1.0.0'
}
```

Kotlin DSL：

```kotlin
dependencies {
    implementation("io.github.mzicode:mzomnipicker:1.0.0")
}
```

### 3. 导入入口

```kotlin
import io.github.mz.mzomnipicker.api.MzOmniPicker
import io.github.mz.mzomnipicker.model.MediaType
```

## 权限配置

MzOmniPicker 不会在库 Manifest 中自动声明媒体读取、相机、录音、网络或所有文件访问权限。宿主 App 应根据业务场景自行声明。框架在申请运行时权限前会检查宿主 Manifest；如果缺少声明，会在 Logcat 中提示并跳过权限申请，避免线上因库隐式权限导致审核或隐私问题。

常用权限：

```xml
<!-- Android 12 及以下读取媒体文件 -->
<uses-permission
    android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />

<!-- Android 9 及以下保存拍摄文件到公共目录时需要 -->
<uses-permission
    android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />

<!-- Android 13+ 按媒体类型读取 -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

<!-- Android 14+ 部分照片/视频访问 -->
<uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" />

<!-- 拍照 / 录像 -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- 只有业务明确需要所有文件访问时才声明，并由宿主自行引导用户授权 -->
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />

<!-- 自定义图片加载、远程封面、在线文档预览等场景需要网络时声明 -->
<uses-permission android:name="android.permission.INTERNET" />
```

推荐按场景最小化声明：

| 场景 | 建议权限 |
| --- | --- |
| 只选图片 | Android 13+ 使用 `READ_MEDIA_IMAGES`；Android 12 及以下加 `READ_EXTERNAL_STORAGE` |
| 只选视频 | Android 13+ 使用 `READ_MEDIA_VIDEO`；Android 12 及以下加 `READ_EXTERNAL_STORAGE` |
| 只选音频 | Android 13+ 使用 `READ_MEDIA_AUDIO`；Android 12 及以下加 `READ_EXTERNAL_STORAGE` |
| 图片 + 视频 | `READ_MEDIA_IMAGES` + `READ_MEDIA_VIDEO`；Android 14+ 可加 `READ_MEDIA_VISUAL_USER_SELECTED` |
| 系统 Photo Picker | 通常不需要读取媒体权限 |
| SAF 文件选择 | 通常不需要存储权限，优先使用返回的 `uri` |
| 拍照 | `CAMERA`；Android 9 及以下保存公共目录时加 `WRITE_EXTERNAL_STORAGE` |
| 录像 | `CAMERA` + `RECORD_AUDIO`；Android 9 及以下保存公共目录时加 `WRITE_EXTERNAL_STORAGE` |

## 快速开始

### 选择图片

```kotlin
MzOmniPicker.with(this)
    .type(MediaType.IMAGE)
    .maxCount(9)
    .grid(true)
    .spanCount(4)
    .start { result ->
        // result: List<MediaEntity>
    }
```

### 选择视频

```kotlin
MzOmniPicker.with(this)
    .type(MediaType.VIDEO)
    .maxCount(3)
    .grid(true)
    .start { result ->
        // 处理视频结果
    }
```

### 选择图片和视频

```kotlin
MzOmniPicker.with(this)
    .type(MediaType.IMAGE_VIDEO)
    .maxCount(9)
    .showCameraEntry(true)
    .start { result ->
        // 图片和视频统一返回 MediaEntity
    }
```

### 选择任意文件

```kotlin
MzOmniPicker.with(this)
    .type(MediaType.ALL)
    .maxCount(20)
    .start { result ->
        // 可能包含图片、视频、音频、PDF、Office、ZIP 等
    }
```

### 使用 Fragment

```kotlin
MzOmniPicker.with(this@YourFragment)
    .type(MediaType.IMAGE)
    .maxCount(9)
    .start { result ->
        // Fragment 版本无需手动传 requireActivity()
    }
```

## 主题

内置主题适合快速覆盖常见业务风格：

| 主题 | 适合场景 |
| --- | --- |
| `PickerTheme.GREEN` | 默认绿色、通用媒体选择器风格 |
| `PickerTheme.WECHAT_DARK` | 深色、聊天/社交类图片选择体验 |
| `PickerTheme.SKY` | 蓝色、轻量工具或内容社区风格 |
| `PickerTheme.AMBER` | 暖色、生活服务或电商活动风格 |

单次调用设置主题：

```kotlin
import io.github.mz.mzomnipicker.api.PickerTheme

MzOmniPicker.with(this)
    .type(MediaType.IMAGE_VIDEO)
    .theme(PickerTheme.WECHAT_DARK)
    .maxCount(9)
    .start { result -> }
```

设置全局主题：

```kotlin
MzOmniPicker.setTheme(PickerTheme.SKY)
```

自定义主题：

```kotlin
val brandTheme = PickerTheme.GREEN.copy(
    primary = 0xFF0EA5E9.toInt(),
    primaryPressed = 0xFF0284C7.toInt(),
    topBarBackground = 0xFF082F49.toInt(),
    bottomBarBackground = 0xFFE0F2FE.toInt(),
)

MzOmniPicker.with(this)
    .type(MediaType.IMAGE)
    .theme(brandTheme)
    .start { result -> }
```

Demo 首页已提供主题切换按钮，可直接在 Android Studio 中运行体验。

## 裁切、编辑与压缩

### 图片裁切

```kotlin
import io.github.mz.mzomnipicker.api.CropOutputFormat

MzOmniPicker.with(this)
    .type(MediaType.IMAGE)
    .crop()
    .cropAspectRatio(1, 1)
    .cropOutput(CropOutputFormat.JPEG, quality = 85)
    .cropMaxSize(1024, 1024)
    .start { result ->
        val cropped = result.firstOrNull()
    }
```

### 自由裁切

```kotlin
MzOmniPicker.with(this)
    .type(MediaType.IMAGE)
    .crop()
    .cropFreeStyle()
    .start { result -> }
```

### 圆形裁切

圆形裁切会自动启用 `1:1` 比例，并默认输出 PNG，以保留圆形外部透明区域。

```kotlin
MzOmniPicker.with(this)
    .type(MediaType.IMAGE)
    .cropOval()
    .start { result -> }
```

### 多图编辑

```kotlin
MzOmniPicker.with(this)
    .type(MediaType.IMAGE)
    .maxCount(9)
    .imageEdit()
    .start { result ->
        // 每张编辑后的图片会以新的 MediaEntity 返回
    }
```

### 图片压缩

```kotlin
MzOmniPicker.with(this)
    .type(MediaType.IMAGE)
    .maxCount(9)
    .smartCompress(
        ignoreByKb = 100,
        quality = 85,
        minQuality = 75,
        maxWidth = 1080,
        maxHeight = 1920,
    )
    .start { result -> }
```

### 视频压缩

```kotlin
MzOmniPicker.with(this)
    .type(MediaType.VIDEO)
    .smartVideoCompress(
        maxLongSide = 1280,
        targetBitRate = 2_500_000,
        frameRate = 30,
    )
    .start { result -> }
```

### 组合处理链

系统 Photo Picker、裁切和压缩可以组合。系统选择完成后，MzOmniPicker 会继续进入统一处理链路。

```kotlin
MzOmniPicker.with(this)
    .type(MediaType.IMAGE)
    .useSystemPhotoPicker(true)
    .crop()
    .cropAspectRatio(1, 1)
    .smartCompress()
    .start { result -> }
```

## 系统 Photo Picker 与 SAF

### 系统 Photo Picker

```kotlin
MzOmniPicker.with(this)
    .type(MediaType.IMAGE)
    .useSystemPhotoPicker(true)
    .maxCount(9)
    .start { result -> }
```

说明：

- 适合图片/视频选择，隐私体验更好，通常不需要读取媒体权限。
- `MediaType.AUDIO` 不走系统 Photo Picker。
- 当前实现中 `MediaType.ALL` 会走框架自身或 SAF，不会走系统 Photo Picker。
- 开启裁切、编辑或压缩时，系统选择完成后会继续进入 MzOmniPicker 处理链路。

### SAF 文件选择器

```kotlin
MzOmniPicker.pickFiles(
    activity = this,
    mimeTypes = arrayOf("application/pdf", "application/zip"),
    allowMultiple = true,
) { result ->
    // SAF 返回的 filePath 可能为空，建议优先使用 uri
}
```

也可以通过链式 API 使用 SAF：

```kotlin
MzOmniPicker.with(this)
    .filter(MediaType.ALL) {
        addMimeType("application/pdf", "application/zip")
    }
    .useSystemFilePicker(true)
    .multiSelect(true)
    .maxCount(10)
    .start { result -> }
```

## 相机与录像

### 直接拍照并进入处理链

```kotlin
MzOmniPicker.with(this)
    .takePhoto()
    .crop()
    .smartCompress()
    .start { result -> }
```

### 独立拍照

```kotlin
MzOmniPicker.takePhoto(this) { success, filePath, uri ->
    if (success) {
        // filePath / uri 为拍照结果
    }
}
```

### 直接录像并进入处理链

```kotlin
import io.github.mz.mzomnipicker.api.CameraRecordTrigger

MzOmniPicker.with(this)
    .takeVideo()
    .recordDurationMs(10_000L, countDown = true)
    .recordTrigger(CameraRecordTrigger.CLICK)
    .smartVideoCompress()
    .start { result -> }
```

### 长按录像

```kotlin
MzOmniPicker.with(this)
    .takeVideo()
    .recordDurationMs(15_000L, countDown = true)
    .longPressRecord()
    .start { result -> }
```

### 列表首位相机入口

```kotlin
MzOmniPicker.with(this)
    .type(MediaType.IMAGE)
    .showCameraEntry(true)
    .photoMode()
    .start { result -> }
```

视频列表中使用录像入口：

```kotlin
MzOmniPicker.with(this)
    .type(MediaType.VIDEO)
    .showCameraEntry(true)
    .videoMode()
    .recordDurationMs(30_000L)
    .start { result -> }
```

### 前置摄像头镜像说明

部分设备前置摄像头录制的视频会带镜像效果。链式录像 API 会通过 `MediaEntity.mirrorHorizontal` 标记视频是否需要水平翻转。内置压缩器会尽量处理该信息；如果你使用自定义 `IVideoCompressor`，需要在转码绘制阶段判断并处理该字段，输出后建议把 `mirrorHorizontal` 置为 `false`。

独立 API `MzOmniPicker.takeVideo(activity, ...)` 只返回原始 `filePath/uri`，不返回 `MediaEntity`，因此不携带 `mirrorHorizontal`。如果需要镜像标记，请使用 `MzOmniPicker.with(this).takeVideo().start { ... }`。

## 高级扩展

### 媒体筛选

```kotlin
MzOmniPicker.with(this)
    .filter(MediaType.IMAGE) {
        addMimeType("image/jpeg", "image/png")
        minSizeBytes(20 * 1024)
    }
    .maxCount(9)
    .start { result -> }
```

视频时长过滤：

```kotlin
MzOmniPicker.with(this)
    .filter(MediaType.VIDEO) {
        maxDurationMs(60_000L)
    }
    .start { result -> }
```

### 预选回显

```kotlin
MzOmniPicker.with(this)
    .type(MediaType.IMAGE)
    .preSelected(selectedItems)
    .maxCount(9)
    .start { result ->
        selectedItems = result
    }
```

预选匹配主要依赖 `id + mediaType`，适合业务编辑页重新打开时回显已选素材。

### 预加载与缓存

```kotlin
MzOmniPicker.preload(this, MediaType.IMAGE, MediaType.VIDEO)

val cachedImages = MzOmniPicker.cached(MediaType.IMAGE)

MzOmniPicker.invalidateCache()
```

建议在权限已就绪后预加载。拍照、保存新文件或外部媒体库变化后，可调用 `invalidateCache()` 强制刷新。

### 自定义图片加载

实现 `IImageEngine` 可以接入 Glide、Coil、Picasso，也可以按文件类型显示自定义封面。

```kotlin
import android.widget.ImageView
import io.github.mz.mzomnipicker.loader.IImageEngine
import io.github.mz.mzomnipicker.model.MediaEntity

class AppImageEngine : IImageEngine {
    override fun loadThumbnail(view: ImageView, item: MediaEntity) {
        // 示例：Glide.with(view).load(item.uri).into(view)
        // 也可以根据 item.mimeType / item.displayName 为 PDF、Word、ZIP 设置图标
    }

    override fun loadOriginal(view: ImageView, item: MediaEntity) {
        // 示例：Glide.with(view).load(item.uri).into(view)
    }
}

MzOmniPicker.setImageEngine(AppImageEngine())
```

### 自定义其他文件预览

当 `MediaType.ALL` 返回 PDF、Office、TXT、ZIP 等非媒体文件时，可以通过 `IOtherPreviewProvider` 接入自己的预览能力。

```kotlin
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.github.mz.mzomnipicker.model.MediaEntity
import io.github.mz.mzomnipicker.preview.IOtherPreviewProvider

MzOmniPicker.setOtherPreviewProvider(object : IOtherPreviewProvider {
    override fun createView(parent: ViewGroup): View {
        return TextView(parent.context).apply {
            textSize = 16f
        }
    }

    override fun bindView(view: View, item: MediaEntity) {
        (view as TextView).text = "Preview: ${item.displayName}"
    }

    override fun onViewRecycled(view: View) {
        // 清理渲染任务、下载任务或播放器资源
    }
})
```

### 自定义压缩器

```kotlin
import android.content.Context
import io.github.mz.mzomnipicker.compress.CompressCallback
import io.github.mz.mzomnipicker.compress.IImageCompressor
import io.github.mz.mzomnipicker.model.MediaEntity

class AppImageCompressor : IImageCompressor {
    override fun needsCompress(item: MediaEntity): Boolean {
        return item.sizeBytes > 500 * 1024
    }

    override fun compress(context: Context, item: MediaEntity, callback: CompressCallback) {
        runCatching {
            // 执行业务压缩，得到新的 uri、path、size 后 copy 一个 MediaEntity
            item
        }.onSuccess { compressed ->
            callback.onSuccess(compressed)
        }.onFailure { error ->
            callback.onError(error) // 框架会自动使用原文件兜底
        }
    }
}

MzOmniPicker.with(this)
    .type(MediaType.IMAGE)
    .imageCompressor(AppImageCompressor())
    .start { result -> }
```

注意：自定义图片/视频压缩器必须调用 `callback.onSuccess(...)` 或 `callback.onError(...)`，否则压缩 loading 不会结束。

### 第三方裁切或编辑 SDK

如果内置裁切/编辑 UI 不满足需求，可以实现 `IImageProcessProcessor` 接入第三方 SDK。第三方处理完成后调用 `callback.onSuccess(result)`，MzOmniPicker 会继续执行压缩并从 `start {}` 返回最终结果。

```kotlin
MzOmniPicker.with(this)
    .type(MediaType.IMAGE)
    .crop()
    .imageCropProcessor { activity, items, cropConfig, callback ->
        // 启动第三方裁切页面，完成后回调：
        callback.onSuccess(items)
    }
    .start { result -> }
```

图片编辑同理：

```kotlin
MzOmniPicker.with(this)
    .type(MediaType.IMAGE)
    .imageEdit()
    .imageEditProcessor { activity, items, cropConfig, callback ->
        callback.onSuccess(items)
    }
    .start { result -> }
```

## 返回结果

`start { result -> }` 返回 `List<MediaEntity>`。

| 字段 | 说明 |
| --- | --- |
| `id` | MediaStore id；系统选择器或临时文件场景可能是生成值 |
| `uri` | 推荐优先使用的文件访问入口 |
| `filePath` | 真实路径；部分系统选择器、云端文件、SAF 文件可能为空 |
| `displayName` | 展示名称 |
| `mimeType` | MIME 类型，例如 `image/jpeg`、`video/mp4`、`application/pdf` |
| `sizeBytes` | 文件大小，单位 byte |
| `durationMs` | 视频/音频时长；图片和普通文件通常为 0 |
| `dateAddedSec` | 添加时间，单位秒 |
| `width` / `height` | 图片或视频尺寸；未知时可能为 0 |
| `mediaType` | `IMAGE`、`VIDEO`、`AUDIO`、`IMAGE_VIDEO`、`ALL` |
| `albumId` | 音频专辑 id，可用于获取 `albumArtUri` |
| `mirrorHorizontal` | 前置摄像头录像是否建议水平翻转 |
| `isImage` / `isVideo` / `isAudio` | MIME 类型便捷判断 |
| `albumArtUri` | 音频专辑封面 Uri，无封面时为 null |

使用建议：

- 优先使用 `uri`，不要强依赖 `filePath`。
- SAF 或云端文件可能只有 `uri`，业务层需要用 `ContentResolver` 读取。
- 压缩、裁切、编辑后通常会返回新的 `uri/filePath/sizeBytes`。

## API 速查

### 基础选择

| API | 说明 |
| --- | --- |
| `MzOmniPicker.with(activity)` | 创建 Activity 版选择器 |
| `MzOmniPicker.with(fragment)` | 创建 Fragment 版选择器 |
| `type(type)` | 设置媒体类型 |
| `filter(filter)` / `filter(type) {}` | 设置筛选条件 |
| `maxCount(n)` | 最大选择数量 |
| `multiSelect(enable)` | 是否多选 |
| `grid(enable)` | 网格或列表模式 |
| `spanCount(n)` | 网格列数 |
| `preSelected(list)` | 预选回显 |
| `showFirstLoading(enable)` | 首次加载是否显示 loading |
| `start { result -> }` | 启动选择并返回结果 |

### 裁切与编辑

| API | 说明 |
| --- | --- |
| `crop()` | 开启裁切 |
| `cropAspectRatio(x, y)` | 固定裁切比例 |
| `cropFreeStyle()` | 自由比例裁切 |
| `cropOval()` | 圆形裁切 |
| `cropOutput(format, quality)` | 输出格式与质量 |
| `cropMaxSize(width, height)` | 输出最大尺寸 |
| `imageEdit()` | 开启图片编辑 |
| `imageCropProcessor(processor)` | 接入第三方裁切 |
| `imageEditProcessor(processor)` | 接入第三方编辑 |

### 压缩

| API | 说明 |
| --- | --- |
| `smartCompress(...)` | 本次选择启用内置图片压缩 |
| `smartVideoCompress(...)` | 本次选择启用内置视频压缩 |
| `imageCompressor(c)` | 本次选择使用自定义图片压缩器 |
| `videoCompressor(c)` | 本次选择使用自定义视频压缩器 |
| `cancelCompressOnBack(enable)` | 压缩 loading 期间是否允许返回取消 |
| `MzOmniPicker.setSmartImageCompressor(...)` | 全局启用内置图片压缩 |
| `MzOmniPicker.setSmartVideoCompressor(...)` | 全局启用内置视频压缩 |
| `MzOmniPicker.setImageCompressor(c)` | 全局设置图片压缩器；传 null 取消 |
| `MzOmniPicker.setVideoCompressor(c)` | 全局设置视频压缩器；传 null 取消 |

### 系统选择器

| API | 说明 |
| --- | --- |
| `useSystemPhotoPicker(enable)` | API 33+ 优先使用系统 Photo Picker |
| `useSystemFilePicker(enable)` | 使用 SAF 文件选择器 |
| `MzOmniPicker.pickFiles(activity, mimeTypes, allowMultiple, listener)` | 独立 SAF 文件选择入口 |
| `MzOmniPicker.pickFiles(fragment, mimeTypes, allowMultiple, listener)` | Fragment 版 SAF 文件选择入口 |

### 相机

| API | 说明 |
| --- | --- |
| `takePhoto()` | 链式拍照，结果进入 `start` 回调 |
| `takeVideo()` | 链式录像，结果进入 `start` 回调 |
| `MzOmniPicker.takePhoto(activity, listener)` | 独立拍照，不进入选择器 UI |
| `MzOmniPicker.takeVideo(activity, listener)` | 独立录像，不进入选择器 UI |
| `MzOmniPicker.takeVideo(activity, maxDurationMs, countDown, trigger, listener)` | 独立限时录像 |
| `showCameraEntry(enable)` | 列表首位显示相机入口 |
| `photoMode()` / `videoMode()` | 指定列表首位相机入口模式 |
| `recordDurationMs(durationMs, countDown)` | 设置录像最长时长和倒计时 |
| `clickRecord()` / `longPressRecord()` | 点击录像或长按录像 |

### 全局扩展

| API | 说明 |
| --- | --- |
| `MzOmniPicker.setTheme(theme)` | 设置全局主题 |
| `MzOmniPicker.setImageEngine(engine)` | 设置全局图片加载引擎 |
| `MzOmniPicker.setOtherPreviewProvider(provider)` | 注册其他文件预览扩展 |
| `MzOmniPicker.preload(context, vararg types)` | 预加载媒体首屏数据 |
| `MzOmniPicker.cached(type)` | 获取首屏缓存 |
| `MzOmniPicker.invalidateCache()` | 清空媒体缓存 |

## 混淆配置

库已内置 `consumer-rules.pro`，一般不需要额外配置。如果你接入了第三方图片加载、文档预览、裁切编辑或压缩 SDK，请按对应 SDK 文档添加规则。

如果业务侧通过反射访问 MzOmniPicker 的公开 API，可额外保留：

```proguard
-keep class io.github.mz.mzomnipicker.api.MzOmniPicker { public *; }
-keep class io.github.mz.mzomnipicker.model.MediaEntity { *; }
```

## FAQ

### 为什么选择结果的 `filePath` 为空？

系统 Photo Picker、SAF、云端文件或部分厂商 ROM 可能只提供 `content://` Uri。建议业务侧优先使用 `uri`，并通过 `ContentResolver` 读取流。只有明确需要真实路径的场景才使用 `filePath`。

### 为什么库不自动声明权限？

媒体权限、相机权限、所有文件访问权限都可能影响上架审核和隐私披露。MzOmniPicker 选择让宿主 App 显式声明需要的权限，避免库隐式带入不必要权限。

### Android 13/14 应该优先用什么方式选图？

如果只是选图片/视频，优先考虑 `useSystemPhotoPicker(true)`，通常不需要读取媒体权限。如果需要统一网格 UI、文档混选、裁切编辑深度定制或自定义预览，可以使用框架内置选择器并按场景申请权限。

### `MediaType.ALL` 能替代文件管理器吗？

它适合在媒体选择器场景中混合展示常见媒体和文档文件。如果你的业务是完整文件管理器，应优先使用 SAF 或业务自有文件扫描能力，并谨慎处理 `MANAGE_EXTERNAL_STORAGE` 合规问题。

### 自定义压缩失败怎么办？

调用 `CompressCallback.onError(error)`。框架会自动使用原文件兜底，并结束 loading。不要既不回调成功也不回调失败。

### 多主题是否只影响选择页？

主题会应用到内置选择页、预览页、裁切页和编辑页的主要颜色。业务自定义预览 View 或第三方裁切/编辑 SDK 的 UI 需要业务侧自行适配主题。

## Roadmap

- 增加更多可配置 UI 文案和图标槽位。
- 增加更多主题预设和 Material You 风格示例。
- 增加示例截图和 GIF 演示。
- 持续优化 Android 14/15 媒体权限和系统选择器体验。

## License

MzOmniPicker 使用 Apache License 2.0。详见 [LICENSE](LICENSE)。
