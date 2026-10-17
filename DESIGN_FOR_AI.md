# ParcelHub 项目思路与技术方案（给 AI 看的交接文档）

## 一、项目背景

做一款 Android App，把分散在各电商/物流平台的取件信息聚合到一处：
- 自动读取取件短信，提取取件码
- 一键拉起淘宝/拼多多/菜鸟/顺丰/京东的身份码或取件页
- 纯本地、不联网、无账号、无广告

## 二、核心痛点与解法

### 痛点 1：取件码散落在短信里
- 解法：监听 `SMS_RECEIVED` 广播，用规则引擎解析
- 解析三级策略：
  1. 引导词匹配：`取件码[为/是/：]XXX`
  2. 通用码形正则：`字母数字-字母数字-字母数字`（如 `3-2-501`）
  3. 快递公司签名识别（顺丰/中通/圆通/韵达/极兔/京东/菜鸟/丰巢…）

### 痛点 2：身份码（各平台动态条码）没法直接调
- 淘宝/京东/顺丰/拼多多的"身份码"是 App 内动态条形码，无公开 API
- **已验证可行的方案**：不是逆向，而是用公开的 Deep Link 把各 App 拉到对应页面

## 三、各平台拉起方案（已实测/待实测）

### 淘宝/菜鸟驿站身份码 ✅ 已验证（参考 shareven/parcel 开源项目）
```kotlin
val h5 = "https://pages-fast.m.taobao.com/wow/z/uniapp/1100333/last-mile-fe/m-end-school-tab/home"
val tbopen = "tbopen://m.taobao.com/tbopen/index.html?h5Url=${URLEncoder.encode(h5)}"
```

### 拼多多取件助手 ✅ 已验证
```
pinduoduo://com.xunmeng.pinduoduo/psnl_goods_help.html?_t_module_name=assistant_pickup
```

### 支付宝菜鸟身份码 ✅ 已验证
```
alipays://platformapi/startapp?appId=20000067&url=<URL编码后的 authCode.html>
```

### 顺丰 🟡 待真机验证
- 优先试 `sf-express://`（顺丰自家 App）
- 兜底微信小程序：`weixin://dl/business/?appid=wxd4185d00bf7e08ac`

### 京东 🟡 待真机验证
- 优先试 `openapp.jdmobile://`（京东自家 App）
- 兜底微信小程序：`weixin://dl/business/?appid=wx73247c7819d61796`

## 四、兜底策略

每个平台拉起都按三层 try-catch：
1. 先试直达 scheme
2. 失败则 `getLaunchIntentForPackage` 拉起 App 首页
3. 再失败 Toast 提示"未安装"

## 五、技术栈

- Kotlin + Jetpack Compose + Material 3
- MVVM（AndroidViewModel，无 DI）
- 本地存储：SharedPreferences + JSON
- minSdk 26 / targetSdk 35
- 不申请 INTERNET 权限

## 六、已知坑

1. GitHub Actions：别用 `android-actions/setup-android@v3`，直接用 runner 自带 SDK
2. 小米/华为需要手动在设置里开"短信监听"权限
3. 微信小程序明文 scheme 需要小程序后台开启，顺丰/京东未必开