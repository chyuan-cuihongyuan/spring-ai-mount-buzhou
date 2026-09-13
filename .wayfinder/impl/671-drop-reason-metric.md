# 671 — 丢弃计数 reason 维度指标

**What to build:** DROP_REASON_* 常量统一三处字面量 + 双轨指标（总量保留 + dropped-reason 带 tag）+ 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 六常量抽出 + countDrop 双轨指标
- [x] 测试（reason tag 序列存在性/常量值域/breakdown 键同源）
- [x] spec 918 + README 行（欠账累计 906–918 十三行）

## Done

验证：`mvn -pl buzhou-core test -Dtest=EventDropBreakdownTest,EventDropBreakdownConcurrencyTest` 全绿。commit 见本轮 `feat(core)` 提交。
