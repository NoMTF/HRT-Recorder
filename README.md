# HRT Recorder

> 本地优先、离线可用的 Android HRT 记录与趋势可视化工具。

**作者：南盺**  
**包名：** `com.nanxin.hrtrecorder`  
**当前版本：** `2.1.22`  
**平台：** Android 8.0+，面向 Android 15 构建

HRT Recorder 的目标很简单：让需要记录 HRT 时间线的人，可以在 Android 手机上快速、清楚、离线地管理自己的记录，而不依赖网站账号、云同步或远程服务。数据默认保存在本机，应用不申请 `INTERNET` 权限，不接入统计 SDK，不上传用户数据。

本应用不是医疗器械，不提供诊断、治疗、处方、剂量建议或临床决策。所有估算图表只用于个人记录回顾和趋势参考；任何健康、用药、检查结果或治疗相关决定，请咨询合格医疗专业人员。

## Highlights

- **本地离线**：无账号、无云端、无网络权限。
- **多药物记录**：支持雌二醇、CPA、睾酮与常见抗雄记录。
- **趋势曲线**：E2 / 抗雄 / T 分析物分轴显示，避免不同单位混画。
- **化验校准**：支持化验结果录入、E2 个体化校准、置信区间、baseline、异常点和收敛度诊断。
- **计划与提醒**：支持每日/每周用药计划、本地提醒、系统提醒同步与执行状态记录。
- **药瓶库存**：支持创建药瓶、库存扣减、补满、保质期和剩余量显示。
- **数据导入导出**：支持 JSON / CSV / HTML 报告，兼容 hrt.mahiro.uk 常见备份结构。
- **分享图片**：可生成本地分享图，不依赖外部链接。
- **罩杯模块**：内置离线罩杯参考计算器。
- **隐私协议**：应用内置完整用户与隐私协议，并提供公开网页版本。

## Screens

HRT Recorder 采用 Kotlin + Jetpack Compose 原生实现，主视觉为粉、蓝、白的清爽高对比界面。核心页面包括：

- `概览`：当前估算值、曲线、状态卡、分享图。
- `计划`：计划分组、今日任务、提醒同步。
- `记录`：用药历史、日历记录、导入数据。
- `化验`：化验结果、校准诊断、模型状态。
- `罩杯`：离线尺码参考计算。
- `药瓶`：库存、保质期、补充与扣减。
- `设置`：语言、主题、导入导出、隐私协议与关于页面。

## Build

需要 Android SDK、JDK 17。根目录执行：

```powershell
.\gradlew.bat :app:assembleSideloadRelease
.\gradlew.bat :app:bundlePlayRelease
```

默认产物：

| 类型 | 路径 |
| --- | --- |
| Sideload APK | `app/build/outputs/apk/sideload/release/app-sideload-release.apk` |
| Play AAB | `app/build/outputs/bundle/playRelease/app-play-release.aab` |

正式签名文件不会进入仓库。发布前请在本地创建 `keystore.properties` 并配置自己的 keystore。

Release 构建启用 R8 与资源收缩，用于减少包体和降低二次打包风险。公开仓库中的源码不会被“加密”；如果你获取的是源码，请以仓库实际内容为准。

## Privacy

- 不需要登录。
- 不申请网络权限。
- 不上传用户数据。
- 不接入广告、统计、远程配置或云同步。
- 本地导出的 JSON / CSV / HTML / 图片由用户自行保存与管理。
- 提醒功能使用 Android 本地通知与系统提醒/日历 Intent。

公开隐私政策与用户协议：  
<https://orange-truth-08b4.guhuao666.workers.dev/>

## References

Android 版作者为 **南盺**。以下项目、资料和网站仅作为文件层、代码层与说明层参考来源，不作为本应用 UI 作者署名：

- Journey: <https://x.com/m1zukiqaqaqaq>
- HRT-Recorder-online: <https://github.com/LaoZhong-Mihari/HRT-Recorder-online>
- Transmtf-HRT-Tracker: <https://github.com/TransmtfTeam/Transmtf-HRT-Tracker>
- HRT-Recorder-PKcomponent-Test: <https://github.com/LaoZhong-Mihari/HRT-Recorder-PKcomponent-Test>
- hrt.mahiro.uk: <https://hrt.mahiro.uk/>
- MtF-wiki cup calculator: <https://mtf.wiki/zh-cn/cup-calculator>
- MtF-wiki repository: <https://github.com/project-trans/MtF-wiki>

更多参考见 [REFERENCES.md](./REFERENCES.md) 与 [Algorithm Explanation.md](./Algorithm%20Explanation.md)。

## Usage Boundary

本仓库暂未选择公开许可证。代码公开用于审阅、学习与协作参考；再分发、商用、改名发布或二次上架请先取得作者明确许可。
