# 831 — Spill 加解密读面

**What to build:** SpillCipher 静态四计数（encryptCalls/encryptFailures/decryptCalls/decryptFailures）+ 嵌套 SpillCipherStats + stats()/resetForTest() + 加解密/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 四计数落点（加解密入口与 catch 处）
- [x] SpillCipherStats 嵌套 record + stats() + resetForTest()
- [x] SpillCipherStatsTest（往返/归零等测）
- [x] spec 1079 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-spill -am test -Dtest='SpillCipherStatsTest'` 全绿 + 既有 SpillCipher 回归绿。commit 见本轮 `feat(spill)` 提交。
