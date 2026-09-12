# effort #744 — 混合排序融合权重读数

- 会话：G 会话 700 系第 45 轮 ｜ spec [744](../../../docs/spec/744-hybrid-ranker-readout.md) ｜ 票 [T1090](../tickets/T1090-hybrid-ranker-readout.md)/[T1091](../tickets/T1091-hybrid-ranker-readout-verify.md) ｜ impl645
- 借鉴：—（638「声明生效确认面」同型；605 混合排序深化）

## 勘察（排重）

- 605 HybridSkillRanker 加权构造+semanticFallbackCount——**权重本身**无读数（yml 声明 2:1 是否生效一无所知）；RRF 融合完成次数无计数。
- 原「EvalRunDurationStats」构思撞 spec 544（同名类 p50/p95 分布已做）——弃。

## 决定

HybridSkillRanker 加 `semanticWeight()`/`lexicalWeight()` 读数+`fusedCount()`（两路齐备完成 RRF 融合次数——语义降级单路不计）。638「声明是否生效」同型确认面。

## 测试

2:1 构造→getter 精确/rank 一次 fusedCount+1/降级不计（语义 bypass 路径）。
