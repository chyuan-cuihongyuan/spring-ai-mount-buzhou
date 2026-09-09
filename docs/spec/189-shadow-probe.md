# Spec 189 — 影子读探针（effort #213）

> wayfinder map：`.wayfinder/maps/effort-213.md`（T561–T562）。借鉴：Istio mirror
> （shadow traffic）——生产请求旁路镜像到候选，对照不打扰主路。

## Problem Statement

候选模型/新 prompt 版本上线前要验证「同输入下输出是否一致/可接受」：金丝雀
（48）切流验证会改变部分用户的响应；离线 A/B（71）数据集可能脱离真实分布。
影子读补第三条路：全量用户走主模型，同时采样镜像到影子——零用户影响的真实
分布对照。

## Solution

`ShadowProbe`（resilience/fallback）：

- **采样**：`sampled(key)` —— 确定性（sha256(key) % 100 < ratePercent）——
  同 key 稳定命中（回归可比）。
- **旁路**：`probe(key, primaryResult, shadowCall, executor)`——命中采样才
  异步执行影子；对照 primary 与影子结果（String 等值判断）记
  `agreed/diverged/sampled` 计数 + `snapshot()`（含最近分歧样本 key——有界
  环形 32）。
- **安全**：影子异常/超时全吞（旁路永不影响主路——影子 submit 即忘，异常
  记 error 计数）；未命中采样零执行零成本。

## User Stories

1. 作为宿主，换模型前挂 5% 影子一周——diverged 率即真实分布下的行为差异面。
2. 作为运维，最近分歧 key 样本可直接回放定位——不用翻全量日志。
3. 作为用户，完全无感（主路响应零变化零延迟——影子异步）。

## Implementation Decisions

- 对照为文本等值（语义评价归 eval 域——诚实边界）。
- snapshot 分歧样本环形 32（有界）。

## Testing Decimals

- 确定性采样（同 key 同判定；rate=0 全不采/rate=100 全采）；一致计 agreed；
  分歧计 diverged 且样本入环形；影子异常吞计 error 主路无感；异步不阻塞主路。

## Out of Scope

- 语义级对照；自动切流；对照明细 JSONL。

## Further Notes

- 验证三路：金丝雀切流（48）/ 离线 A/B（71）/ 在线影子（本轮）。
