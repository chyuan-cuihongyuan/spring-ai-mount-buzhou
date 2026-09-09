# Spec 344 — 审计链完整性巡检（effort #344）

> wayfinder map：`.wayfinder/maps/effort-344.md`（T679–T680）。C 会话第 45 轮。

## Problem Statement

guard 健康面（GuardHealth）只验证审计存储**可读**：链被篡改（哈希
断链、签名失效）时健康面依旧 UP——可读 ≠ 完整。审计被改是最高级
事故（事实源失守），却没有任何面会红。

## Solution

`AuditChainHealth`（guard，BuzhouHealth 机制名 `guard-audit-chain`）：

- **status() 巡检**：`loadAll` → `AuditChainVerifier.verify`——
  断链 **DOWN**（details：firstBreakIndex/brokenRecordId/breakReason/
  headHash/keyVersionStats——事后篡改可被证明并定位）；干净 UP
  （details：verifiedCount/headHash）。
- **超窗 UNKNOWN**：链长超巡检窗（默认 10,000 条，防健康探针变全表
  扫描）报 UNKNOWN 带修法——「链长超巡检窗，全量校验归宿主手动
  loadAll+verify」；**宁诚实降级不做段校验**（段首位断链语义模糊）。
- 空链 UP（无事发生不是病）；审计关闭/无存储/无签名密钥 UNKNOWN
  （降级运行不是 DOWN——GuardHealth 同口径）。
- 装配挂 BuzhouGuardHealthAutoConfiguration（auditEnabled + store +
  SigningKeyRing 齐备才装配；indicator 子配置同 GuardHealth）。
- DOWN 自动流入 312 告警（yml 配 guard-audit-chain 规则即得）与
  332 探针裁决——健康面聚合既有管道，零新管道。

## User Stories

1. 作为安全负责人，我想审计链被篡改时健康面红，所以 事实源失守
   不会被「存储可读」的绿掩盖。
2. 作为审计者，我想断链定位到首个断点（id/reason），所以 事后取证
   有起点。
3. 作为运维，我想超长链不拖垮健康探针（超窗 UNKNOWN 带修法），所以
   健康轮询不被全表扫描劫持。
4. 作为运维，我想 312 告警规则直接可配 guard-audit-chain，所以 断链
   即通知无需新管道。
5. 作为使用者，审计关闭时该面 UNKNOWN 不 DOWN，所以 降级运行不被
   误报事故。

## Implementation Decisions

- 只读探针（verify 不改链）；InMemory 环形天然有界（窗上限对 JDBC
  更关键）。
- 巡检窗常量默认 10,000（构造可覆写——测试用小窗）。

## Testing Decisions

- 干净链 UP / 断链 DOWN 且 details 定位 / 空链 UP / 超窗 UNKNOWN /
  审计关 UNKNOWN / 装配（齐备装配、缺 keyRing 不装配）。
- 先例：GuardHealth 测试手法（InMemory store + 真实 AuditChain append）。

## Out of Scope

- 后台定时校验（status() 按需——健康轮询天然周期化）；段校验；
  告警升级（330 族）；InMemory 环形滚出窗的补侦（JDBC 链无此限）。

## Further Notes

- 新公共类型 `AuditChainHealth` 随轮 regenerate 快照。
