适配 coolapk-desktop v1.36.0–v1.36.1 使用的酷安请求协议。

- 将 API 请求升级为 Token V3（bcrypt cost=4），同步新版客户端版本参数。
- 一级评论统一通过 api.coolapk.com 请求，保留排序、只看楼主及分页游标。
- 自动注册并持久保存合成数盟设备 ID，游客和各登录账号分别保存，优先保留手动配置。
- GitHub Actions 运行单元测试、构建 Release APK，并通过 apksigner 验证签名；附带 SHA-256 校验文件。

使用新生成的独立签名。若已安装版本证书不同，Android 将拒绝覆盖安装。请先通过应用内备份功能保存需要的数据，再卸载旧版并安装。

当前开发网络访问酷安内容 API 返回空的 HTTP 403；协议签名、设备注册和请求参数分别验证，尚未在登录手机上完成评论浏览实测。

协议参考：https://github.com/coolapk-desktop/coolapk-desktop/tree/v1.36.1/src-tauri/src/coolapk
