# impl 1513 — ProbeBudget 探测流量预算（R113 = effort #1912 / spec 1912 / T3025-T3026）

**What**：`ProbeBudget`（core/health 静态纯函数 + Verdict 枚举）——
probeShare（探测 QPS/容量 QPS 占比读数）+ verdict 两态（≤ maxShare
WITHIN 边界含上/ > OVER）；QPS 非负/capacity≥1/maxShare∈(0,1]
fail-fast。

**Why**：SRE 健康检查预算惯例——实例数上涨后探测 QPS 线性膨胀，
「探活变压死」事故反复；占比读数 + 预算判定让探测反噬事前可见。
与 TTL 探针状态机互补。

**Verify**：`ProbeBudgetTest` 3 用例全绿（占比两例/两态含上边界/
畸形三型 fail-fast）。

**Status**：done（2026-09-23）
