# Spec 38 — 迁移器 / 黄金扩充 / 红队新面 / 观测审计 / health

> effort #8（T135–T140 / impl-108–113）。

## §A 压缩梯子事件化（T135 / impl-108）

- 梯子（0.7→1.0 步进加压）**每级**实际折入都通知（此前只发首轮）；payload 增
  `evictRatio`（当前级比例）——消费方可区分梯子级与主路径。
- `CompactionListener`（函数式接口，替代 T115 的 BiConsumer）：`onCompacted(sessionId,
  result, evictRatio)`；异常吞（lenient 不变）。

## §B 跨 store 迁移器（T136 / impl-109）

- `SessionMigrator.migrate(source, target, sessionId, keepIds)`（core 静态工具）：
  复用 exportSession + importSession 两条成熟管线（不引第三套数据通路）；
  默认 Id 重映射；keepIds 冲突 fail-fast 沿用；指标 `buzhou.session.migrations`。
- 轻量工具定位（非自动服务）：调用方显式逐会话搬迁（JDBC→Redis 切换/缩容下线）。
- 跨 store 形态用例在 examples（依赖方向 core ← store 模块）。
