# MPicker

[English](README_EN.md) | [GitHub](https://github.com/mzicode/MPicker)

MPicker 是一个面向现代 Android 的媒体选择与处理库。它把图片、视频、音频、任意文件选择、系统 Photo Picker、SAF 文件选择器、拍照、录像、裁切、编辑、压缩和预览扩展整合到同一套链式 API 中，重点解决 Android 多版本媒体权限、分区存储、系统选择器和厂商 ROM 差异带来的长期兼容问题。

> 核心入口：`MPicker.with(activity)` / `MPicker.with(fragment)`
> 系统范围：支持 Android 5.0（API 21）到 Android 16（API 36），兼容 Android 10 分区存储、Android 13/14 媒体权限模型、系统 Photo Picker 和 SAF。

## 目录

- [三步上手（黄金路径）](#三步上手黄金路径)
- [该用哪种方式？（选型决策表）](#该用哪种方式选型决策表)
- [安装](#安装)
- [权限配置](#权限配置)
- [快速开始（完整示例）](#快速开始完整示例)
- [主题](#主题)
- [裁切、编辑与压缩（进阶）](#裁切编辑与压缩进阶)
- [系统 Photo Picker 与 SAF](#系统-photo-picker-与-saf)
- [相机与录像（进阶）](#相机与录像进阶)
- [高级扩展（高级）](#高级扩展高级)
- [返回结果](#返回结果)
- [API 速查](#api-速查)
- [混淆配置](#混淆配置)
- [FAQ](#faq)
- [License](#license)

---

## 三步上手（黄金路径）

90% 的需求只需要下面三种用法。复制即用，无需额外配置：

### 1. 选图片

```kotlin
MPicker.with(this)
    .type(MediaType.IMAGE)
    .maxCount(9)
    .grid(true)
    .start { result ->
        // result: List<MediaEntity>
    }
```

### 2. 选视频

```kotlin
MPicker.with(this)
    .type(MediaType.VIDEO)
    .maxCount(3)
    .grid(true)
    .start { result -> }
```

### 3. 拍照

```kotlin
MPicker.with(this)
    .takePhoto()
    .start { result -> }
```

> 以上是最常见的「黄金路径」。下面的内容都是**可选进阶**——系统选择器、裁切、编辑、压缩、录像、扩展点等，按需查看即可。

## 该用哪种方式？（选型决策表）

如果你不确定该用哪种入口，先看这张表。它按「场景 → 推荐 → 权限负担」组织，能帮你避开不必要的权限和合规麻烦。

| 你的场景 | 推荐 | 权限负担 | 说明 |
| --- | --- | --- | --- |
| 只选图/视频，想零权限、快速上架 | `useSystemPhotoPicker(true)` | 几乎无 | 系统 Photo Picker，隐私体验最好 |
| 要统一网格 UI / 列表选择 | 内置选择器（`.type(...)`） | 按媒体类型声明 | 需要在 Manifest 声明对应读取权限 |
| 选 PDF / ZIP / Word 等文档 | `useSystemFilePicker(true)` 或 `MPicker.pickFiles(...)` | 通常无 | SAF，优先用返回的 `uri` |
| 媒体 + 文档混合展示 | `.type(MediaType.ALL)`（进阶） | 高，含 `MANAGE_EXTERNAL_STORAGE` | Google Play 审核风险，见 [权限配置](#权限配置) |
| 拍一张照片 | `.takePhoto()` | `CAMERA` | 结果进入统一回调 |
| 拍头像并裁切 | `.takePhoto().cropOval()` | `CAMERA` | 圆形裁切自动 1:1 + PNG |
| 录视频 | `.takeVideo()` | `CAMERA` + `RECORD_AUDIO` | 支持限时/点击/长按 |
| 选完要压缩/编辑 | 在上面任意链路后接 `.smartCompress()` / `.imageEdit()` | 同上 | 进阶，见对应章节 |

> **简单原则**：能用系统 Photo Picker / SAF 就用它们（权限最少、合规最省心）；只有需要统一网格 UI、深度定制 UI 或文档混选时，才用内置选择器并声明对应权限。

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
    implementation 'io.github.mzicode:mpicker:1.0.0'
}
```

Kotlin DSL：

```kotlin
dependencies {
    implementation("io.github.mzicode:mpicker:1.0.0")
}
```

### 3. 导入入口

```kotlin
import io.github.mz.mpicker.api.MPicker
import io.github.mz.mpicker.model.MediaType
```

## 权限配置

### 核心原则

MPicker **不会在库 Manifest 中自动声明**媒体读取、相机、录音、网络或所有文件访问权限。宿主 App 应根据业务场景自行声明。这样做的目的：

- 避免库隐式带入不必要权限，影响上架审核和隐私披露；
- 让你按场景**最小化声明**，降低用户授权负担。

框架在申请运行时权限前会检查宿主 Manifest：

- 如果宿主**已声明**所需权限，正常弹出运行时授权；
- 如果宿主**未声明**，会在选择页显示「未在 AndroidManifest.xml 中声明媒体权限」的提示（同时 Logcat 打印缺哪些权限），而不是静默失败。

### 零权限路线（推荐优先）

系统 Photo Picker 和 SAF 通常**不需要任何媒体读取权限**，是上架审核和隐私体验最友好的方式：

```kotlin
// 系统 Photo Picker：选图/视频，几乎零权限
MPicker.with(this)
    .type(MediaType.IMAGE)
    .useSystemPhotoPicker(true)
    .maxCount(9)
    .start { result -> }

// SAF：选任意文档，通常零权限
MPicker.pickFiles(
    activity = this,
    mimeTypes = arrayOf("application/pdf", "application/zip"),
    allowMultiple = true,
) { result -> }
```

### 按场景最小化声明（可直接复制）

按你的实际场景，复制对应的权限块到宿主 `AndroidManifest.xml`：

**只选图片**

```xml
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission
    android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

**只选视频**

```xml
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission
    android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

**图片 + 视频**

```xml
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<!-- Android 14+ 可选：部分照片/视频访问 -->
<uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" />
<uses-permission
    android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

**只选音频**

```xml
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission
    android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

**拍照 / 录像**

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<!-- Android 9 及以下保存到公共目录时需要 -->
<uses-permission
    android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

**全部权限参考（按需选用，不要全抄）**

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

> ⚠️ **关于 `MANAGE_EXTERNAL_STORAGE` / `MediaType.ALL` 的合规提示**：`MediaType.ALL` 内部列表依赖所有文件访问权限。`MANAGE_EXTERNAL_STORAGE` 在 Google Play 审核极严（需填写权限声明表、可能被拒），国内商店也有各自政策。**如果你的业务只是选几个文档，优先用 SAF（`MPicker.pickFiles`），不要为了 ALL 去申请所有文件访问权限。**

## 快速开始（完整示例）

### 选择图片

```kotlin
MPicker.with(this)
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
MPicker.with(this)
    .type(MediaType.VIDEO)
    .maxCount(3)
    .grid(true)
    .start { result ->
        // 处理视频结果
    }
```

### 选择图片和视频

```kotlin
MPicker.with(this)
    .type(MediaType.IMAGE_VIDEO)
    .maxCount(9)
    .showCameraEntry(true)
    .start { result ->
        // 图片和视频统一返回 MediaEntity
    }
```

### 选择任意文件（进阶）

```kotlin
MPicker.with(this)
    .type(MediaType.ALL)
    .maxCount(20)
    .start { result ->
        // 可能包含图片、视频、音频、PDF、Office、ZIP 等
    }
```

> `MediaType.ALL` 需要所有文件访问权限，合规风险较高，详见 [权限配置](#权限配置)。多数「选文档」场景用 SAF（`MPicker.pickFiles`）更合适。

### 使用 Fragment

```kotlin
MPicker.with(this@YourFragment)
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
import io.github.mz.mpicker.api.PickerTheme

MPicker.with(this)
    .type(MediaType.IMAGE_VIDEO)
    .theme(PickerTheme.WECHAT_DARK)
    .maxCount(9)
    .start { result -> }
```

设置全局主题：

```kotlin
MPicker.setTheme(PickerTheme.SKY)
```

自定义主题：

```kotlin
val brandTheme = PickerTheme.GREEN.copy(
    primary = 0xFF0EA5E9.toInt(),
    primaryPressed = 0xFF0284C7.toInt(),
    topBarBackground = 0xFF082F49.toInt(),
    bottomBarBackground = 0xFFE0F2FE.toInt(),
)

MPicker.with(this)
    .type(MediaType.IMAGE)
    .theme(brandTheme)
    .start { result -> }
```

Demo 首页已提供主题切换按钮，可直接在 Android Studio 中运行体验。

## 裁切、编辑与压缩（进阶）

### 图片裁切

```kotlin
import io.github.mz.mpicker.api.CropOutputFormat

MPicker.with(this)
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
MPicker.with(this)
    .type(MediaType.IMAGE)
    .crop()
    .cropFreeStyle()
    .start { result -> }
```

### 圆形裁切

圆形裁切会自动启用 `1:1` 比例，并默认输出 PNG，以保留圆形外部透明区域。

```kotlin
MPicker.with(this)
    .type(MediaType.IMAGE)
    .cropOval()
    .start { result -> }
```

### 多图编辑

```kotlin
MPicker.with(this)
    .type(MediaType.IMAGE)
    .maxCount(9)
    .imageEdit()
    .start { result ->
        // 每张编辑后的图片会以新的 MediaEntity 返回
    }
```

### 图片压缩

```kotlin
MPicker.with(this)
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
MPicker.with(this)
    .type(MediaType.VIDEO)
    .smartVideoCompress(
        maxLongSide = 1280,
        targetBitRate = 2_500_000,
        frameRate = 30,
    )
    .start { result -> }
```

### 组合处理链

系统 Photo Picker、裁切和压缩可以组合。系统选择完成后，MPicker 会继续进入统一处理链路。

```kotlin
MPicker.with(this)
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
MPicker.with(this)
    .type(MediaType.IMAGE)
    .useSystemPhotoPicker(true)
    .maxCount(9)
    .start { result -> }
```

说明：

- 适合图片/视频选择，隐私体验更好，通常不需要读取媒体权限。
- `MediaType.AUDIO` 不走系统 Photo Picker。
- 当前实现中 `MediaType.ALL` 会走框架自身或 SAF，不会走系统 Photo Picker。
- 开启裁切、编辑或压缩时，系统选择完成后会继续进入 MPicker 处理链路。

### SAF 文件选择器

```kotlin
MPicker.pickFiles(
    activity = this,
    mimeTypes = arrayOf("application/pdf", "application/zip"),
    allowMultiple = true,
) { result ->
    // SAF 返回的 filePath 可能为空，建议优先使用 uri
}
```

也可以通过链式 API 使用 SAF：

```kotlin
MPicker.with(this)
    .filter(MediaType.ALL) {
        addMimeType("application/pdf", "application/zip")
    }
    .useSystemFilePicker(true)
    .multiSelect(true)
    .maxCount(10)
    .start { result -> }
```

## 相机与录像（进阶）

### 直接拍照并进入处理链

```kotlin
MPicker.with(this)
    .takePhoto()
    .crop()
    .smartCompress()
    .start { result -> }
```

### 独立拍照

```kotlin
MPicker.takePhoto(this) { success, filePath, uri ->
    if (success) {
        // filePath / uri 为拍照结果
    }
}
```

### 直接录像并进入处理链

```kotlin
import io.github.mz.mpicker.api.CameraRecordTrigger

MPicker.with(this)
    .takeVideo()
    .recordDurationMs(10_000L, countDown = true)
    .recordTrigger(CameraRecordTrigger.CLICK)
    .smartVideoCompress()
    .start { result -> }
```

### 长按录像

```kotlin
MPicker.with(this)
    .takeVideo()
    .recordDurationMs(15_000L, countDown = true)
    .longPressRecord()
    .start { result -> }
```

### 列表首位相机入口

```kotlin
MPicker.with(this)
    .type(MediaType.IMAGE)
    .showCameraEntry(true)
    .photoMode()
    .start { result -> }
```

视频列表中使用录像入口：

```kotlin
MPicker.with(this)
    .type(MediaType.VIDEO)
    .showCameraEntry(true)
    .videoMode()
    .recordDurationMs(30_000L)
    .start { result -> }
```

### 前置摄像头镜像说明

部分设备前置摄像头录制的视频会带镜像效果。链式录像 API 会通过 `MediaEntity.mirrorHorizontal` 标记视频是否需要水平翻转。内置压缩器会尽量处理该信息；如果你使用自定义 `IVideoCompressor`，需要在转码绘制阶段判断并处理该字段，输出后建议把 `mirrorHorizontal` 置为 `false`。

独立 API `MPicker.takeVideo(activity, ...)` 只返回原始 `filePath/uri`，不返回 `MediaEntity`，因此不携带 `mirrorHorizontal`。如果需要镜像标记，请使用 `MPicker.with(this).takeVideo().start { ... }`。

## 高级扩展（高级）

### 媒体筛选

```kotlin
MPicker.with(this)
    .filter(MediaType.IMAGE) {
        addMimeType("image/jpeg", "image/png")
        minSizeBytes(20 * 1024)
    }
    .maxCount(9)
    .start { result -> }
```

视频时长过滤：

```kotlin
MPicker.with(this)
    .filter(MediaType.VIDEO) {
        maxDurationMs(60_000L)
    }
    .start { result -> }
```

### 预选回显

```kotlin
MPicker.with(this)
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
MPicker.preload(this, MediaType.IMAGE, MediaType.VIDEO)

val cachedImages = MPicker.cached(MediaType.IMAGE)

MPicker.invalidateCache()
```

建议在权限已就绪后预加载。拍照、保存新文件或外部媒体库变化后，可调用 `invalidateCache()` 强制刷新。

### 自定义图片加载

实现 `IImageEngine` 可以接入 Glide、Coil、Picasso，也可以按文件类型显示自定义封面。

```kotlin
import android.widget.ImageView
import io.github.mz.mpicker.loader.IImageEngine
import io.github.mz.mpicker.model.MediaEntity

class AppImageEngine : IImageEngine {
    override fun loadThumbnail(view: ImageView, item: MediaEntity) {
        // 示例：Glide.with(view).load(item.uri).into(view)
        // 也可以根据 item.mimeType / item.displayName 为 PDF、Word、ZIP 设置图标
    }

    override fun loadOriginal(view: ImageView, item: MediaEntity) {
        // 示例：Glide.with(view).load(item.uri).into(view)
    }
}

MPicker.setImageEngine(AppImageEngine())
```

### 自定义其他文件预览

当 `MediaType.ALL` 返回 PDF、Office、TXT、ZIP 等非媒体文件时，可以通过 `IOtherPreviewProvider` 接入自己的预览能力。

```kotlin
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.github.mz.mpicker.model.MediaEntity
import io.github.mz.mpicker.preview.IOtherPreviewProvider

MPicker.setOtherPreviewProvider(object : IOtherPreviewProvider {
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
import io.github.mz.mpicker.compress.CompressCallback
import io.github.mz.mpicker.compress.IImageCompressor
import io.github.mz.mpicker.model.MediaEntity

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

MPicker.with(this)
    .type(MediaType.IMAGE)
    .imageCompressor(AppImageCompressor())
    .start { result -> }
```

注意：自定义图片/视频压缩器必须调用 `callback.onSuccess(...)` 或 `callback.onError(...)`，否则压缩 loading 不会结束。

### 第三方裁切或编辑 SDK

如果内置裁切/编辑 UI 不满足需求，可以实现 `IImageProcessProcessor` 接入第三方 SDK。第三方处理完成后调用 `callback.onSuccess(result)`，MPicker 会继续执行压缩并从 `start {}` 返回最终结果。

```kotlin
MPicker.with(this)
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
MPicker.with(this)
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

- **优先使用 `uri`，不要强依赖 `filePath`。**
- SAF 或云端文件可能只有 `uri`，业务层需要用 `ContentResolver` 读取。
- 压缩、裁切、编辑后通常会返回新的 `uri/filePath/sizeBytes`。

## API 速查

### 基础选择（最常用）

| API | 说明 |
| --- | --- |
| `MPicker.with(activity)` | 创建 Activity 版选择器 |
| `MPicker.with(fragment)` | 创建 Fragment 版选择器 |
| `type(type)` | 设置媒体类型 |
| `maxCount(n)` | 最大选择数量 |
| `multiSelect(enable)` | 是否多选 |
| `grid(enable)` | 网格或列表模式 |
| `spanCount(n)` | 网格列数 |
| `preSelected(list)` | 预选回显 |
| `showFirstLoading(enable)` | 首次加载是否显示 loading |
| `start { result -> }` | 启动选择并返回结果 |
| `takePhoto()` / `takeVideo()` | 链式拍照 / 录像 |
| `useSystemPhotoPicker(enable)` | 系统 Photo Picker |
| `useSystemFilePicker(enable)` | SAF 文件选择器 |

### 裁切与编辑（进阶）

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

### 压缩（进阶）

| API | 说明 |
| --- | --- |
| `smartCompress(...)` | 本次选择启用内置图片压缩 |
| `smartVideoCompress(...)` | 本次选择启用内置视频压缩 |
| `imageCompressor(c)` | 本次选择使用自定义图片压缩器 |
| `videoCompressor(c)` | 本次选择使用自定义视频压缩器 |
| `cancelCompressOnBack(enable)` | 压缩 loading 期间是否允许返回取消 |
| `MPicker.setSmartImageCompressor(...)` | 全局启用内置图片压缩 |
| `MPicker.setSmartVideoCompressor(...)` | 全局启用内置视频压缩 |
| `MPicker.setImageCompressor(c)` | 全局设置图片压缩器；传 null 取消 |
| `MPicker.setVideoCompressor(c)` | 全局设置视频压缩器；传 null 取消 |

### 系统选择器与相机

| API | 说明 |
| --- | --- |
| `useSystemPhotoPicker(enable)` | API 33+ 优先使用系统 Photo Picker |
| `useSystemFilePicker(enable)` | 使用 SAF 文件选择器 |
| `MPicker.pickFiles(activity, mimeTypes, allowMultiple, listener)` | 独立 SAF 文件选择入口 |
| `MPicker.pickFiles(fragment, mimeTypes, allowMultiple, listener)` | Fragment 版 SAF 文件选择入口 |
| `MPicker.takePhoto(activity, listener)` | 独立拍照，不进入选择器 UI |
| `MPicker.takeVideo(activity, listener)` | 独立录像，不进入选择器 UI |
| `MPicker.takeVideo(activity, maxDurationMs, countDown, trigger, listener)` | 独立限时录像 |
| `showCameraEntry(enable)` | 列表首位显示相机入口 |
| `photoMode()` / `videoMode()` | 指定列表首位相机入口模式 |
| `recordDurationMs(durationMs, countDown)` | 设置录像最长时长和倒计时 |
| `clickRecord()` / `longPressRecord()` | 点击录像或长按录像 |

### 全局扩展（高级）

| API | 说明 |
| --- | --- |
| `MPicker.setTheme(theme)` | 设置全局主题 |
| `MPicker.setImageEngine(engine)` | 设置全局图片加载引擎 |
| `MPicker.setOtherPreviewProvider(provider)` | 注册其他文件预览扩展 |
| `MPicker.preload(context, vararg types)` | 预加载媒体首屏数据 |
| `MPicker.cached(type)` | 获取首屏缓存 |
| `MPicker.invalidateCache()` | 清空媒体缓存 |

## 混淆配置

库已内置 `consumer-rules.pro`，一般不需要额外配置。如果你接入了第三方图片加载、文档预览、裁切编辑或压缩 SDK，请按对应 SDK 文档添加规则。

如果业务侧通过反射访问 MPicker 的公开 API，可额外保留：

```proguard
-keep class io.github.mz.mpicker.api.MPicker { public *; }
-keep class io.github.mz.mpicker.model.MediaEntity { *; }
```

## FAQ

### 我是新手，该从哪里开始？

看 [三步上手](#三步上手黄金路径) 和 [选型决策表](#该用哪种方式选型决策表)。绝大多数需求用「选图 / 选视频 / 拍照」三个入口就够了。系统 Photo Picker / SAF 几乎不需要权限，适合想快速上架的场景。

### 为什么选择结果的 `filePath` 为空？

系统 Photo Picker、SAF、云端文件或部分厂商 ROM 可能只提供 `content://` Uri。建议业务侧**优先使用 `uri`**，并通过 `ContentResolver` 读取流。只有明确需要真实路径的场景才使用 `filePath`。

### 为什么库不自动声明权限？

媒体权限、相机权限、所有文件访问权限都可能影响上架审核和隐私披露。MPicker 选择让宿主 App 显式声明需要的权限，避免库隐式带入不必要权限。框架会在选择页明确提示「未声明权限」，不会静默失败。

### Android 13/14 应该优先用什么方式选图？

如果只是选图片/视频，优先考虑 `useSystemPhotoPicker(true)`，通常不需要读取媒体权限。如果需要统一网格 UI、文档混选、裁切编辑深度定制或自定义预览，可以使用框架内置选择器并按场景申请权限。

### `MediaType.ALL` 能替代文件管理器吗？

它适合在媒体选择器场景中混合展示常见媒体和文档文件，但依赖所有文件访问权限，合规成本高。如果你的业务是完整文件管理器或只是选几个文档，应优先使用 SAF（`MPicker.pickFiles`）或业务自有文件扫描能力，并谨慎处理 `MANAGE_EXTERNAL_STORAGE` 合规问题。

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

MPicker 使用 Apache License 2.0。详见 [LICENSE](LICENSE)。
