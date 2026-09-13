# 945 — SessionIndexStore 契约校验套件

> 来源：I 会话第 45 轮 = effort #945（[T1321](../../.wayfinder/tickets/T1321-index-contract-shape.md) / [T1322](../../.wayfinder/tickets/T1322-index-contract-verify.md) / impl 694）。spec 922/936 契约系列延续。

## 背景

`SessionIndexStore`（upsert/get/list/delete/purgeOlderThan）是会话治理第一查询面——第三方实现的「覆盖幂等、DELETED 排除、purge 计数/limit」语义无自证工具。

## 目标

- `SessionIndexStoreContract`（spi 包静态 verify 范式）五项检查：
  1. upsert→get 往返一致（字段逐项相等）；
  2. 同 id 重复 upsert 覆盖幂等（后写胜）；
  3. delete 后 get empty 且 delete 二次无操作；
  4. list 默认排除 DELETED 状态行（spec 33 §B 审计行显式过滤口径）；
  5. purgeOlderThan 按 lastActiveAt 截止删除并返回实际删除计数、limit 尊重；
- core 测试域 `IndexContractAccessTest`：内存实现过五项。

## 兼容性

纯增量：新公共契约类 + 测试；零既有行为变化。
