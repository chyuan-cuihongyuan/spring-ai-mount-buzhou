# 705 — store SPI 契约校验套件

> 来源：G 会话第 6 轮 = effort #705（借鉴 Pact consumer contract testing——契约即可执行验证物）/ [T961](../../.wayfinder/tickets/T961-store-contract-shape.md) / [T962](../../.wayfinder/tickets/T962-store-contract-verify.md) / impl 508。

## 背景

SessionStateStore SPI 的精细语义（`deleteIfValueMatches` 消费一次 CAS——HITL 一次性放行依赖；`compareAndSwap` null-expect 首写——日翻越竞态钉住；`scanByPrefix` 下推扫描；`deleteSession` 幂等清场）任何一项走样，上层机制静默劣化且难以归因。第三方 store 实现者今天只能靠读 Javadoc 自证。

## 目标

- `SessionStateStoreContract`（core/spi，主源码、零 JUnit 依赖）：静态 `verify(SessionStateStore)` 跑九项有序契约检查：
  1. put/get 往返；2. getAll 全量；3. delete 移除；4. 未知会话空读；5. `deleteIfValueMatches` 值匹配才删（消费一次）；6. `compareAndSwap` null-expect 仅缺位写；7. `compareAndSwap` 匹配覆写；8. `deleteSession` 幂等清场；9. `scanByPrefix` 前缀过滤。
- 返回 `Report`：`List<Check(name, passed, detail)>`（有序）+ `passed()` 全绿布尔 + `failures()` 失败名清单；不可变。
- 检查会话用 `__contract__` 前缀唯一 id，`try/finally deleteSession` 自清理——对真实存储零残留。
- 失败不抛异常（逐项收集——一跑看全部缺口）；`verify` 自身异常 folding 进对应 Check。

## 非目标

不做抽象测试基类 + JUnit 运行器绑定（主源码不进测试依赖）；不覆盖其余四 SPI（后续轮可扩）。

## 测试

真实 InMemorySessionStateStore 全绿 9/9；注入三类走样实现（CAS 无条件删 / scan 漏过滤 / deleteSession no-op）逐项红且失败名可读；运行后零残留；Report 不可变。

## 兼容性

纯增量公共类（入 API 快照——收口轮随再生入档）；零行为变化。
