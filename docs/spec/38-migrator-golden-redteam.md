# Spec 38 — 迁移器 / 黄金扩充 / 红队新面 / 观测审计 / health

> effort #8（T135–T140 / impl-108–113）。

## §A 压缩梯子事件化（T135 / impl-108）

- 梯子（0.7→1.0 步进加压）**每级**实际折入都通知（此前只发首轮）；payload 增
  `evictRatio`（当前级比例）——消费方可区分梯子级与主路径。
- `CompactionListener`（函数式接口，替代 T115 的 BiConsumer）：`onCompacted(sessionId,
  result, evictRatio)`；异常吞（lenient 不变）。
