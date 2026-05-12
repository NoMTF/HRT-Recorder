# Antiandrogen PK Reference Notes

Android 版作者：南盺

抗雄相关曲线只用于趋势可视化。它们不表示药效强弱，不表示受体阻断程度，不表示睾酮抑制百分比，也不能替代医疗判断。

## CPA

CPA 是当前抗雄曲线中唯一直接绘制为血药浓度趋势的对象。实现锚点：

- reference dose: `50 mg`
- reference Cmax: `140 ng/mL`
- reference Tmax: `3 h`
- terminal half-life used by model: about `43.9 h`

因此，`12.5 mg` 口服 CPA 的峰值会按近似线性剂量缩放到约 `35 ng/mL`。这与 Androcur 说明书常见数据更一致，也避免旧模型在图表采样时出现不自然的直线上冲。

## Record-only antiandrogens

以下药物目前主要用于记录、计划、提醒和药瓶库存，不混入 CPA 曲线：

- Spironolactone
- Bicalutamide
- Finasteride
- Dutasteride

不画曲线不代表没有药理作用，只代表当前没有采用足够稳妥的本地血药浓度模型。

## Source Layer References

- DailyMed spironolactone label: <https://dailymed.nlm.nih.gov/>
- DailyMed bicalutamide label: <https://dailymed.nlm.nih.gov/>
- DailyMed finasteride label: <https://dailymed.nlm.nih.gov/>
- DailyMed dutasteride / Avodart label: <https://dailymed.nlm.nih.gov/>
- Cyproterone acetate public SmPC / monograph values for oral peak and half-life.
- Transmtf-HRT-Tracker: <https://github.com/TransmtfTeam/Transmtf-HRT-Tracker>

## Limits

- 不模拟药效、受体亲和力、雄激素抑制百分比或激素反馈。
- 不根据图表给出“应该吃多少”的建议。
- 所有数值都是工程参考，服务于离线记录与趋势显示。

