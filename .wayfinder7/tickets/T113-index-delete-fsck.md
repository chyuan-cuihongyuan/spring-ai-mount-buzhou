---
Type: task
Status: open
---
## Question

会话删除与索引/fsck 联动（T109 遗留）：SessionCleaner 级联删除时索引行仍 CLOSED（DELETED 状态预留未联动）；fsck 会话全集仍走观测留痕（依赖观测完整性）。两个缺口是否本 effort 闭合？

## Resolution

AFK 自决：闭合。①SessionIndexObserver 无 delete 回调——在 core SessionCleaner 增可选 SessionIndexStore 贡献（auto-config 检测 bean 时把 index.delete 挂进级联；编程式 RuntimeConfig.cleanupContributors 同口）；索引行为置 DELETED 而非物理删（审计留存，query 默认过滤 DELETED 不在——status 过滤由调用方显式指定）。②fsck 增 `run(stores, index)` 重载：有索引时全集切索引源（索引面完整性高于观测留痕），无索引回退观测。产 spec 33 §B + impl-88。
