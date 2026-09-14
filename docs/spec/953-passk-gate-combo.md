# 953 — pass@k×防抖门组合补验

> 来源：I 会话第 52 轮 = effort #953（[T1331](../../.wayfinder/tickets/T1331-passk-gate-combo-shape.md) / [T1332](../../.wayfinder/tickets/T1332-passk-gate-combo-verify.md) / impl 694 续）。评估域三口径（频率门 80 / 无偏概率 902 / 稳定性 908）的组合语义收口。

## 背景

频率门（单次 passRate ≥ threshold）与 pass@k 概率口径对同数据可给出相反结论（单次 0.4 不达标 vs k 次采样概率达标）——两口径并存时需要组合语义测试固化「不矛盾、互补」。

## 目标

`PassAtKGateComboTest`：
1. 场景构造：n=4 采样中 2 pass（单次 passRate=0.5 恰不达 0.6 门）但 pass@2 = 1−C(2,2)/C(4,2) ≈ 0.833 达标——双结论并存的语义位置测试；
2. enforceStable 与 history 的复用一致性（k 次循环后 history 条数 == k）；
3. 纯测试轮零生产变更。

## 兼容性

纯测试轮。
