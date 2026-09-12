# effort #713 — 数据集标签与过滤

- 会话：G 会话 700 系第 14 轮 ｜ spec [713](../../../docs/spec/713-dataset-tags.md) ｜ 票 [T1026](../tickets/T1026-dataset-tags.md)/[T1027](../tickets/T1027-dataset-tags-verify.md) ｜ impl613
- 借鉴：Langfuse（≈15K star）dataset tags——评测资产的组织维度

## 勘察（排重）

- EvalDatasetMeta 四组件（name/description/itemCount/createdAt）无标签；grep tag/Tag 在 EvalDatasetStore 零命中（前轮已勘）。
- snapshotDataset（100）是版本副本——非组织维度。

## 决定

EvalDatasetMeta 扩第 5 组件 `tags`（归一升序不可变，默认空）+4 参兼容构造；store 增 `tagDataset`/`untagDataset`（幂等，tag 归一 trim+lowercase，校验 `[a-z0-9:-]{1,32}` fail-fast）与 `listDatasetsByTag(tag)`（listDatasets 过滤）；metaToMap/decodeMeta 增 tags 字段（旧记录解码=空表——向后兼容）。

## 测试

打标/去标幂等+归一/listDatasetsByTag 过滤/旧记录无 tags 字段解码=空表/非法 tag fail-fast。

## 诚实边界

dataset 级标签（item 级不做）；无标签继承（snapshot 副本不带原标签——显式重打）；纯组织维度不参与 fingerprint（指纹语义不变）。
