# Wayfinder Map — Buzhou PII yml 声明式规则（effort #93，B 会话第 5 轮）

> B 会话第 5 轮。fog 种子⑤「PII yml 声明式规则」：spec 118 自定义规则仅程序面
> （CustomPiiRules 构造注入），out-of-scope 明列「yml 声明式配置」——本轮接续。

## Destination

`buzhou.guard.pii.custom-rules` yml 键：List<{name, pattern}>（或 name→pattern map）
→ 装配期解析为 CustomPiiRules，同时供输出侧与输入侧 hook；非法规则名/正则
fail-fast。默认无键零变化。

## Notes

- 号段协议：B=奇数 spec（本轮 129）；A=偶数 spec + 远端轮号（A 提交 3102dd7 声明）。
- 输入侧 hook 补 (types, customRules) 构造器——spec 118「输入侧叠加」out-of-scope
  一并收口。
- 键走 guard fromYml env 直读（与 pii.enabled/types/input-redaction 同范式）；
  结构化键入绑定矩阵 SKIPPED 登记。

## Decisions so far

- 装配期 fail-fast（非法 NAME 沿 Rule 构造器；正则编译失败原样上抛）——配置错误
  不带病运行。

## Not yet specified

- 配置体检 doctor 对 custom-rules 的近邻/值域检查（后续按需）。

## Out of scope

- 沿用 #7–#92；NER 型规则；per-type 豁免。

## Tickets

- [x] [T475 输入侧 hook 构造器补齐 + GuardModule yml 解析](../tickets/T475-pii-yml.md)（impl-277）
- [x] [T476 yml 声明式规则回归 + 矩阵登记](../tickets/T476-pii-yml-tests.md)（impl-277）
