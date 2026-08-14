# Wayfinder3 Tracker（本地 markdown · effort #3「生产级收口」）

本目录是**第三个 effort** 的 wayfinder 图，延续 [`.wayfinder/`](../.wayfinder/README.md)（effort #1，T1–T27）与 [`.wayfinder2/`](../.wayfinder2/README.md)（effort #2，T28–T54 全闭合、27 实现纵切片落地）。**map = `MAP.md`**；**决策票 = `tickets/T<n>-<slug>.md`**（编号 **T55 起全局续用**）。

**总纲 spec**：由 `/to-spec` 从本图 + [docs/research/oss-production-grade.md](../docs/research/oss-production-grade.md) 合成，落 `docs/spec/13-*.md`；**实现纵切片 = `impl/<NN>-<slug>.md`**（`/to-tickets` 切出，索引见 `impl/README.md`）。

## Ticket 文件约定

frontmatter 字段：

| 字段 | 取值 |
|------|------|
| `id` | `T55`..`Tn`，与文件名一致（全局唯一，续 effort #1/#2） |
| `title` | 一句话标题（= ticket 的 name，叙述里用它，不裸用 id） |
| `type` | `research` \| `prototype` \| `grilling` \| `task` |
| `status` | `open` \| `closed`（closed 即离开 frontier） |
| `assignee` | 领取者；`""` = 未领取（未领取的 open ticket 才算可被领取） |
| `blocked-by` | id 列表；**全部 closed 才算 unblocked** |
| `created` | 日期 |

- **frontier** = `status:open` + `assignee:""` + 无未闭合 `blocked-by`。
- **领取**：开工前先把 `assignee` 写自己（claim），并发会话据此跳过。
- **解决**：在 ticket 末尾补 `## Resolution` 段，`status` 改 `closed`，再到 `MAP.md` 的「Decisions so far」加一行（标题链接 + 一句话 gist）。
- **每会话最多解决一张 ticket**（research 例外）；chart-the-map 阶段不解决任何 ticket（research 子 agent 例外）。
- **用户常设授权（2026-08-14）**：本 effort 全程「不需询问意见、按推荐迭代」——grilling/task 票允许以 **ratify 研究推荐** 的方式 AFK 闭合（Resolution 注明「用户常设授权、可推翻」）。

## 引用方式

叙述、MAP 索引里一律用 ticket 的 **title** 包链接，不裸写 `#T55`。
