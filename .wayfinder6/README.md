# Effort #6 tracker 约定

沿用 `docs/agents/issue-tracker.md` 的本地 markdown 约定，effort 目录 = `.wayfinder6/`。

- **Map**: `MAP.md` — Notes / Decisions-so-far / Fog body。
- **决策票**: `tickets/T<n>-<slug>.md`（T103 起全局续用）。frontmatter：`Type:`（research/prototype/grilling/task）、`Status:`（open/closed）、`blocked-by:`（逗号分隔票号）、`assignee:`。
- **执行切片**: `impl/<nn>-<slug>.md`（78 起全局续用，/to-tickets 产出）。
- **Frontier**: `tickets/` 中 open、无未闭合 blocked-by、无 assignee 的票，按票号取。
- **Resolve**: 票尾追加 `## Resolution`，`Status:` 改 closed，MAP 的 Decisions so far 追加一行 gist + 链接。
- **切片完成**: impl 票尾追加 `## Done`（commit 号 + 验证方式），`Status:` 改 done。
- **每轮自迭代**（用户 2026-08-15 授权全自主，effort #6 延续）：wayfinder 解一张决策票 → /to-spec 增量 → /to-tickets 切片 → /implement 落地 → 模块级验证 → commit；里程碑轮全仓 `mvn clean verify`。
