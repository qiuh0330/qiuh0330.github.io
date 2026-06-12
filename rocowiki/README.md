# 果冻工具 Android App (rocowiki)

原生 Android 应用（Kotlin + Jetpack Compose），数据与 `../roco` 网页版同源：
JSON 数据来自 `../roco/json`，精灵图片来自 `../roco/image`，全部打包进 APK，完全离线可用。

- 包名：`com.qiuh0330.gdtools`
- 最低支持 Android 7.0 (API 24)
- 底部导航：精灵图鉴 / 图鉴任务 / 精灵蛋组 / 收集情况

## 功能

- **精灵图鉴**：搜索（名称/编号），按时期、属性、蛋组筛选；点击查看详情
  （种族值七维（高低值绿/红标色）、特性、进化链、升级/学习/血脉技能；技能描述中的术语可点击查看释义）
- **图鉴任务**：按精灵分组的任务清单，勾选记录进度，支持搜索与按完成状态筛选
- **精灵蛋组**：按身高体重查蛋（带可能性评分）、查询同蛋组精灵、按蛋组浏览全部精灵
- **收集情况**：全部精灵按图鉴编号排序，支持名称/蛋组筛选，每只 9 类收集标记（炫彩/异色/性格 ×7）

## 与网页版的数据互通

任务进度和收集记录的存储与导出格式跟网页版 localStorage 完全一致：

- App 内「📤 导出」→ JSON 保存到手机「下载」目录，可在网页版「📥 导入进度/记录」恢复；
- 网页版导出的 JSON 同样可以在 App 内导入（旧版 `task_x_y` 进度格式也会自动转换）。

## 构建

工程不复制网页资源，`app/build.gradle.kts` 通过 `assets.srcDir` 直接引用 `../roco`，
必须保持仓库目录结构完整。本地工具链（JDK 17 + Gradle 8.9 + Android SDK 34）在
`.toolchain/`（已 gitignore），构建命令：

```powershell
$env:JAVA_HOME = "$PWD\.toolchain\jdk-17.0.19+10"
.\.toolchain\gradle-8.9\bin\gradle.bat :app:assembleRelease
```

也可以直接用 Android Studio 打开本目录构建。

产物：`app/build/outputs/apk/release/app-release.apk`（debug 签名，仅供个人安装使用）。
