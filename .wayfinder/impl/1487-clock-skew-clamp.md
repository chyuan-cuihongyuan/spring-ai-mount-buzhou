# impl 1487 — ClockSkewClamp 时钟偏斜校正（R87 = effort #1886 / spec 1886 / T2973-T2974）

**What**：`ClockSkewClamp`（core/observability 静态纯函数 + 嵌套
ClampedSpan）——clamp（负偏斜平移保持时长 → 终点越界收缩下限 0）
+ skewMillis 偏斜读数；子/父区间合法性双查 fail-fast。

**Why**：Zipkin/Brave 偏斜校正——跨机采集时钟漂移让观测树包含
关系失真，拓扑时序与关键路径读数被污染；「平移保时长、越界收缩」
两级钳位确定性串联。

**Verify**：`ClockSkewClampTest` 5 用例全绿（平移保持时长/内嵌零
改动/终点收缩/双越界串联/畸形 fail-fast）。

**Status**：done（2026-09-23）
