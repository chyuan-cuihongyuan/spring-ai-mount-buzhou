# Spec 2041 — P 会话 R42 对账轮（effort #2041，R42）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3183–T3184，impl 1592）。
> R6k 对账轮第七例（Wave 7 收口）。

## Problem Statement

Wave 7（R37–R41）新增 5 个公共类型（HysteresisWatermark /
InSyncTracker / SimHashFingerprint / VersionRequirement / Freezable
Buffer）未入快照——skills 与 spill 两模块为本会话首入；R38/R39 等
push 中断积压待推。

## Solution

R6k 同款四件套：快照 1031→1036（+5 全 P 系）+ api-surface.md 五行 +
CONTEXT 930→935 + 全仓 mvn verify 三门全绿 + P 对账门核账（spec
2000–2041 卌二号四件套）+ push 补推全部积压。

## Further Notes

- 里程碑：42/150（28%）——P 系原语已覆盖 core/memory/guard/resilience/
  tools/mcp/skills/spill 八模块。
