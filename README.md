<p align="center">
  <a href="https://github.com/NoMTF/HRT-Recorder/releases/latest">
    <img alt="立即下载 APK" src="https://img.shields.io/badge/%E7%AB%8B%E5%8D%B3%E4%B8%8B%E8%BD%BD-Android%20APK-f472b6?style=for-the-badge&labelColor=0f172a" />
  </a>
  <a href="https://github.com/NoMTF/HRT-Recorder/releases/latest">
    <img alt="查看发布页" src="https://img.shields.io/badge/%E6%9F%A5%E7%9C%8B-Release%20%E5%8F%91%E5%B8%83%E9%A1%B5-38bdf8?style=for-the-badge&labelColor=0f172a" />
  </a>
</p>

<p align="center">
  <img alt="HRT Recorder" src="https://capsule-render.vercel.app/api?type=waving&height=190&color=0:7dd3fc,55:ffffff,100:f9a8d4&section=header&text=HRT%20Recorder&fontColor=111827&fontSize=48&fontAlignY=42&animation=fadeIn" />
</p>

<p align="center">
  <b>把 HRT 记录留在自己手机里。</b><br />
  用药、计划、化验、药瓶、趋势图和分享图，都在本地完成。
</p>

<p align="center">
  <img alt="Android" src="https://img.shields.io/badge/Android-8.0%2B-7dd3fc?style=flat-square" />
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Compose-f9a8d4?style=flat-square" />
  <img alt="No account" src="https://img.shields.io/badge/No%20account-required-38bdf8?style=flat-square" />
  <img alt="No internet permission" src="https://img.shields.io/badge/No%20INTERNET-permission-f472b6?style=flat-square" />
</p>

---

## 下载

最新安装包放在 GitHub Releases：

<p>
  <a href="https://github.com/NoMTF/HRT-Recorder/releases/latest">
    <img alt="下载最新版" src="https://img.shields.io/badge/%E4%B8%8B%E8%BD%BD%E6%9C%80%E6%96%B0%E7%89%88-HRT%20Recorder%20APK-f472b6?style=for-the-badge&labelColor=111827" />
  </a>
</p>

| 项目 | 内容 |
| --- | --- |
| 应用名 | HRT Recorder |
| 包名 | `com.nanxin.hrtrecorder` |
| 当前版本 | `2.1.22` |
| 最低系统 | Android 8.0 / API 26 |
| 目标系统 | Android 15 / API 35 |

---

## 这是什么

HRT Recorder 是一个 Android 本地记录工具。它关注三件事：

| 方向 | 能做什么 |
| --- | --- |
| 记录 | 快速记录用药、计划、提醒、药瓶库存 |
| 观察 | 查看 E2、抗雄、T 的趋势曲线与当前估算 |
| 留档 | 保存化验、导入导出 JSON / CSV / HTML、生成分享图 |

它不需要账号，不接入云同步，也不会把记录交给远程服务保存。  
数据在你的设备里，导出与分享由你自己决定。

---

## 功能一览

| 模块 | 内容 |
| --- | --- |
| 概览 | 当前估算、趋势曲线、状态卡、分享图片 |
| 计划 | 每日/每周计划、本地提醒、系统提醒同步 |
| 记录 | 用药历史、日历视图、编辑、删除、兼容导入 |
| 化验 | E2 化验、个体化校准、CI、baseline、异常点诊断 |
| 罩杯 | 离线罩杯参考计算器与分享图 |
| 药瓶 | 库存、保质期、补满、自动扣减 |
| 设置 | 语言、主题、备份、隐私协议、关于 |

---

## 隐私原则

- 不需要登录。
- 不申请网络权限。
- 不上传用户数据。
- 不接入广告、统计 SDK、远程配置或云同步。
- 本地导出的 JSON / CSV / HTML / 图片由用户自行保存和管理。
- 提醒功能使用 Android 本地通知与系统提醒/日历 Intent。

公开隐私政策与用户协议：  
<https://orange-truth-08b4.guhuao666.workers.dev/>

---

## 使用边界

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
| APK | `app/build/outputs/apk/sideload/release/app-sideload-release.apk` |
| AAB | `app/build/outputs/bundle/playRelease/app-play-release.aab` |

正式签名文件不会进入仓库。发布前请在本地创建 `keystore.properties` 并配置自己的 keystore。

---

## 参考

以下项目、资料和网站作为文件层、代码层与说明层参考来源：

- Journey: <https://x.com/m1zukiqaqaqaq>
- HRT-Recorder-online: <https://github.com/LaoZhong-Mihari/HRT-Recorder-online>
- Transmtf-HRT-Tracker: <https://github.com/TransmtfTeam/Transmtf-HRT-Tracker>
- HRT-Recorder-PKcomponent-Test: <https://github.com/LaoZhong-Mihari/HRT-Recorder-PKcomponent-Test>
- hrt.mahiro.uk: <https://hrt.mahiro.uk/>
- MtF-wiki cup calculator: <https://mtf.wiki/zh-cn/cup-calculator>
- MtF-wiki repository: <https://github.com/project-trans/MtF-wiki>

更多说明见 [REFERENCES.md](./REFERENCES.md) 与 [Algorithm Explanation.md](./Algorithm%20Explanation.md)。

---

## 授权边界

本仓库当前未附加公开许可证。代码公开用于审阅、学习与协作参考；再分发、商用、改名发布或二次上架请先取得明确许可。
