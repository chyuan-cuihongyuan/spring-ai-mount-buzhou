---
id: T7
title: .scratch/ 移出 git 跟踪 + 加入 .gitignore
type: task
status: closed
assignee: zcode
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

**决策**：**仅停止跟踪，保留历史**（默认路径）。`.scratch/` 59 个 `.md` 草稿经六类敏感扫描无任何命中（密钥 / token / Bearer / 邮箱 / 内网主机 / 保密标记；唯一内网主机命中 `buzhou.internal`/`core.internal` 为 OTel span 命名空间误报）→ 不值得承担 `git filter-repo` 重写代价。

**操作**：`.gitignore` 加 `.scratch/`（归入「private, not published」语义段）+ `git rm -r --cached .scratch`（59 项移出索引、工作树保留）。验证 `git ls-files .scratch` = 0、`git check-ignore` 命中、19 个历史提交仍引用（历史保留）。`CLAUDE.md`/`docs/agents/issue-tracker.md` 对 `.scratch/` 的引用为路径模板、移出后仍成立。

**实现切片**：[impl/02](../impl/02-untrack-scratch.md)。
