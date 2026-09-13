# 922 — SessionLeaseStore 契约校验套件

> 来源：I 会话第 23 轮 = effort #922（[T1295](../../.wayfinder/tickets/T1295-lease-contract-shape.md) / [T1296](../../.wayfinder/tickets/T1296-lease-contract-verify.md) / impl 675）。spec 705（SessionStateStoreContract）/ 743（MessageStoreContract）同构扩散（Pact consumer contract testing）。

## 背景

租约是「多实例安全」的基石 SPI（acquire/renew/release/steal/inspect/deleteSession 七方法 + fencing token 单调），但第三方 store 实现无自证工具——语义偏差（如 fence 不单调、release 后 fence 复用）在多实例竞态下才显形，排查成本极高。契约套件把七方法语义固化为可复用校验。

## 目标

- `SessionLeaseStoreContract`（spi 包，静态 verify 范式——主源码零 JUnit 依赖、逐项收集不抛）九项检查：
  1. tryAcquire 空闲会话成功且 fencing token 为正；
  2. 同 owner 重复 acquire 幂等（同 token）；
  3. 异 owner acquire 被拒（持有期内）；
  4. renew 持有人成功（TTL 续期）；
  5. renew 非持有人失败；
  6. release 后同会话可再 acquire（新 fence）；
  7. steal 抢占成功且 fence 严格递增（旧 token 失效语义由 inspect 可见）；
  8. inspect 反映租约状态（存在/持有人）；
  9. deleteSession 幂等（不存在时无操作）；
- TTL 全部秒级短 TTL（测试快）；
- core 测试域 `LeaseContractAccessTest`：InMemorySessionLeaseStore 过九项（第三方实现的接入示例）。

## 兼容性

纯增量：新公共契约类（spi 包）+ 测试；零既有行为变化。
