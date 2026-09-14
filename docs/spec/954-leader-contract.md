# 954 — LeaderElector 契约校验套件

> 来源：I 会话第 53 轮 = effort #954（[T1321](../../.wayfinder/tickets/T1321-leader-contract-shape.md) / [T1322](../../.wayfinder/tickets/T1322-leader-contract-verify.md) / impl 699 续）。spec 922/936/945 契约系列延续（选主 SPI 正确性契约，与对方 R40 选举竞争读数面分轴）。

## 背景

`LeaderElector`（spec 331）是后台任务单执行者的基石 SPI——第三方实现的「重入幂等、跟随态判定、resign 转移、纪元语义」偏差会导致双主执行（重复清扫/重复投递）。

## 目标

- `LeaderElectorContract`（spi 包静态 verify 范式）五项检查：
  1. 空位 tryAcquireOrRenew → 新纪元（epoch ≥ 1）且 leader=true；
  2. 持有人重入幂等（同 epoch、leader=true——TTL 续期）；
  3. 他人持有时本候选返回跟随态（leader=false）；
  4. resign 后空位可再获取；
  5. inspect 反映持有人/空位（空位 epoch=0、holder=null）。
- core 测试域 `LeaderContractAccessTest`：InMemoryLeaderElector 过五项。

## 兼容性

纯增量：新公共契约类 + 测试；零既有行为变化。
