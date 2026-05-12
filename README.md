<p align="center">
  <img alt="HRT Recorder" src="https://capsule-render.vercel.app/api?type=rounded&height=180&color=0:bae6fd,48:ffffff,100:fbcfe8&text=HRT%20Recorder&fontColor=111827&fontSize=46&desc=Offline-first%20HRT%20timeline%20and%20trend%20viewer&descAlignY=70" />
</p>

<p align="center">
  <a href="https://github.com/NoMTF/HRT-Recorder/releases/latest">
    <img alt="Release" src="https://img.shields.io/github/v/release/NoMTF/HRT-Recorder?style=for-the-badge&label=Release&color=f9a8d4&labelColor=eff6ff" />
  </a>
  <img alt="Android" src="https://img.shields.io/badge/Android-8.0%2B-7dd3fc?style=for-the-badge&labelColor=ffffff" />
  <img alt="Offline" src="https://img.shields.io/badge/Offline-first-fbcfe8?style=for-the-badge&labelColor=ffffff" />
  <img alt="No Internet Permission" src="https://img.shields.io/badge/No%20INTERNET%20permission-bae6fd?style=for-the-badge&labelColor=ffffff" />
</p>

<p align="center">
  <b>多功能，本地，离线。</b><br />
  一个 Android HRT 记录、计划、库存和趋势可视化工具。
</p>

---

## 下载安装

前往 GitHub Release 下载最新 APK：

<p>
  <a href="https://github.com/NoMTF/HRT-Recorder/releases/latest">
    <img alt="Download APK" src="https://img.shields.io/badge/Download-APK-f472b6?style=for-the-badge&labelColor=0f172a" />
  </a>
</p>

| 项目 | 内容 |
| --- | --- |
| 应用名 | HRT Recorder |
| 包名 | `com.nanxin.hrtrecorder` |
| 当前版本 | `2.1.22` |
| 最低系统 | Android 8.0 / API 26 |
| 目标系统 | Android 15 / API 35 |
| 技术栈 | Kotlin + Jetpack Compose |

---

## 它解决什么

HRT Recorder 的目标不是做一个新的账号系统，也不是把私人数据交给远程网站保存。

它更像一个随身的本地记录本：

| 你需要 | HRT Recorder 提供 |
| --- | --- |
| 不想依赖网站账号 | 本地数据、本地计算、离线可用 |
| 想快速记录用药 | 用药记录、计划、提醒、药瓶库存 |
| 想看趋势 | E2 / 抗雄 / T 分析物趋势图 |
| 想保存化验 | 化验结果、E2 校准、诊断信息 |
| 想备份数据 | JSON / CSV / HTML 导出 |
| 想保护隐私 | 无登录、无云同步、无统计 SDK |

---

## 核心功能

| 模块 | 功能 |
| --- | --- |
| 概览 | 当前估算值、趋势曲线、状态卡、分享图片 |
| 计划 | 每日/每周计划、本地提醒、系统提醒同步 |
| 记录 | 用药历史、日历记录、编辑、删除、导入 |
| 化验 | E2 化验、个体化校准、CI、baseline、异常点诊断 |
| 罩杯 | 离线罩杯参考计算器 |
| 药瓶 | 库存、保质期、补满、自动扣减 |
| 设置 | 语言、主题、导入导出、隐私协议、关于 |

---

## 数据与隐私

<p>
  <img alt="Local data" src="https://img.shields.io/badge/Data-local%20only-bae6fd?style=flat-square" />
  <img alt="No account" src="https://img.shields.io/badge/Account-not%20required-fbcfe8?style=flat-square" />
  <img alt="No analytics" src="https://img.shields.io/badge/Analytics-none-7dd3fc?style=flat-square" />
</p>

- 不需要登录。
- 不申请网络权限。
- 不上传用户数据。
- 不接入广告、统计、远程配置或云同步。
- 本地导出的 JSON / CSV / HTML / 图片由用户自行保存与管理。
- 提醒功能使用 Android 本地通知与系统提醒/日历 Intent。

公开隐私政策与用户协议：  
<https://orange-truth-08b4.guhuao666.workers.dev/>

---

## 重要边界

HRT Recorder 不是医疗器械，不提供诊断、治疗、处方、剂量建议或临床决策。  
所有估算图表只用于个人记录回顾和趋势参考。任何健康、用药、检查结果或治疗相关决定，请咨询合格医疗专业人员。

---

## 构建

需要 Android SDK 与 JDK 17。

```powershell
.\gradlew.bat :app:assembleSideloadRelease
.\gradlew.bat :app:bundlePlayRelease
```

| 类型 | 路径 |
| --- | --- |
| Sideload APK | `app/build/outputs/apk/sideload/release/app-sideload-release.apk` |
| Play AAB | `app/build/outputs/bundle/playRelease/app-play-release.aab` |

正式签名文件不会进入仓库。发布前请在本地创建 `keystore.properties` 并配置自己的 keystore。

Release 构建启用 R8 与资源收缩，用于减少包体并降低二次打包风险。

---

## 参考来源

参考来源：

- Journey: <https://x.com/m1zukiqaqaqaq>
- HRT-Recorder-online: <https://github.com/LaoZhong-Mihari/HRT-Recorder-online>
- Transmtf-HRT-Tracker: <https://github.com/TransmtfTeam/Transmtf-HRT-Tracker>
- HRT-Recorder-PKcomponent-Test: <https://github.com/LaoZhong-Mihari/HRT-Recorder-PKcomponent-Test>
- hrt.mahiro.uk: <https://hrt.mahiro.uk/>
- MtF-wiki cup calculator: <https://mtf.wiki/zh-cn/cup-calculator>
- MtF-wiki repository: <https://github.com/project-trans/MtF-wiki>

更多参考见 [REFERENCES.md](./REFERENCES.md) 与 [Algorithm Explanation.md](./Algorithm%20Explanation.md)。

---

## 使用边界

本仓库暂未选择公开许可证。代码公开用于审阅、学习与协作参考；再分发、商用、改名发布或二次上架请先取得明确许可。

<p align="center">
  <sub>HRT Recorder · pink, blue, white · offline-first Android app</sub>
</p>

