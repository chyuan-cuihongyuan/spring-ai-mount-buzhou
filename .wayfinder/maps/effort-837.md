# effort #837 — 限流键热点读数

- 会话：H 会话 800 系第 39 轮（补位轮：R38 跳号致 effort 837 缺位——本轮即填补，effort 连续性恢复，G 会话 spec745 缺位教训的即时应用）｜ spec [837](../../../docs/spec/837-ratelimit-key-hotspot.md) ｜ 票 [T1177](../tickets/T1177-ratelimit-key-hotspot.md)/[T1178](../tickets/T1178-ratelimit-key-hotspot-verify.md) ｜ impl591
- 借鉴：Envoy per-connection rate limit 键域观测思想（Envoy proxy ≈25K）

## 勘察（排重）

- GCRA/ModelRateLimiter：额度执行——键域热力缺位。
- TagCardinalityGuard：指标标签基数——限流键域互补。
- grep -i `hotspot|hot.*key`：无命中。

## 决定

`RateLimitKeyHotspot`（resilience.ratelimit，纯读数）：record(key, amount, atMillis)——键封顶 128 超限并入 __overflow__ 桶（跨域口径一致）；requests/amountSum（×1000 毫账避免 double CAS）/lastSeen(max)+totalRequests/distinctKeys+top(n) requests 降序典序破平；null/空白/负额忽略。键=模型|维度组合由调用方拼。喂点=ModelRateLimiter 策略层（不改 backend SPI）。

## 测试

top 排序+额度毫账累计 30.0 精确+lastSeen/溢出桶入账 distinct 128+1/脏入参三形态+top 边界空真——3 例全绿。

## 诚实边界

键拼装语义归调用方（本类不拆维度）；amount 单位由调用方定（RPM/TPM 混布需分键）；溢出桶不可回溯（有界取舍）；喂点手动不改 SPI。
