# 795 — 围栏裁决分布读面

**What to build:** SequenceFence 五桶 verdict 计数 + 嵌套 FenceVerdictStats + stats() + spec 303 判定矩阵全场景测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 五桶 verdict 计数（固定键有界）
- [x] judge 纯函数抽取 + observe 计数包装（语义逐位不变）
- [x] FenceVerdictStats 嵌套 record + stats()
- [x] FenceVerdictStatsTest（判定矩阵全场景/守恒/不可变/fresh 零值）
- [x] spec 1043 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-core test -Dtest='FenceVerdictStatsTest'` 全绿 + 既有围栏回归绿。commit 见本轮 `feat(core)` 提交。
