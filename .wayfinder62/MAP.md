# Wayfinder Map — Buzhou 数据集快照副本（effort #62，50 轮自迭代第 27 轮）

> effort #62，延续 #61（T369–T370 / impl-246）。主线：**spec 82 fog 项「数据集
> 快照副本」**——指纹能验「同版本」，但版本本体不可冻结：数据集一直被 addItem
> 演化，历史 run 指向的内容会漂移。

## Destination

`EvalDatasetStore.snapshotDataset(source, target)`：全部条目<b>原 id 复制</b>到新
数据集（快照指纹与源一致——指纹覆盖 id/input/expected）；target 已存在 fail-fast
（快照不可覆盖——版本不可变语义）；source 缺失 fail-fast；快照可继续 addItem
（nextItemId 从 max+1 续排——演化分叉后指纹自然分离）。

## Notes

- 借鉴：LangSmith dataset versioning（不可变版本 + run 指向版本）。

## Decisions so far

- 原 id 复制（重排 id 会破指纹等值——快照的意义就在「同版本」）。

## Not yet specified

- 快照血缘查询（listSnapshotsOf source——需求证据后议）；run 记录绑定快照名。

## Out of scope

- 沿用 #7–#61；快照 TTL。

## Tickets

- [x] [T373 snapshotDataset 原 id 复制](tickets/T373-dataset-snapshot.md)（impl-247）
- [x] [T374 3 例红队（指纹一致/不可覆盖/续排分叉）+ 收口](tickets/T374-snapshot-close.md)
