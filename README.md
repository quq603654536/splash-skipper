# 开屏快跳 / Splash Skipper

面向小米澎湃 OS 3 / Android 16 的独立安卓开屏广告跳过工具。使用 Android 无障碍 API 识别按钮并模拟点击，不修改其他应用的 APK、签名或网络流量。

当前版本：1.0.3。本仓库提供应用源码、构建脚本和测试；安装包请按下方说明自行构建。第三方应用 APK、签名密钥和设备诊断数据不随源码发布。

## 使用

1. 按下方说明构建并安装 `dist/splash-skipper-1.0.3.apk`，打开“开屏快跳”。
2. 点击“设置无障碍服务”，在系统的已下载应用中手动开启“开屏快跳”。系统详情入口受限时会自动回退到无障碍总设置。
3. 澎湃系统建议开启后台自启动，将省电策略设为无限制，并在最近任务中锁定“开屏快跳”卡片。设置入口受系统版本影响，必要时从应用信息手动进入。上述设置可减少后台清理影响，不能保证服务永不被系统结束。
4. 默认对可启动应用生效，可在“应用”页逐项排除。系统设置、权限控制器、安装器、桌面等固定排除。
5. “增强识别”默认关闭。开启后可识别没有广告标记的右上角“跳过”，但可能跳过首次引导。
6. 可在系统快捷设置面板手动添加“开屏快跳”磁贴，用于暂停或恢复。

## 范围与限制

- 只在检测到前台应用切换后的 12 秒内扫描；识别到后至多执行一次跳过点击。
- 包含系统标记为“不重要”的控件，以兼容部分广告 SDK；窗口尚未切换完成时会在识别时间内重试。熄屏和锁屏时不执行点击。
- 同时观察窗口变化与应用 Activity 恢复；同一应用反复进入前台也会重新启动识别，不依赖桌面一定发送包名事件。
- 默认综合按钮文字、位置、大小、倒计时、广告标记和控件 ID 判断，不匹配泛化的“关闭”“同意”“确定”按钮。
- 发现隐私政策、用户协议、验证码等敏感界面特征时停止扫描。
- 优先点击控件或小范围可点击父控件。控件不支持直接点击时，可在用户开启的“兼容点击”模式下对已识别的按钮模拟触摸。
- 完全用图片/游戏画面绘制、无跳过入口、出现过晚、系统隐藏无障碍节点等场景可能无法识别。不能保证覆盖所有应用或所有广告。
- 默认策略有意保守；应用更改按钮文字或结构后可能需要调整规则。

## 本地数据与权限

- 不申请 `INTERNET`、存储、联系人、短信或位置权限。
- 无障碍服务用于读取当前界面与执行点击；由用户在系统设置中手动开启，可随时关闭。
- 只保留最近 100 条应用名称、包名、时间和点击方式；不保存页面正文、截图、账号、密码或验证码。
- 首页计数表示已执行点击，不等于每次都已成功关闭广告。
- 应用列表通过可启动应用查询获取，不申请 `QUERY_ALL_PACKAGES`。

## 构建（macOS）

使用 Java 17+、Python 3.9+ 和 Google 官方 Android SDK 工具，不依赖第三方 Android 库。当前构建已在 Java 21 上验证。SDK 引导脚本下载 macOS 版本的构建工具；Windows / Linux 构建尚未验证。

```sh
git clone https://github.com/quq603654536/splash-skipper.git
cd splash-skipper
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
python3 bootstrap.py
python3 build.py
```

`bootstrap.py` 下载固定版本的 Android 36 平台及 Build Tools 36.0.0，并按 Google 仓库发布的校验值检查下载内容。产物在 `dist/`。首次构建会在 `.signing/` 生成本地签名密钥；保留它才能覆盖升级同一个应用。签名密码仅保存在权限为 0600 的本地文件，不包含在源码归档中。

请先安装 JDK 21；也可将 `JAVA_HOME` 手动设置为本机 Java 17+ 的 JDK 目录。构建脚本保留原开发环境的 GoLand JDK 作为默认值，其他机器应显式设置 `JAVA_HOME`。`tools/`、`build/`、`dist/`、`.signing/` 和测试编译输出均已加入 `.gitignore`。

## 验证

纯 Java 规则测试：

```sh
mkdir -p tests/out
"$JAVA_HOME/bin/javac" -d tests/out app/src/main/java/com/wkm/splashskip/SkipMatcher.java app/src/main/java/com/wkm/splashskip/LaunchSession.java tests/MatcherTest.java tests/SessionTest.java
"$JAVA_HOME/bin/java" -cp tests/out MatcherTest
"$JAVA_HOME/bin/java" -cp tests/out SessionTest
```

本地设备测试：

```sh
python3 build.py --fixture
adb install -r dist/splash-skipper-fixture.apk
# 先在手机上手动开启主应用的无障碍服务
python3 tests/device_test.py DEVICE_SERIAL
adb uninstall com.wkm.splashskip.fixture
```

测试包括倒计时按钮、有广告标记的普通跳过、非可点击控件的手势兼容、首次引导不误点、隐私页面不误点。结果写入 `build/device-test-results.json`。测试页面不联网，无真实广告，也不操作第三方登录或支付。

## 源码

- `SkipMatcher.java`：可独立测试的匹配策略。
- `SkipService.java`：前台会话、限时扫描、控件点击与手势回退。
- `LaunchSession.java`：可独立测试的前台恢复与识别时间窗口。
- `MainActivity.java`：权限状态、澎湃设置入口、应用范围与记录。
- `Store.java`：本地设置、排除列表与环形记录。
- `PauseTileService.java`：快捷设置暂停磁贴。
- `fixture/`：只用于测试的模拟页面，测试结束后卸载。
