# Spec 129 — PII yml 声明式规则（effort #93）

> wayfinder map：`.wayfinder/maps/effort-93.md`（T475–T476）。#85 收口 fog 种子⑤；
> spec 118 out-of-scope「yml 声明式配置；输入侧叠加」双项接续。

## Problem Statement

自定义 PII 规则（spec 118）只能程序面声明（构造 `CustomPiiRules` 传给 hook）——
用 yml 装配的宿主（`buzhou.guard.*` 全键面）无法声明领域规则，只能写代码绕行；
且输入侧 hook（spec 106）没有自定义规则通道，输入面领域格式裸奔。

## Solution

`buzhou.guard.pii.custom-rules` yml 键（guard fromYml env 直读，与
pii.enabled/types/input-redaction 同范式）：

- **形态**：List of Map（`- name: ORDER_ID / pattern: "ORD-\\d{6,}"`）或
  name→pattern Map（紧凑形态）；NAME 沿 spec 118 纪律（`[A-Z0-9_]{2,32}`）。
- **解析**：装配期（GuardModule.fromYml）构造 `CustomPiiRules`——非法 NAME /
  正则编译失败 fail-fast（配置错误不带病运行）。
- **接线**：输出侧 PiiRedactionHook 与输入侧 PiiInputRedactionHook（本轮补
  `(types, customRules)` 构造器）全四象限（有无 types × 有无 custom）。
- 无键 = 零变化（既有程序面构造器不受影响）。
- ReDoS 风险归声明方（spec 118 javadoc 纪律延续）。

## User Stories

1. 作为用 yml 装配的宿主，我声明几行 `custom-rules` 即得领域格式脱敏（订单号/
   工号/内部 token），不改一行代码。
2. 作为宿主，输入侧与输出侧共用同一份声明——用户输入与工具输出两面同防。
3. 作为运维，写错规则名或正则在启动期即失败（fail-fast），不进入运行期带病静默。

## Implementation Decisions

- `PiiInputRedactionHook` 新增 `(Set<PiiType>, CustomPiiRules)` 构造器（镜像输出侧）。
- `GuardModule.Builder` 增 `customPiiRules(CustomPiiRules)` 程序面 + fromYml 解析面。
- 绑定矩阵：`buzhou.guard.pii.custom-rules` 为结构化 List 键 → SKIPPED 显式登记
  （与 buzhou.bulkhead.agents 同档）。

## Testing Decisions

- yml map → GuardModule：输出叠加脱敏 / 输入叠加脱敏 / map 紧凑形态等价 / 无键
  零变化 / 非法 NAME fail-fast / 坏正则 fail-fast。
- 先例：PiiRedactionTest（guard yml 面）。

## Out of Scope

- NER 型规则；per-rule 开关；config-doctor 值域检查。

## Further Notes

- PII 防线四件套至此齐：内置五型（86）/ 自定义规则（118）/ 输入侧（106）/
  yml 声明式（本轮）。
