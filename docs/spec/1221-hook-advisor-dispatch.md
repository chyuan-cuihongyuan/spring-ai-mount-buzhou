# 1221 — R22：HookAdvisor 切面分发直测（beforeModel 短路 × afterModel 回填 × onModelError 决策树）

> 来源：K 会话第 22 轮 = effort #1221（[T1851](../../.wayfinder/tickets/T1851-hook-advisor-shape.md) / [T1852](../../.wayfinder/tickets/T1852-hook-advisor-verify.md) / impl 924）。方法论：决策树全覆盖测试（每个 `instanceof`/`null` 分支 = 一条用例），adviseCall 与 adviseStream 同构对称断言。

## Problem Statement

HookAdvisor 是 Hook 机制进入 Spring AI advisor 链的执行面：beforeModel 切面 Block 短路、afterModel replaceResponse 回填、onModelError 三态兜底决策（Block 回填文本/Replace 回填结构化响应/null 放行 rethrow）——分发回归 = Hook 安全护栏（beforeModel 拦截）与韧性兜底（onModelError）同时失效。

## 目标

- HookAdvisorTest（8 用例，adviseCall × adviseStream 同构对称）：beforeModel Block 短路（nextCall 不触达）；happy path（markResponded→afterModel→ctx.response 返回，afterModel replaceResponse 回填生效）；nextCall 异常 → onModelError 决策树（Block 回填文本吞错/Replace 回填结构化响应吞错/Continue 放行 rethrow）；流式 Block 短路 Flux.just、流式异常兜底 Flux、流式无处理 Flux.error 透传。

## 实现决策

- HookChain 子类 override beforeModel/afterModel/onModelError 三切面（脚本化 HookResult + 录制）；HookEnvironment 3 参真件（InMemorySessionStateStore）。
- CallAdvisorChain/StreamAdvisorChain 匿名 stub（nextCall/nextStream 脚本化、copy/getAdvisors 平实现）；ChatClientResponse 经 record 构造。

## 测试决策

- 断言只对外部行为：返回的响应文本/对象身份、链方法调用记录、异常类型与身份（rethrow isSameAs）。
- 验收门：定向绿 + HookAdvisor 分支提升入账 + buzhou-core 定向绿。

## 兼容性

纯测试增量：主代码零变化、公共 API 面零变化、既有测试零改动。

## Out of Scope

- HookChain 自身切面分发逻辑（既有 HookChain*Test 套件覆盖）。

## Further Notes

- 同构对称断言（call/stream 逐分支成对）是本类分发骨架的核心风险形状——一侧回归另一侧独立可见。
