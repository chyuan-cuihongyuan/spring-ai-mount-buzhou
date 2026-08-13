# 02 — `.scratch/` 移出 git 跟踪 + 加入 `.gitignore`

**Impl-of:** [`.wayfinder/SPEC.md`](../SPEC.md) · **maps-to decision:** [T7](../tickets/T7-remove-scratch-from-git.md)

**What to build:** 把 `.scratch/`（内部 issue / spec 草稿）从 git 跟踪中干净移除，让开源仓只对外暴露该暴露的内容。先扫描全量内容确认无密钥 / 敏感信息；无则**仅停止跟踪**（`git rm -r --cached .scratch` + 写 `.gitignore`），保留历史；仅当发现敏感信息才升级为 `git filter-repo` 历史重写（默认不走）。

**Blocked by:** 无 — 可立即开始。

**Status:** done (assignee: zcode)

- [x] 扫描 `.scratch/` 全量文件，出具「无敏感信息」结论（见 Resolution）
- [x] `git rm -r --cached .scratch`，并在 `.gitignore` 加入 `.scratch/`
- [x] 提交后 `git status` 不再跟踪 `.scratch/`，git 历史保留（19 个历史提交仍引用）
- [x] N/A — 未发现敏感信息，无需历史重写

## Resolution

**扫描结论（2026-08-13）**：`.scratch/` 共 **59 个文件、全部 `.md`**（371K），内容为内部 issue/spec 草稿。六类敏感扫描**均无命中**：

- 高信号密钥（`sk-`/`AKIA`/`ghp_`/`ghs_`/`xox`/`AIza`/PEM 私钥）：无
- 真值形态的 `api_key`/`secret`/`token`/`credential`/`password` 赋值：无
- `Bearer`/`Authorization` 头：无
- 非示例邮箱：无
- 内网主机/IP：唯一命中 `buzhou.internal`/`core.internal` 为 **OTel span 命名空间**（非主机名），已确认为误报
- `confidential`/`proprietary`/`保密`/`严禁外传` 标记：无

→ **无敏感信息，走默认 untrack 路径，不做历史重写。**

**操作**：

1. `.gitignore` 新增「Internal design / issue drafts」段 + `.scratch/`（沿用既有「private, not published」分组语义）。
2. `git rm -r --cached .scratch` —— 59 项从索引移除，**工作树文件保留**。
3. 验证：`git ls-files .scratch` = 0；`git check-ignore .scratch/` 命中 `.gitignore:23`；19 个历史提交仍引用 `.scratch`（历史保留）；`CLAUDE.md` 与 `docs/agents/issue-tracker.md` 对 `.scratch/` 的引用均为**路径模板/约定说明**、非指向具体被跟踪文件的链接，移出跟踪后仍成立。

**未做**：`mvn clean verify` —— 本次为纯 git 卫生变更、无 Java 代码触及，SPEC 已记录本地十五模块全绿，重跑对验收无增量价值。
