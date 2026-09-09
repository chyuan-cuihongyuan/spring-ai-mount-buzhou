# Wayfinder Map — Buzhou 工具目录漂移看门狗（effort #219，B 会话第 42 轮）

> B 会话第 42 轮。目录指纹（175）有了但没人<b>盯着</b>——MCP 热更新/装配变更
> 后要人工拍快照比对。看门狗把它自动化：周期/触发式重拍指纹，变化即发事件。

## Destination

CatalogDriftWatcher（core/exec）：持基线指纹；check(definitions) 重拍 diff
——非空即发 tool.catalog.drifted 事件（payload 三分类）+ 更新基线 + 计数；
首次 check 建基线不发事件。组合 175 为自动化闭环。

## Notes

- 号段：B=奇数 spec（本轮 201）；轮次 .wayfinder200+。
- check 由宿主定时/事件触发（tools/list_changed 等）——本类不含调度。
- 与 MCP drift（18）互补：那是协议源，这是装配全景。

## Decisions so far

- 首拍只建基线（装配时点多工具集变化是常态——不是事件）。

## Not yet specified

- 基线持久化（当前进程内）；drift webhooks 接线。

## Out of scope

- 沿用各轮；自动回滚；schema 语义判定。

## Tickets

- [x] [T573 CatalogDriftWatcher（基线+重拍 diff+事件）](../tickets/T573-drift-watcher.md)（impl-314）
- [x] [T574 看门狗回归（首拍建基线/变化事件/无变静默/再变再发）](../tickets/T574-drift-watcher-tests.md)（impl-314）
