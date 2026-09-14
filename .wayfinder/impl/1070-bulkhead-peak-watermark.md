# 1070 — 舱壁在飞峰值水位读面

**What to build:** AgentBulkhead 增量（per-agent 峰值表 256 折叠+recordPeak 采样+peakInFlight/peakSaturation）+ 四测。

**Blocked by:** None.

**Status:** done

- [x] AgentBulkhead 峰值水位（internal 扩展，无新公共类型——快照面不变）
- [x] AgentBulkheadPeakTest 四测
- [x] spec 1417 + README 行

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='AgentBulkheadPeakTest,AgentBulkheadTest,AgentBulkheadResizeTest'` 14/14 绿。
