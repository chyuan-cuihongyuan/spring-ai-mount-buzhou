# 614 — GCRA 限流 yml 装配

> 来源：F 会话第 15 轮 = effort #600（spec 603 原语的装配扩散轮——D 会话 324/422 同模式）/ [T878](../../.wayfinder/tickets/T878-gcra-assembly-shape.md) / [T879](../../.wayfinder/tickets/T879-gcra-assembly-verify.md) / impl 467。

## 背景

GcraRateLimitBackend（spec 603）只有编程注入；突发即 429 的供应商场景需要三行 yml 声明即得平滑整形。

## 目标

`buzhou.resilience.rate-limit.smoothing: gcra`（配 `gcra-burst-tolerance`）声明即装配。

## 非目标

- 不做 Redis 共享 GCRA（雾区）。

## 设计

- smoothing 闭集 {token-bucket（默认）, gcra}，未知值装配 fail-fast。
- 共享后端在场 → 共享语义优先（跨实例额度 > 单进程整形，诚实取舍）。
- RateLimit record 扩组件 + canonical @ConstructorBinding（多构造绑定坑 R39 同法）+ 四参兼容构造。

## 测试

装配 3 用例（默认口径/闭集校验/E2E chat）+ yml 绑定 1 用例；resilience 全模块零回归。

## 兼容性

默认 smoothing 缺失 = 令牌桶零变化；四参构造保留源码兼容。
