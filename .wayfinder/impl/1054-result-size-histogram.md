# 1054 — 工具结果字节直方分桶读面

**What to build:** ToolResultSizeHistogram implements BuzhouHook（afterTool 单点记账：五幂次边界桶+溢出桶+三总量，UTF-8 口径）+ stats()/resetForTest() + E2E 三测。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ToolResultSizeHistogram（core/hook，opt-in，CONTINUE 零裁决）
- [x] ToolResultSizeHistogramTest（六尺寸落桶 E2E / 失败隔离 / reset）
- [x] spec 1401 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='ToolResultSizeHistogramTest'` 3/3 绿。
