# 39 — guard · 审计链持久化 + 密钥版本化轮换 + 独立校验

**What to build:** 审计能力默认在线且可运维：审计记录 append-only 持久化（JDBC 表 + InMemory 有界环形），随 guard 自动装配（无密钥降级哈希链 + WARN）；签名密钥版本化（keyVersion 嵌记录、rotate 原子切换、旧钥只验不签、minVerifyVersion、文件加载 KeyProvider）；独立校验工具（全量重放 + VerificationReport 定位首断点）；sessionHash 随会话收尾发布。

**Blocked by:** 29（分类与日志）

**Status:** ready-for-agent

- [ ] AuditRecordStore SPI（JDBC append-only 表 + InMemory 有界环形）+ guard 自动装配接线（buzhou.guard.audit.enabled）
- [ ] SigningKeyRing：keyVersion 嵌记录、rotate()、旧钥只验不签、minVerifyVersion、KeyProvider（PKCS#8 PEM 文件路径配置）
- [ ] 无密钥启动：降级纯哈希链 + WARN（不阻断）
- [ ] AuditChainVerifier：记录集 + KeyRing → VerificationReport{verifiedCount, firstBreakIndex, brokenRecordId, keyVersionStats}
- [ ] sessionHash 随会话收尾发布；nightly 重放校验（注记级接入既有 nightly 节奏）
- [ ] 单测：轮换后旧记录仍可验、新记录用新钥、篡改任一记录 verify 定位断点、跨重启链可验（持久化）
