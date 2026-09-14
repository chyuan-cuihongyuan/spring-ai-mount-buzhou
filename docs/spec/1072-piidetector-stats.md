# 1072 — PII 检测引擎读面

> 来源：J 会话第 72 轮 = effort #1072（[T1599](../../.wayfinder/tickets/T1599-piidetector-stats-shape.md) / [T1600](../../.wayfinder/tickets/T1600-piidetector-stats-verify.md) / impl 824）。借鉴：Yara 规则引擎统计（引擎原生匹配数与业务上报数双层分离对账）。guard/pii 域第二轴（引擎原语层）。

## Problem Statement

`PiiDetector`（PiiType 规则引擎：scan 检测原语 + pseudonymize 假名化）零计数——引擎层与业务层（PiiHitStats）口径分离后，**引擎原生匹配数与业务上报数的对账缺口**不可见：规则匹配了但业务没上报（类型过滤/上游丢弃）时，引擎侧无任何信号。

## 目标

- `PiiDetector` 增量（guard/pii，静态面）：四 `AtomicLong`。
  - `scanCalls`：scan 入口计数；`scansWithHits`：命中非空的扫描数（弱校验分子）；
  - `matchesFound`：原生匹配总数（dedupe 前口径——引擎原语，与业务 HitStats 上报数对账）；
  - `pseudonymizeCalls`：假名化调用数。
- 嵌套 `record PiiDetectorStats(long scanCalls, long scansWithHits, long matchesFound, long pseudonymizeCalls)` + `stats()` + `resetForTest()`。
- 口径诚实：scanCalls 与 scansWithHits 弱校验非硬守恒（空文本/纯命中分布差异）。

## 兼容性

纯增量读面：scan/pseudonymize 返回语义、去重叠与格式保留脱敏逐位不变；静态面理由同 R46–R71 先例；无新配置项。

## Out of Scope

- 按 PiiType 分桶（PiiHitStats 业务侧已覆盖）。
- pattern 匹配耗时（另轴）。
