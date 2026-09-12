# 460 — 黄金轨迹 payload 归一化

**What to build:** EventSequenceAssert 增 `assertPayloadNormalized` + 静态 `normalizePayload/normalizeValue`（五类哨兵、深层递归、键保序）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 归一化断言 + 公开静态工具（test-jar 面）
- [x] 3 用例绿（哨兵/嵌套/端到端）
- [x] spec 607 + README 行

## Done

验证：`mvn -pl buzhou-core test -Dtest=EventSequenceAssertNormalizationTest` 绿（3/3）。commit 见本轮 `test(core)` 提交。
