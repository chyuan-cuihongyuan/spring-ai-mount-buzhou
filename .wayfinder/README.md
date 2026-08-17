# Wayfinder Tracker（本地 markdown · 单一目录）

本目录是**全部 effort** 的 wayfinder 图。**总索引 map = [`MAP.md`](MAP.md)**；**各 effort 的 map = `maps/effort-<NN>.md`**（其运行期约定存档 = `maps/readme-effort-<NN>.md`）；**决策票 = `tickets/T<n>-<slug>.md`**（票号 **T1–T248 全局连续**，T231–T239 跳号未用，下一张 = **T249**）；**实现纵切片 = `impl/<NN>-<slug>.md`**（索引与编号史见 [`impl/README.md`](impl/README.md)，下一片 = **196**）；**总纲 spec = [`SPEC.md`](SPEC.md)**（effort #1 的 `/to-spec` 产物；effort #2 起总纲落 `docs/spec/12+`）。

> 2026-08-17 前每个 effort 一个独立目录（`.wayfinder/`、`.wayfinder2/` … `.wayfinder15/`），现已融合为本单一目录——**新 effort 只加 `maps/effort-<NN>.md`，勿再新建 `.wayfinderN/`**。
> 这是 fallback 的「local-markdown tracker」——没有 GitHub Issues / `gh`，全在仓内文件。

## Ticket 文件约定

frontmatter 字段（effort #1–#3 的 yaml 风格）：

| 字段 | 取值 |
|------|------|
| `id` | `T1`..`Tn`，与文件名一致（全局唯一、跨 effort 续号） |
| `title` | 一句话标题（= ticket 的 name，叙述里用它，不裸用 id） |
| `type` | `research` \| `prototype` \| `grilling` \| `task` |
| `status` | `open` \| `closed`（closed 即离开 frontier） |
| `assignee` | 领取者；`""` = 未领取（未领取的 open ticket 才算可被领取） |
| `blocked-by` | id 列表；**全部 closed 才算 unblocked** |
| `created` | 日期 |

- effort #4 起改用行式 frontmatter：`Type:` / `Status:` / `blocked-by:` / `assignee:`（见 [maps/readme-effort-04.md](maps/readme-effort-04.md)）；两种风格并存于历史票，读取时都认。
- **frontier** = 本 effort `status:open` + `assignee:""` + 无未闭合 `blocked-by`。
- **type 语义**：`research`(AFK，可一会话多张)、`prototype`/`grilling`/`task` 为 HITL（须真人介入）；详见 wayfinder SKILL。effort #2 起有「用户常设授权 AFK ratify」惯例（Resolution 注明「用户常设授权、可推翻」）。
- **领取**：开工前先把 `assignee` 写自己（claim），并发会话据此跳过。
- **解决**：在 ticket 末尾补 `## Resolution` 段，`status` 改 `closed`，再到**该 effort 的** `maps/effort-<NN>.md` 的「Decisions so far」加一行（标题链接 + 一句话 gist）。
- **每会话最多解决一张 ticket**（research 例外）。
- chart-the-map 阶段不解决任何 ticket（research 子 agent 例外）。

## 引用方式

叙述、MAP 索引里一律用 ticket 的 **title** 包链接（如「[CI 在 GitHub 红而本地绿的根因与修复](tickets/T1-ci-red-remotely-green-locally.md)」），不裸写 `#T1`。
