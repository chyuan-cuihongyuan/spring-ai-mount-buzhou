# Wayfinder Map — Buzhou AB 面按版本查 run（effort #76，50 轮自迭代第 41 轮）

> effort #76，延续 #75（T409–T410 / impl-260）。主线：**spec 113 fog 项「AB 面
> runsOfVersion」**——eval 侧有了版本查询，AB 对比（B 换版验收的基线）还只能
> 按名翻。

## Destination

`PairwiseEvalRunner.abRunsOfVersion(store, fingerprint)` 静态：同指纹历史对比
聚合（startedAt 倒序沿用 abRuns 排序面）；空/空白指纹 = 空结果。spec 113 的
AB 面同构——B 换版验收「同基线版本对比集」零胶水。

## Notes

- 借鉴：spec 113 同思想（git by-hash）。

## Decisions so far

- 复用 abRuns 全量 + 内存过滤（AB run 量级小——不另做下推扫描）。

## Not yet specified

- 版本对比集 diff（两个指纹各取最新对比——视图层另议）。

## Out of scope

- 沿用 #7–#75。

## Tickets

- [x] [T411 abRunsOfVersion 静态查询](tickets/T413-ab-version-query.md)（impl-261）
- [x] [T412 红队（同指纹聚合/演化分流/空诚实）+ 收口](tickets/T414-ab-version-close.md)
