# Spec 72 — 会话轨迹→数据集回流（effort #32）

> wayfinder map：`.wayfinder/maps/effort-32.md`（T295–T296）。OSS 借鉴：LangSmith
> session/trace 转 dataset。

## Problem Statement

从已知良好会话建评估集需要手工逐轮复制问答——trace 与 dataset 两系统缺回流桥。

## Solution

`SessionTrajectoryImporter.importFromSession(sessionId, datasetName)`：完整轮（首条
USER 问 + 首条 ASSISTANT 答）批量入集，带 (sessionId, turnSeq) 溯源；缺问缺答跳过
计数；同溯源去重；dataset 未建 fail-fast。golden 与否归调用方（机制不预设筛选）。

## User Stories

1. 作为评估作者，我要从黄金会话一键建集，所以回归评估有真实语料。
2. 作为红队，我要溯源去重与缺轮跳过被钉住，所以重复回流不膨胀数据集。
3. 作为既有用户，我要零行为变化，所以升级零风险。

## Implementation Decisions

- 口径与 FeedbackImporter 同源（顶层轮序、firstText、去重键）。

## Testing Decisions

- 完整轮入集 + 溯源断言；重复回流全去重；dataset 未建 fail-fast；空会话零导入。

## Out of Scope

- 自动黄金筛选；按轮谓词过滤；新配置键。

## Further Notes

- 与负反馈回流互补：正例建集（本面）+ 负例回流（既有）。
