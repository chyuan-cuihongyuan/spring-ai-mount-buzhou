# Wayfinder Map — Buzhou 会话轨迹→数据集回流（effort #32）

> effort #32，延续 #5–#31（累计 178 轮 / T1–T294 / impl 1–217）。
> 主线：**黄金轨迹一键建集**——借鉴 LangSmith「session/trace 转 dataset」：既有良好
> 会话的完整轮次（问→答）批量转为评估项；golden 与否由调用方筛会话（机制不预设）。

## Destination

`SessionTrajectoryImporter.importFromSession(sessionId, datasetName)`：完整轮入集
（input/expected/溯源会话+轮次）；缺问缺答轮跳过计数；同溯源去重；dataset 未建
fail-fast；零新键。

## Notes

- 口径与 FeedbackImporter 同源（顶层轮序、firstText、溯源去重键）。

## Decisions so far

- 通用回流器（不叫 Golden*——golden 判定归调用方）；result 三态计数。

## Not yet specified

- 按轮过滤谓词 / 工具中间轮展开（需求证据后议）。

## Out of scope

- 沿用 #7–#31；自动筛选黄金会话；新配置键。

## Tickets

- [x] [T295 SessionTrajectoryImporter](tickets/T295-trajectory.md)（impl-218）
- [x] [T296 红队 + 文档 + 快照 + verify + 收口](tickets/T296-trajectory-close.md)
