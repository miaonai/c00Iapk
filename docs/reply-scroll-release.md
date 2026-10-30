# 修复回复窗口导致动态页面跳动

修复打开回复楼主、回复评论或楼中楼回复窗口时，背景动态/评论列表意外滚动的问题。横屏回复和表情面板切换一并处理。

原因是回复输入框调用 `requestFocusFromTouch()`，使窗口退出触摸模式，背景列表的焦点变化触发自动滚动。现在使用输入框本身支持的 `requestFocus()`，保留键盘输入及正常的焦点行为。

验证与交付：

- 修复前的 Android 15 模拟器测试在六个场景中检测到背景位移，例如条目顶部偏移由 `-37px` 变为 `0px`，楼中楼由 `-53px` 变为 `0px`。[复现记录](https://github.com/miaonai/c00Iapk/actions/runs/41078856689)
- 发布流程要求 Android 11、15 的滚动回归测试、单元测试、Release 构建及签名校验全部通过。
- 回归测试使用离线背景列表和真实 `ReplyActivity`，覆盖横竖屏、三种回复场景、取消重开、再次滚动、页面顶部及表情/键盘切换；未覆盖所有厂商 ROM 和第三方输入法。
- APK 使用原有 GitHub Secrets 中的 Release 密钥签名，与 `v2026.10.18-comments` 保持相同签名，支持覆盖安装该版本。
- 下载 `c001apk-release.apk` 安装；`SHA256SUMS` 为文件校验值，`signature.txt` 和 `package.txt` 为签名及包信息。
