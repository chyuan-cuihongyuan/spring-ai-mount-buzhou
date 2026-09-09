# Wayfinder Map — Buzhou 审计链完整性巡检（effort #344，C 会话第 45 轮）

> C 会话第 45 轮。guard 健康面只探「审计存储读得到」（GuardHealth）——
> 链被篡改（哈希断链/签名失效）时健康面仍 UP：可读 ≠ 完整。证书透明
> CT log / Bitcoin 链的思想：链完整性要定期验证，断链是最高级事故
> （审计被改 = 事实源失守）。

## Destination

`AuditChainHealth`（guard，机制名 guard-audit-chain）：status() 时
loadAll → AuditChainVerifier.verify——断链 DOWN（details 定位首个断点
id/reason + headHash + keyVersionStats）；链长超巡检窗（默认 10k）报
UNKNOWN 带修法（全量校验归宿主手动——诚实边界）；空链 UP（无事发生
不是病）；审计关闭/无存储 UNKNOWN。装配挂 guard 健康自动配置
（SigningKeyRing 缺席 = UNKNOWN 无签名密钥是降级运行不是 DOWN——
GuardHealth 同口径）。

## Notes

- 号段：spec 344 / T679–T680 / impl-367。
- 借鉴源：Certificate Transparency 一致性证明 / 区块链全节点验证。
- 纪律：只读探针（校验不改链）；DOWN 自动进 312 告警与 332 探针裁决
> （健康面聚合既有管道）。

## Decisions so far

- 超窗 UNKNOWN 而非截断校验（段校验首位断链语义模糊——宁诚实降级）。

## Out of scope

- 定期后台校验（status() 按需——健康轮询天然周期化）；告警升级策略
  （330 族）；窗内篡改但窗已滚出（InMemory 环形——JDBC 持久链无此限）。

## Tickets

- [x] [T679 AuditChainHealth 巡检判定](../tickets/T679-chain-health.md)
- [x] [T680 装配 + 收口](../tickets/T680-chain-assembly.md)
