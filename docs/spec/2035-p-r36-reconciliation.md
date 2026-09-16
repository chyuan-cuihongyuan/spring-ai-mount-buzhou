# Spec 2035 — P 会话 R36 对账轮（effort #2035，R36）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3171–T3172，impl 1586）。
> R6k 对账轮第六例（Wave 6 收口）。

## Problem Statement

Wave 6（R31–R35）新增 5 个公共类型（RedirectBudget / RequiredChecks
Rollup / Ucb1Selector / MaintenanceTrigger / ToolProvenanceIndex）未入
快照——其中 mcp/tools 两模块为本会话首入。

## Solution

R6k 同款四件套：快照 1026→1031（+5 全 P 系）+ api-surface.md 五行 +
CONTEXT 925→930 + 全仓 mvn verify 三门全绿 + P 对账门核账（spec
2000–2035 卅六号四件套）+ push。

## Further Notes

- Wave 6 特点：首次覆盖 buzhou-tools 与 buzhou-mcp 两模块（provenance
  新子包）——P 系原语域从 core/memory/guard/resilience 扩到六模块。
