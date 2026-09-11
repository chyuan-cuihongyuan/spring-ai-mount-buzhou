# Wayfinder Map — Buzhou 评估 A/A 抖动检测（effort #513，E 会话第 14 轮）

> E 会话第 14 轮。勘察：评估闭环有 run 落盘/对比（81 EvalRunDiff 四态
> 迁移）/门（80）/指纹（82/93）——**A/A 检测**（同数据集同版本跑两遍，
> 逐项 verdict 翻转 = 抖动项）无命名语义：对比器的 REGRESSION 在 A/A
> 语境其实是「抖动」，REGRESSION/FIX 方向语义在 A/A 下不存在。HELM/
> 工业 A/A test 思想：先测评估系统自身的稳定性再谈版本对比。

## Destination

`eval.EvalFlakinessDetector`（纯函数——EvalRunDiff 同型不触 store）：
`analyze(runA, runB)` → FlakinessReport（compared/flakyItems[itemId+
statusA+statusB]/flakyRate/driftItems——单侧项是数据集漂移不算抖动）。
红绿语义：pass=绿、fail/error=红（321 错误从严同口径——error 不当
pass 折算）；抖动 = 同项红绿翻转（方向不区分——A/A 无方向）。
消费面：host 同指纹数据集跑两遍 → analyze → 抖动清单进 quarantine/
标记；与 80 EvalGate 组合（抖动率超阈 fail）宿主侧一行。

## Notes

- 号段：spec 513 / T777–T778 / impl-416。
- 借鉴源：HELM/工业 A/A test（先验评估系统稳定性）；语义分层复用 81
  状态对齐法（不造第二对齐器——按 status 红绿映射独立实现方向无关版）。
- 诚实边界：两 run 才能判抖动（单 run 无法区分稳定红与抖动）；不自动
  重跑 k 次（k 次留宿主循环）。

## Out of scope

- k 次重复自动编排；duration 抖动（时延方差——108/111 timer 面）；
  自动 quarantine 执行。

## Tickets

- [x] [T777 红绿映射与抖动判定](../tickets/T777-aa-flakiness-analyze.md)
- [x] [T778 报告面与漂移隔离](../tickets/T778-aa-flakiness-report.md)
