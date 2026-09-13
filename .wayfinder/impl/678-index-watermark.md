# 678 — 会话索引存量水位读面

**What to build:** InMemorySessionIndexStore.watermark() internal 读面 + 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] Watermark record + watermark()
- [x] IndexWatermarkTest（条目数变化/无界语义/零行为变化）
- [x] spec 925 + README 行（欠账累计 906–925 二十行）

## Done

验证：`mvn -pl buzhou-core test -Dtest=IndexWatermarkTest` 全绿。commit 见本轮 `feat(core)` 提交。
