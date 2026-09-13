# 659 — 工具耗时火焰图数据面

**What to build:** ToolGraphAnalyzer.timings 纯函数（TOOL 子集父子树 + self/cumulative 分解 + 稳定排序）+ ToolTimingProfile record + 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] timings()：TOOL 过滤/树构建（环防护）/self-cumulative 分解/稳定排序
- [x] ToolTimingProfile record
- [x] ToolFlameTimingTest（树分解/根处理/环防护/RUNNING 计 0/排序稳定/既有零回归）
- [x] spec 906 + README 行

## Done

验证：`mvn -pl buzhou-observability -am test` 全绿。commit 见本轮 `feat(observability)` 提交。
