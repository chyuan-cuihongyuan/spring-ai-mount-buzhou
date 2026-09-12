# 709 — 角色权限拒绝有界日志

> 来源：G 会话第 10 轮 = effort #709（借鉴 Redis ACL LOG——有界最近拒绝 + 每项 reason）/ [T969](../../.wayfinder/tickets/T969-denial-log-shape.md) / [T970](../../.wayfinder/tickets/T970-denial-log-verify.md) / impl 512。

## 背景

ToolRoleGuardHook（spec 141）拒绝时只发 `buzhou.tool-role.denied` 计数（tag 仅 role——per-tool tag 违基数守卫）。运维问题「哪个会话角色在反复尝试哪些无权工具（探测面扫描/配置错配）」不可答；fail-closed 未定义角色与常规未授权在计数上不可分。

## 目标

- `ToolDenialLog`（guard 包）：
  - 有界环形明细（默认 128 条）：{timestampMillis, role, toolName, reason}；reason ∈ `unauthorized`（有角色无权限）/ `undefined-role`（fail-closed 未定义）；`entries()` 最新在前不可变快照。
  - 有界聚合：ConcurrentHashMap<(role,tool) 复合键, LongAdder> 封顶 64 键（超限置 `_truncated`），`topDenials()` 不可变快照。
- ToolRoleGuardHook 增可选构造注入 log；拒绝路径双记（明细 + 聚合）；既有两参构造行为逐字节不变；micrometer 计数事件不动。

## 非目标

不做健康段/端点装配（读面接装配轮顺延）；不做清零 API（探针只读——ACL LOG RESET 语义留位）；不做持久化（进程内有界探针）。

## 测试

双记与 reason 分流；环形封顶最老被挤；聚合 64 键封顶 truncated；无 log 构造零回归；快照不可变。

## 兼容性

opt-in 纯增量；默认构造逐字节不变。
