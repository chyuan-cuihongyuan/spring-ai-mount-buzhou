# Wayfinder Map — Buzhou 语义漂移触发压缩（effort #51，50 轮自迭代第 16 轮）

> effort #51，延续 #50（T341–T342 / impl-236）。主线：**#35 fog 项「语义漂移触发
> 压缩（embedding 真检测）」**——spec 70 的边界压缩只有积压计数判据；「话题已换」
> 这个更自然的边界信号缺席。Letta 语义触发压缩思想。

## Destination

`SemanticDriftDetector`（函数接口：当前输入 vs 摘要渲染文本 → 漂移？）+
`LexicalDriftDetector`（字符 bigram Jaccard 默认实现——零依赖，阈值 0.15 保守档，
空基准不判）；InjectionViewProcessor 在边界压缩判据处并列 driftTrigger（与积压
触发同路径同管线——增量摘要/事实对账/检查点全复用）；MemoryModule 键
`semantic-drift`（默认关）+ `semantic-drift-threshold`；可注入 embedding 版
（SPI 自由）。

## Notes

- 借鉴：Letta 语义触发压缩 / Phoenix 漂移检测；诚实边界：词面版召回有限，
  embedding 真检测走注入点（fog 不预设实现）。

## Decisions so far

- 与积压判据并联不互斥（任一命中即走边界压缩——两个信号两个场景）。
- 漂移判据需摘要基准（previous 非空才可比对——无基准不动作）。

## Not yet specified

- embedding 检测器默认装配（记忆模块接 EmbeddingModel——模型面另议）；漂移事件
  （memory.compacted payload 加 trigger 字段）。

## Out of scope

- 沿用 #7–#50；压缩策略本身变更（复用 spec 70 全管线）。

## Tickets

- [x] [T343 检测器 SPI + 词面实现 + IVP/MemoryModule 接线](../tickets/T343-semantic-drift.md)（impl-237）
- [x] [T344 4 例红队（漂移触发/同话题/默认零变化/词面判定面）+ 收口](../tickets/T344-drift-close.md)
