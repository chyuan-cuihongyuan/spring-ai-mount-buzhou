# 617 — 技能目录漂移看门狗

> 来源：F 会话第 18 轮 = effort #600（spec 616 指纹原语的接线扩散；spec 201 CatalogDriftWatcher 的 skills 镜像）/ [T884](../../.wayfinder/tickets/T884-skill-drift-watcher-shape.md) / [T885](../../.wayfinder/tickets/T885-skill-drift-watcher-verify.md) / impl 470。

## 背景

技能目录指纹（spec 616）只有原语；宿主需要「定时重拍、漂移即事件」的看门狗面（工具侧 201 同需求已满足）。

## 目标

`SkillCatalogDriftWatcher`：首拍建基线；check 漂移 → `skill.catalog.drifted` 事件 + 计数 + 基线推进。

## 非目标

- 无内置调度（宿主定时/事件触发——201 同口径）。

## 设计

镜像 201 结构；事件载荷 {added, removed, changed, oldSummary, newSummary}。**接线时修正 616 diff 方向缺陷**（对齐 175：added=参数侧新增，`基线.diff(现)` 即时间正向）。

## 测试

2 用例（基线→静默→漂移→推进 / null 安全）+ 指纹测试 4 用例修正后全绿。

## 兼容性

616 尚未发布消费方（本轮同批落地），方向修正无外部影响。
