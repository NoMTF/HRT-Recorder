# Algorithm Explanation

Android 版作者：南盺

HRT Recorder 的药代曲线是本地趋势估算模型，不是医疗器械算法。它只服务于个人记录回顾、图表可视化和数据整理，不能用于诊断、治疗、处方、剂量调整或临床决策。

## Architecture

- `DoseEvent` 记录一次用药事件：药物类别、路线、时间、剂量、化合物与附加字段。
- `PkEngine.kt` 将事件转换为 E2 / CPA / Testosterone 三类分析物曲线。
- `CalibrationEngine.kt` 处理 E2-only 化验校准、EKF / OU-Kalman、置信区间、baseline 与异常点诊断。
- `ResultChart.kt` 负责图表绘制与点选预览，不改变算法结果。
- JSON 导入兼容不代表算法完全相同；导入外部数据后仍由 HRT Recorder 本地模型计算。

## Estradiol

雌二醇曲线覆盖常见 E2、EB、EV、EPP、EC、EN、EU 以及注射、口服、舌下、凝胶、贴片等路线。模型使用轻量的一室/多室工程近似，用于在手机端快速绘制趋势。

E2 化验校准只作用于 E2 曲线，不会让 CPA、T 或其它抗雄参与 E2 个体化校准。

## CPA

CPA 口服曲线采用公开说明书锚点：

```text
50 mg oral CPA -> about 140 ng/mL near 3 h
12.5 mg oral CPA -> about 35 ng/mL peak by approximate linear scaling
```

实现上使用 Bateman 曲线，并反推吸收常数使峰值时间接近 3 小时。这样能避免旧模型在吸收段出现“平地起惊雷”式的垂直尖峰。

非 CPA 抗雄不会混入 CPA 曲线。比卡鲁胺、螺内酯、非那雄胺、度他雄胺主要作为记录、计划、提醒和药瓶库存对象。

## Testosterone

Testosterone 曲线覆盖 T、TC、TE、TU 的主要路线。相关工程参数参考 HRT-Recorder-PKcomponent-Test 与 hrt.mahiro.uk 的可复现结构。当前不做 T 化验校准。

## Calibration

E2 校准通过用户录入的化验结果重放个体化模型，输出：

- baseline E2
- 校准倍率
- 68% / 95% 置信区间
- 异常点提示
- NIS / residual 等诊断量
- 收敛度参考

校准结果用于更贴近个人记录的趋势显示，仍不构成医疗建议。

## Import / Export

- 默认 JSON 导出包含 events、labResults、plans、bottles、settings 等本地数据。
- hrt.mahiro.uk 常见 JSON 可导入，尽量保留时间、体重、剂量、路线、模板和化验结果。
- CSV / HTML 报告用于用户自主管理数据。

## Limits

- 不模拟受体效应、激素反馈、肝肾功能、蛋白结合差异或个体代谢极端情况。
- 不提供治疗目标、剂量建议或用药安全判断。
- 任何估算值都应被理解为“记录工具中的趋势参考”。

