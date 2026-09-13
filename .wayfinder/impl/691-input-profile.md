# 691 — 数据集输入长度画像读面

**What to build:** EvalDatasetStore.inputLengthProfile（count/total/avg/max/p95 画像）+ 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] inputLengthProfile + InputProfile record
- [x] InputProfileTest（统计精确/P95 插值/超长项显形/空集/未建 fail-fast）
- [x] spec 942 + README 行

## Done

验证：`mvn -pl buzhou-core test -Dtest=InputProfileTest` 全绿。commit 见本轮 `feat(core)` 提交。
