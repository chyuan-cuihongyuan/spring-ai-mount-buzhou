# Wayfinder Map — Buzhou 影子读探针（effort #213，B 会话第 36 轮）

> B 会话第 36 轮。金丝雀（spec 48）把<b>部分流量</b>切到候选模型看结果；反过来
> 的需求也常见——<b>全量走主模型</b>，同时按采样把同样的调用喂给影子模型对照，
> 不影响响应。借鉴 service mesh 的 mirror/shadow traffic（Istio mirror）。

## Destination

ShadowProbe（resilience/fallback）：probe(key, primarySupplier 已返回值,
shadowCallable, executor)——确定性哈希采样（同 key 同判定）；命中采样异步执行
影子、结果对照（文本等值/hash）记 diverged/agreed 计数；影子异常吞（旁路
永不影响主路）。snapshot 观测。

## Notes

- 号段：B=奇数 spec（本轮 189）；轮次 .wayfinder200+。
- 与金丝雀（48）正交：那是切流，这是旁路对照；与 A/B eval（71）正交：那是
  离线 run，这是生产在线影子。

## Decisions so far

- 采样确定性（hash(key)%100 < rate）——同 key 稳定命中/不命中。

## Not yet specified

- 对照明细导出（JSONL）；延迟对照（不只是内容）。

## Out of scope

- 沿用各轮；影子结果回注主路；自动切流。

## Tickets

- [x] [T561 ShadowProbe（确定性采样+异步旁路对照）](tickets/T561-shadow-probe.md)（impl-308）
- [x] [T562 影子回归（采样命中/一致/分歧/异常吞/不采样零成本）](tickets/T562-shadow-tests.md)（impl-308）
