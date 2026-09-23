# impl 1523 — FallbackChainValidator 降级链配置校验（R123 = effort #1922 / spec 1922 / T3045-T3046）

**What**：`FallbackChainValidator`（core/transaction 静态纯函数）——
validate 四规则（primary 非空/fallbacks 非空/无重复/主不在备链）
错误列表全收集 + isSane 便捷布尔。

**Why**：Resilience4j/LiteLLM 降级链惯例——链配置错误首次降级才
暴雷是最坏时机；静态校验启动期拦住，多错误并列收集修一次到位。
与备模型降级链（#5 执行面）互补。

**Verify**：`FallbackChainValidatorTest` 4 用例全绿（合法零错误/
主模型混入精确下标/重复逐个入账/多错误并列收集）。

**Status**：done（2026-09-23）
