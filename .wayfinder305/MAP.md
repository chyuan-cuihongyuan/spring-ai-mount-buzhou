# Wayfinder Map — Buzhou 工具健康探测装配（effort #305，C 会话第 6 轮）

> C 会话第 6 轮。spec 165 的 `ToolHealthProber`（Consul health check）standalone
> ——宿主手工 new、无 bean 无周期无健康面。

## Destination

`buzhou.tools.health.enabled=true` 即装配：prober bean + interval 自调度 +
翻转计数 + `ToolHealth`（BuzhouHealth 严格口径：外部工具 DOWN 不拉低机制，
down 列表详情显形）；探针注册归宿主（分层诚实）；默认关零变化。

## Notes

- 号段：spec 305 / T601–T602 / impl-328。
- prober 补 `lastKnown()`（快照读取，健康面不主动探测）。

## Decisions so far

- DOWN 严格口径（BuzhouHealth 契约）：工具机制整体恒 UP（其他工具仍可用），
  DOWN 工具进 details.down 列表。

## Out of scope

- 探针自动发现（框架不知道怎么探——165 分层诚实原则）。

## Tickets

- [x] [T601 health 属性组 + prober/健康面装配](tickets/T601-health-assembly.md)（impl-328）
- [x] [T602 装配回归 + lastKnown 快照](tickets/T602-health-close.md)（impl-328）
