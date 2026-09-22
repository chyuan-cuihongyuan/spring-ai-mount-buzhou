# impl 1501 — MergePressureReadout 合并压力读面（R101 = effort #1900 / spec 1900 / T3001-T3002）

**What**：`MergePressureReadout`（core/recovery 静态纯函数 +
Pressure 枚举）——pressure 占比读数 + verdict 三态（OK/WARN/REJECT
边界含上）+ shouldRejectInsert 硬阀独立安全阀；active≥0/上限≥1/
warnAt∈(0,1] fail-fast。

**Why**：ClickHouse MergeTree too-many-parts——写入碎片堆积到拒绝
那一刻才暴露；压力刻度让降频/加快合并的决策提前。与墓碑占比
（删除堆积）互补：这是写入碎片堆积。

**Verify**：`MergePressureReadoutTest` 4 用例全绿（占比/三态边界/
硬阀独立/畸形四型 fail-fast）。

**Status**：done（2026-09-23）
