# 775 — 加密消息存储操作计数读面

**What to build:** EncryptingMessageStore encrypted/decrypted/passthrough 三计数 + 嵌套 CryptoStoreStats + stats() + 双向透传计数测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数埋点（toCarrier/fromCarrier 单点）
- [x] CryptoStoreStats 嵌套 record + stats()
- [x] EncryptingMessageStoreStatsTest（append/load 计数/旧明文透传/已信封跳过）
- [x] spec 1022 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-core test -Dtest='EncryptingMessageStoreStatsTest,EncryptingMessageStoreTest'` 全绿。commit 见本轮 `feat(core)` 提交。
