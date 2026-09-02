# 课程表 ClassSchedule

一个为大学生写的安卓课程表应用：**Kotlin + Jetpack Compose（Material 3）**，纯本地存储，不申请任何权限，无广告、无联网。

## 功能

- **周视图课表**：左右滑动切换周次；点顶部「第X周」弹出选周面板；「今」一键回到本周；今天的列高亮
- **大学式排课**：每门课设置 星期 / 节次（第X~Y节）/ 周次范围（第X~Y周）/ 每周 · 单周 · 双周
- **多时段课程**：同一门课分散在不同周次段（如 1-8 周和 11-16 周）时，分别添加多条即可
- **冲突检测**：保存前自动检查与现有课程的时间重叠（可选择仍要保存）
- **学期设置**：开学日期（自动对齐周一）、总周数、每天节数、第一节课时间与课间，自动推算每节课钟点
- **课程颜色**：默认按课程名自动分配稳定颜色，也可手动挑选

## 云端编译 APK（推荐：本机无需安装任何开发工具）

1. 注册并登录 [GitHub](https://github.com)
2. 新建一个仓库（如 `ClassSchedule`），把本项目**全部文件**上传（网页拖拽上传即可；`.github` 文件夹必须上传，云端编译靠它触发）
3. 上传完成后，仓库的 **Actions** 页签会自动开始构建（首次约 3~6 分钟）
4. 构建完成后点进该次运行记录，页面底部 **Artifacts** 区域下载 `ClassSchedule-debug-apk`（zip 包，解压得到 `app-debug.apk`）
5. 把 apk 传到手机安装（允许"未知来源/安装未知应用"）

> 说明：debug 签名的 APK 是完整可用的安装包，个人使用没有问题。

## 本地编译（可选）

1. 安装 Android Studio（自带 Android SDK 与 JDK）
2. 打开本项目，等待 Gradle 同步完成
3. USB 连接手机（开启开发者选项 + USB 调试），点 Run

国内网络下载 Gradle 慢时，把 `gradle/wrapper/gradle-wrapper.properties` 的 `distributionUrl` 换成腾讯镜像：

```
https://mirrors.cloud.tencent.com/gradle/gradle-8.9-bin.zip
```

## 技术说明

- 最低支持 Android 8.0（API 26）
- 数据保存在 SharedPreferences（JSON），不上传任何服务器；卸载或清除数据会删除课程，请留意
- 代码结构：

```
app/src/main/java/com/classschedule/app/
├── MainActivity.kt      # 入口与页面切换
├── data/
│   ├── Models.kt        # 课程/学期模型、周次计算、冲突检测
│   └── Store.kt         # 本地 JSON 存储
└── ui/
    ├── Theme.kt         # 主题与课程色板
    ├── Components.kt    # 通用组件（步进器等）
    ├── ScheduleScreen.kt   # 周视图主界面
    ├── CourseEditScreen.kt # 添加/编辑课程
    └── SettingsScreen.kt   # 学期与时间设置
```

## 致谢

交互与数据模型设计参考了以下开源项目：[NexioSchedule](https://github.com/HaoZai000/NexioSchedule)、[sleepy](https://github.com/lingion/sleepy)、[CourseSchedule](https://github.com/cakeni/CourseSchedule)、[WakeupSchedule_BUPT](https://github.com/xianfei/WakeupSchedule_BUPT)。
