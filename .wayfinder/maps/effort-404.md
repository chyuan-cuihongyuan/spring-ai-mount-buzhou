# Wayfinder Map — Buzhou 审计 Merkle 根与包含证明（effort #404，D 会话第 5 轮）

> D 会话第 5 轮。勘察（2026-09-08）：guard.audit 有哈希链（AuditChain）
> + 签名/钥环 + 全链巡检（344 AuditChainVerifier）——但验证是**全链线性**
> 的：第三方想确认「某一条记录确实在链上」必须拿到全部记录。CT log 的
> 核心武器 Merkle 化完全缺失（grep merkle 零命中）。

## Destination

`guard.audit` Merkle 化（Certificate Transparency 借鉴——发布根、按需
出证明）：`AuditMerkleTree.of(records)`（叶 = sha256(JCS canonical
unsignedMap)——与链链接同摘要基；奇数层复制末叶 Bitcoin 式）+
`InclusionProof`（兄弟哈希+方向位列表）+ 静态 `verify(leafHex, steps,
rootHex)`（第三方零全链验证）+ `AuditChain.sealMerkle()`（时点封印：
recordCount+rootHex+sealedAt，有界保留 32 印）——根可对外发布，单条
记录凭证明+根即可验，全链不必离开本方。

## Notes

- 号段：spec 404 / T699–T700 / impl-377。
- 借鉴源：Certificate Transparency（12k★ 生态标准）STH（树头签名）+
  inclusion proof；Bitcoin 式奇数叶复制。
- 纪律：叶摘要与链 prev_hash 同基（同一记录两种证明路径互证）；封印
  是快照不是移动（链照常生长，印描述「截至此刻」）。

## Out of scope

- 签名树头（STH 签名——链已带签名，根签名待真需求）；定时自动封印
  （宿主调度按需 seal——cron 语义归宿主）；封印对外发布通道（webhook
  族扩散候选）；JDBC/Redis 持久化封印。

## Tickets

- [x] [T699 AuditMerkleTree + 包含证明](../tickets/T699-audit-merkle-tree.md)
- [x] [T700 链封印 API + 验证回路](../tickets/T700-merkle-seal.md)
