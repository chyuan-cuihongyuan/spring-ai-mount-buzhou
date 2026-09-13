# effort #712 — span 状态分布读数（⚠ R21=effort#720 修正：core 重建撞 spec 543 同名类，已删并收敛 healthSummary 到 analytics 类——教训：排重 grep 必须 -i 且按类名后缀查）

- 会话：G 会话 700 系第 13 轮 ｜ spec [712](../../../docs/spec/712-span-status-distribution.md) ｜ 票 [T1024](../tickets/T1024-span-status-dist.md)/[T1025](../tickets/T1025-span-status-dist-verify.md) ｜ impl612
- **换题注记**：原计划「webhook 签名双密钥轮换」勘察撞 spec 540（verifyWithRotation 双密钥+容差窗组合已收口——教训：排重 grep 须 -i，verifyWith**R**otation 大写漏检）。换入池内「span 状态分布读数」（已验证零命中）。
- 借鉴：OpenTelemetry（CNCF，trace 规范事实标准）span status 语义——状态分布是健康第一读数

## 勘察（排重）

- SpanStatus 常量（RUNNING/OK/ERROR/CANCELLED）+ObservabilityStore.spansOfSession 齐备；E 会话池「span 状态分布」未做（grep statusDistribution 零命中）。
- 509 是时延 SLO（错误预算语义）——非 span 状态面。

## 决定

`SpanStatusDistribution`（core.observability 纯函数）：of(List<SpanRecord>)→Report——(kind,status) 聚合计数（kind/status 字典序）+total+**runningResidue**（RUNNING 残留=未关闭 span 泄漏信号）+errorRate（ERROR/total，total=0 诚实 0.0）。null fail-fast。

## 测试

混合状态聚合计数/kind×status 二维分布/RUNNING 残留与错误率/空表与 null。

## 诚实边界

纯读数（告警归 312 规则订阅）；不拉取不存储（调用方供 spans——queries 归 dashboard 面）；CANCELLED 计入 total 不计 errorRate 分母误差（口径=ERROR/total 显式）。
