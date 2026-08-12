---
id: T7
title: .scratch/ 移出 git 跟踪 + 加入 .gitignore
type: task
status: open
assignee: ""
blocked-by: []
created: 2026-08-13
---

## Question

`.scratch/` 当前被 git 跟踪（52 个内部 issue/spec 文件：`buzhou-implementation/`、`spring-ai-trip/`）。如何干净移除——

- 仅停止跟踪（`git rm -r --cached .scratch` + 写 `.gitignore`），保留历史？
- 还是需要从历史抹除（`git filter-repo` rewrite）——仅当含敏感信息才值得承担重写代价。

## Context

- 决策点：先确认 `.scratch/` 内容**无密钥/敏感信息**；无则仅 untrack 足够（默认走这条，见 MAP「Out of scope」已排除历史抹除）。
- 仓库卫生项，支持 core 深化 effort 的可信度（开源仓不应泄漏内部排障草稿）。
- AFK 可做：扫描敏感 → `git rm --cached` + `.gitignore` + 提交。

## Resolution

<!-- task 完成后填写：扫敏结论 + 实际操作 + commit -->
