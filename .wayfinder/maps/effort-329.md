# Wayfinder Map — Buzhou API 快照收口（effort #329，C 会话第 30 轮）

> C 会话第 30 轮（半程点）。快照机有平台残缺：split(":") 在 Windows 切碎
> classpath、`\classes` 不匹配 `/classes`——门恒跳过、regenerate 写空文件；
> 公共面自 effort #143 起未入档（B 尾巴 + C R1–R29 攒了 38 型）。

## Destination

ApiSurfaceSnapshotTest 跨平台修复（File.pathSeparator + `\`→`/` 归一——
Linux 零变化、Windows 首次真比对/再生）→ regenerate 全量快照（+38 型）→
api-surface.md 补 C 会话节（efforts #300–#328）→ 比对测试本机真跑绿。

## Notes

- 号段：spec 329 / T649–T650 / impl-352。
- 契约：快照更新流程 = regenerate → 人工核对 diff → api-surface.md 同步
  入档（impl-179 立的规矩，本轮首次在 Windows 走通全流程）。

## Decisions so far

- 修复只动测试基建，不碰任何生产面；Linux 行为零变化（CI 幂等）。
- api-surface.md 以「一会话一大节（按主题分组）」入档，不逐 effort 切片
  （38 型逐轮切片信息密度太低）。

## Out of scope

- 方法级快照（类型级已够防漂移粗闸）；yml 键清单（随各 spec）。

## Tickets

- [x] [T649 快照机跨平台修复 + 全量再生](../tickets/T649-snapshot-fix.md)（impl-352）
- [x] [T650 api-surface.md C 会话节 + 验证收口](../tickets/T650-surface-doc.md)（impl-352）
