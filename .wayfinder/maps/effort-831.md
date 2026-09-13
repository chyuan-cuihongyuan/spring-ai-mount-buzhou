# effort #831 — 会话准入拒绝分布

- 会话：H 会话 800 系第 32 轮 ｜ spec [831](../../../docs/spec/831-spawn-rejection-distribution.md) ｜ 票 [T1163](../tickets/T1163-spawn-rejection-distribution.md)/[T1164](../tickets/T1164-spawn-rejection-distribution-verify.md) ｜ impl584
- 借鉴：k8s admission 拒绝读数扩散（kubernetes/kubernetes ≈115K；SpawnGate 拒绝事件聚合面）

## 勘察（排重）

- SpawnGate：拒绝以事件流（backpressure.spawn-rejected+reason）发出——无聚合分布。
- CapabilityDecisionAudit（700）：能力域审计——准入域缺位。
- grep -i `rejection|admission.*count`：无聚合命中。

## 决定

`SpawnRejectionDistribution`（core.backpressure，synchronized 记账）：record(reason, atMillis)——原因开集键封顶 16（超限不记+truncated）；count+lastSeen(max)+total；snapshot 计数降序+dominant（严格大于首达）；null/空白忽略；空报告。喂点=拒绝事件消费者装配侧。

## 测试

计数降序+dominant+lastSeen 取 max/封顶 16+超封顶不计数+truncated/null 空白忽略+空报告——3 例全绿。

## 诚实边界

原因开集由 SpawnGate 语义定义（本类不解释）；超封顶原因丢弃如实（truncated）；喂点手动不改 gate。
