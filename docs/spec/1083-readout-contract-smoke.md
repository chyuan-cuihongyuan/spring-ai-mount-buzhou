# 1083 — J 系读面统一契约冒烟轮

> 来源：J 会话第 83 轮 = effort #1083（[T1621](../../.wayfinder/tickets/T1621-readout-smoke-shape.md) / [T1622](../../.wayfinder/tickets/T1622-readout-smoke-verify.md) / impl 835）。借鉴：JUnit QuickCheck 性质收缩（性质=组件非负+reset 幂等，作用于读面谱系全体）。读面谱系的谱系（meta-readout）。

## Problem Statement

J 系 R46–R77 产出 15 个静态 stats() 读面，各自轮内仅验证各自语义——**统一契约（可调用/组件非负/reset 幂等）无集中冒烟**：任一读面若混入负值组件或 reset 漏字段，各轮绿测试无法发现。

## 目标

- starter 模块新增 `ReadoutContractSmokeTest`（聚合模块跨模块 classpath）。
- 显式清单（15 读面类）反射驱动三性质：①stats() 可调用且 record 组件全非负 ②resetForTest() 后组件全归零 ③重复调用稳定。
- 清单显式维护 = 新读面登记纪律落点（新增读面须入清单，遗漏即冒烟缺口）。

## 兼容性

纯测试增量；反射只读（invoke stats/resetForTest 公有静态方法），无生产变更。

## Out of Scope

- 非计数型读面（近窗环/快照类语义各异，不强冒烟）。
- 自动清单发现（反射全包扫描成本高，显式清单即纪律）。
