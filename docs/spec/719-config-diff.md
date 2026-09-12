# 719 — 生效配置 diff 读面

> 来源：G 会话第 20 轮 = effort #719（借鉴 kubectl diff——「变化即清单」）/ [T989](../../.wayfinder/tickets/T989-config-diff-shape.md) / [T990](../../.wayfinder/tickets/T990-config-diff-verify.md) / impl 522。

## 背景

生效配置快照端点（spec 343）只有全量视图——「这次热重载/部署到底改了什么」要人肉对比两份快照。变更可见性是配置治理（热重载 spec 110、漂移审计 spec 414）的读面缺口。

## 目标

- `ConfigDiff`（core/health 纯函数）：`static List<Entry> diff(Map<String,String> before, Map<String,String> after)`——
  - Entry{key, before, after, kind}；kind ∈ ADDED / REMOVED / CHANGED；不变键不出现；
  - 按 key 字典序稳定输出；不可变。
- **掩码语义**：消费方传入快照端点同源 Map（敏感值已 `***` 掩码）——掩码值相等 = 未变（诚实注记：掩码底变化不可见）；diff 不做二次掩码（不猜敏感键——上游已定）。

## 非目标

不做端点装配（spec 343 端点扩展留装配轮顺延）；不做结构化嵌套 diff（快照是扁平键值）。

## 测试

三分类 + 字典序 + 掩码同值不算变更 + null 拒绝 + 不可变。

## 兼容性

纯增量公共类；零行为变化。
