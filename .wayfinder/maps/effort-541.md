# Wayfinder Map — Buzhou 双 judge 一致率（effort #541，E 会话第 41 轮）

> E 会话第 41 轮（516 校准的双 judge 变体轮）。勘察：judge 换版前后/
> 两个 judge 并用时，**间一致率**无计量（516 是 vs 金标准）——朴素一致
> 率被机遇一致虚高（两 judge 都 95% 判绿 → po=0.9+ 但毫无分辨力）。
> scikit-learn cohen_kappa_score 思想。

## Destination

`eval.JudgeAgreement`（纯函数——516 同型）：analyze(judgeA, judgeB) →
AgreementReport(bothRed/bothGreen/aRedOnly/bRedOnly/singleSided/po/pe/
κ)+strength 分级（Landis-Koch：>0.8 almost-perfect / 0.6-0.8 substantial
…）。二值化同 516（判红=正类）；单侧排除；pe≥1 或 n=0 → κ=null 诚实
空值。

## Notes

- 号段：spec 541 / T835-836 / impl-442。
- 借鉴源：scikit-learn cohen_kappa_score + Landis-Koch 分级。

## Out of scope

- 多 judge（Fleiss κ）；加权 κ（有序评分）。

## Tickets

- [x] [T835 Cohen κ 计算](../tickets/T835-judge-agreement.md)
- [x] [T836 Landis-Koch 分级](../tickets/T836-kappa-strength.md)
