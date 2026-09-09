# Spec 404 — 审计 Merkle 根与包含证明（effort #404）

> wayfinder map：`.wayfinder/maps/effort-404.md`（T699–T700）。D 会话第 5 轮。

## Problem Statement

审计链验证是全链线性的：第三方想确认「某条记录确实在链上」必须拿到
全部记录——审计数据必须整份外发才能自证，既大又泄漏面广；链生长后
「截至某时点链上有什么」没有可对外发布的紧凑承诺。

## Solution

`guard.audit` Merkle 化（Certificate Transparency 借鉴）：

- **`AuditMerkleTree.of(List<AgentAuditRecord>)`**：不可变快照树。
  叶摘要 = sha256Hex(Jcs.canonicalize(record.unsignedMap()))——与链
  prev_hash **同摘要基**（同一记录两种证明路径互证）；层内奇数复制
  末叶（Bitcoin 式）；空树根 = sha256("")（与链创世同值）。
- **`InclusionProof`**（recordId、leafIndex、steps[{siblingHex,
  siblingOnRight}]、rootHex）；`AuditMerkleTree.proof(recordId)` 按需
  出证。
- **静态 `verify(leafHex, steps, rootHex)`**：第三方只凭叶摘要+证明+
  已发布根即可验包含——全链不必离开本方。
- **`AuditChain.sealMerkle()`**：时点封印（`AuditMerkleSeal`：
  sealedAt、recordCount、rootHex）——快照式（链照常生长，印描述
  「截至此刻」）；有界保留最近 32 印；`merkleSeals()` 只读。

## User Stories

1. 作为审计方，我想只凭已发布根与单条证明验证记录包含，所以 不需要
   拿到全链数据。
2. 作为安全负责人，我想定期封印得到紧凑根值对外发布，so 链内容事后
   不可整批篡改而不被发现。
3. 作为宿主，我想叶摘要与链 prev_hash 同基，所以 两种证明路径可以
   互证交叉核对。

## Implementation Decisions

- Bitcoin 式奇数叶复制（实现简单、业界惯例）。
- 封印有界 32（内存纪律；更早的印归宿主自行持久化）。

## Testing Decisions

- 根确定性；每片叶的证明都能 verify；篡改叶/换根 → verify false；
  奇数叶树；空树根 = sha256("")；链 seal → proof → verify 回路 +
  封印后追加记录不影响旧印。

## Out of Scope

- 签名树头（STH）；定时自动封印；印对外发布通道；持久化封印。

## Further Notes

- 新公共类型 `AuditMerkleTree`（含嵌套 `InclusionProof`/`ProofStep`）+
  `AuditMerkleSeal` 随轮 regenerate 快照。
