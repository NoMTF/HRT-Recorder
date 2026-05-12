# References

Android 版作者：南盺

本文件记录 HRT Recorder 在实现过程中参考过的项目、资料和公开页面。它们只作为文件层、代码层与说明层参考来源，不作为应用 UI 中的作者署名。

## Project References

- Journey: <https://x.com/m1zukiqaqaqaq>
- HRT-Recorder-online: <https://github.com/LaoZhong-Mihari/HRT-Recorder-online>
- Transmtf-HRT-Tracker: <https://github.com/TransmtfTeam/Transmtf-HRT-Tracker>
- HRT-Recorder-PKcomponent-Test: <https://github.com/LaoZhong-Mihari/HRT-Recorder-PKcomponent-Test>
- hrt.mahiro.uk: <https://hrt.mahiro.uk/>
- MtF-wiki cup calculator: <https://mtf.wiki/zh-cn/cup-calculator>
- MtF-wiki repository: <https://github.com/project-trans/MtF-wiki>

## Algorithm And Product Notes

- E2 基础曲线和多药物结构参考了 hrt.mahiro.uk 与 HRT-Recorder-PKcomponent-Test 中可复现的工程参数。
- E2 化验录入、个体化校准、置信区间、baseline、异常点与收敛诊断参考了 Transmtf-HRT-Tracker 的功能思路，并由 Android 端重新实现。
- CPA 口服曲线采用公开药品说明书中常见的 `50 mg -> 约 140 ng/mL，约 3 小时达峰` 锚点，并以 Bateman 曲线做本地趋势估算。
- 其它抗雄数据以记录、计划、提醒和药瓶库存为主；在缺少足够可靠药代曲线时不会强行混入 CPA 曲线。
- 罩杯模块参考 MtF-wiki 的输入思路和尺码区间，由 Android 端离线实现。

## Boundaries

- 不新增 `INTERNET` 权限。
- 不接入登录、云同步、统计 SDK、广告 SDK 或远程配置。
- 不把趋势估算、参考区间、药瓶库存或提醒功能包装成医疗建议。
- 不把参考来源写成用户可见作者。

