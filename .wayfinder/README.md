# Wayfinder Tracker（本地 markdown）

本目录是当前 effort 的 wayfinder 图。**map = `MAP.md`**；**ticket = `tickets/T<n>-<slug>.md`**。
这是 fallback 的「local-markdown tracker」——没有 GitHub Issues / `gh`，全在仓内文件。

## Ticket 文件约定

frontmatter 字段：

| 字段 | 取值 |
|------|------|
| `id` | `T1`..`Tn`，与文件名一致 |
| `title` | 一句话标题（= ticket 的 name，叙述里用它，不裸用 id） |
| `type` | `research` \| `prototype` \| `grilling` \| `task` |
| `status` | `open` \| `closed`（closed 即离开 frontier） |
| `assignee` | 领取者；`""` = 未领取（未领取的 open ticket 才算可被领取） |
| `blocked-by` | id 列表；**全部 closed 才算 unblocked** |
| `created` | 日期 |

- **frontier** = `status:open` + `assignee:""` + 无未闭合 `blocked-by`。
- **type 语义**：`research`(AFK，可一会话多张)、`prototype`/`grilling`/`task` 为 HITL（须真人介入）；详见 wayfinder SKILL。
- **领取**：开工前先把 `assignee` 写自己（claim），并发会话据此跳过。
- **解决**：在 ticket 末尾补 `## Resolution` 段，`status` 改 `closed`，再到 `MAP.md` 的「Decisions so far」加一行（标题链接 + 一句话 gist）。
- **每会话最多解决一张 ticket**（research 例外）。
- chart-the-map 阶段不解决任何 ticket（research 子 agent 例外）。

## 引用方式

叙述、MAP 索引里一律用 ticket 的 **title** 包链接（如「[CI 在 GitHub 红而本地绿的根因与修复](tickets/T1-...)」），不裸写 `#T1`。
