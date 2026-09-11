# 629 — 技能漂移看门狗渲染节拍接线

> 来源：F 会话第 30 轮 = effort #600（spec 617 看门狗的宿主接线）/ [T908](../../.wayfinder/tickets/T908-renderer-drift-shape.md) / [T909](../../.wayfinder/tickets/T909-renderer-drift-verify.md) / impl 482。

## 背景

SkillCatalogDriftWatcher（617）check 需宿主触发；目录清单每轮渲染是现成节拍。

## 目标

渲染器带 watcher：每轮 renderCatalog 顺带 check（漂移最迟下一轮显形，零调度）。

## 非目标

- 不做独立定时巡查（渲染节拍已够）。

## 设计

五参构造（null 零变化）；check 在 renderEntries 入口（缓存命中与否都查——指纹独立于渲染缓存）；装配建 watcher（WARN emitter）。

## 测试

2 用例：基线→改描述→漂移事件 / 无 watcher 零变化。

## 兼容性

既有构造零变化；新公共方法随轮再生快照。
