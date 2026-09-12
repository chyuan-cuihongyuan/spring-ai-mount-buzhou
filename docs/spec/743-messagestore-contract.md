# 743 — MessageStore SPI 契约校验套件

> 来源：G 会话第 44 轮 = effort #744（spec 705 同构扩散）/ [T1037](../../.wayfinder/tickets/T1037-messagestore-contract-shape.md) / [T1038](../../.wayfinder/tickets/T1038-messagestore-contract-verify.md) / impl 546。

## 背景

SessionStateStore 契约（spec 705）之外的第二 SPI——MessageStore（append/load/deleteSession）是归档 saga/导入还原的数据底座，语义走样同样静默劣化。

## 目标

- `MessageStoreContract`（core/spi，spec 705 同构）：四项有序契约——append/load 往返保序、未知会话空读、多次追加保序、deleteSession 幂等清场；Report/Check 不可变；`__contract__` 探针自清理。

## 测试

InMemory 实现 4/4 全绿；deleteSession no-op 走样实现逐项红；探针零残留。

## 兼容性

纯增量公共类（快照收口轮随再生入档）。
