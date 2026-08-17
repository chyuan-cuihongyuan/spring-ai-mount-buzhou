> **历史存档**：以下为 effort #7 运行期的 tracker 约定（当时每个 effort 一个独立目录 `.wayfinder7/`）。
> 2026-08-17 起全部 effort 融合为单一 `.wayfinder/`：map → `maps/effort-07.md`，决策票 → `../tickets/`，实现切片 → `../impl/`，现行约定见 [`../README.md`](../README.md)。

# Effort #7 tracker 约定

沿用 `docs/agents/issue-tracker.md` 本地 markdown 约定，effort 目录 = `.wayfinder7/`。

- **Map**: `MAP.md`；**决策票**: `tickets/T<n>-<slug>.md`（T112 起）；**执行切片**: `impl/<nn>-<slug>.md`（87 起）。
- **每轮自迭代**（用户 2026-08-15 授权全自主延续）：wayfinder 解票 → /to-spec 增量 → /to-tickets 切片 → /implement → 模块级验证（下游模块一律 `-am`）→ emoji commit。
- 里程碑轮全仓 `mvn clean verify`。
