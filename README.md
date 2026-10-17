# ParcelHub 快递聚合

一款纯本地、不联网、无广告的 Android 取件码聚合 App。

## 功能

- **短信自动解析**：收到取件短信后自动提取取件码、驿站名、快递公司
- **本地列表**：待取 / 已取两栏，点卡片复制取件码
- **一键拉起各平台身份码**：
  - 淘宝 / 菜鸟驿站（tbopen 直达 H5）
  - 拼多多取件助手（pinduoduo scheme）
  - 支付宝菜鸟身份码（alipays scheme）
  - 顺丰（sf-express App，兜底微信小程序 `wxd4185d00bf7e08ac`）
  - 京东（openapp.jdmobile App，兜底微信小程序 `wx73247c7819d61796`）
- **零网络请求**：MVP 不申请 INTERNET 权限，所有数据存本地

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- MVVM（手写 ViewModel，无 DI 框架）
- SharedPreferences + JSON 持久化
- 最低支持 Android 8.0（API 26）

## 已知限制（MVP）

- 不查在途物流轨迹（V2 接 ALAPI）
- 不支持桌面 Widget（V2 加）
- 京东/顺丰的小程序 path 需要真机实测后补
- 短信解析基于规则，遇到奇葩签名可能漏识别

## 权限说明

- `RECEIVE_SMS` / `READ_SMS`：监听取件短信
- `POST_NOTIFICATIONS`：Android 13+ 通知权限

## 构建

本地：
```
gradle assembleDebug
```

云端：push 到 main 自动触发 GitHub Actions，从 Actions 页下载 `parcel-hub-debug-apk`。

## License

MIT
