# Spec 43 — 命令限额与配置校验（effort #9）

> effort #9 主线 spec。§A：run_command 输出兜底上限可配（T157）；§B：配置校验补全（T158）。

## §A run_command 输出内存兜底上限可配（T157 / impl-128）

### Problem Statement

图前勘察误判纠偏（诚实记录）：进程树强杀（超时/取消两路 killProcessTree）与输出内存兜底
（readBounded 5MB 截断）**已存在**（impl-49/60）。真实缺口只剩：5MB 兜底上限硬编码不可配——
低内存部署（如 512MB 容器）无法收紧，进程输出洪峰仍可先吃满兜底再截断。

### Solution

输出内存兜底上限可配（编程构造参 + `buzhou.tools.run-command.max-output-bytes`），缺省 5MB
不变；截断语义钉住：截断标记可见、进程照常跑完、exit 码照常上报。上下文治理仍归 Spill
offload（两层互不替代——本上限是 OOM 防护不是上下文防护）。

### User Stories

1. As a 运维工程师, I want 按部署规格收紧输出兜底上限, so that 低内存容器不被进程输出洪峰冲击。
2. As a 应用开发者, I want 截断发生时看到明确标记, so that 输出缺失可发现不静默。
3. As a 应用开发者, I want 非正上限启动即拒, so that 配错兜底不如不配的问题立刻暴露。

### Implementation Decisions

- `RunCommandTool` 增七参构造（maxOutputBytes；正数 fail-fast）；`DEFAULT_MAX_OUTPUT_BYTES`
  公开常量 5MB；readBounded/truncate 实例化按配置上限截断，标记文本携带实际上限值。
- `ToolsModule.Builder` 增 `runCommandMaxOutputBytes`（yml 键 `run-command.max-output-bytes`；
  非正数 BuzhouConfigurationException）；缺省 null → 工具默认。
- 沙箱委托版（SandboxRunCommandTool）输出治理归 backend 域，不重复实现（诚实边界）。

### Testing Decisions

- tools 单测（POSIX 门控，Prior art RunCommandToolTest）：10KB 上限 + 100KB 产出 → 标记可见 +
  体积贴近上限；缺省 5MB 行为钉住（6MB 产出截断）；非法上限构造拒绝。

### Out of Scope

- rlimit/cgroup 资源限额（纯 JDK 不可移植；沙箱档 SandboxLimits 已有，内置档黑名单+环境白名单
  +输出上限+进程树强杀即内置档的完整防线）。

## §B 配置校验补全（T158 / impl-129）

### Problem Statement

三处配置校验缺口：`buzhou.runaway.*` 与 `buzhou.backpressure.*` 全部数值/策略键零 fail-fast
（负步数、非法策略词静默落到下游派生）；`buzhou.webhook.max-attempts/outbox-capacity` 非法值
静默回退默认——配置错而不知，是「深水区运行时失败」的典型入口。

### Solution

越界/非法值启动即拒（BuzhouConfigurationException，消息含键名/收到值/合法形态）；宽容只留给
「未配置 null」（null = 不限/默认的既有语义不变）。webhook 两键静默回退改显式拒绝
（pre-1.0 破坏性变更，api-surface 入档）。

### User Stories

1. As a 应用开发者, I want 负数步数/非法策略词启动即被拒, so that 配置错误在启动期暴露而非运行期。
2. As a 应用开发者, I want null = 不限/默认语义不变, so that 既有合法配置零迁移。
3. As a SRE, I want 错误消息给出键名/收到值/合法形态, so that 排查无需翻源码。

### Implementation Decisions

- `BuzhouRunawayProperties`：perTurn/perSession 正整数、wallClock 正时长、perTool maxCalls 正整数、
  softThresholdRatio ∈ (0,1]、repetition.consecutive ≥2 + action ∈ {block,flag-only}、
  escalatePolicy = emit-event（当前唯一档）。
- `BuzhouBackpressureProperties`：maxConcurrentSessions 正整数、spawnQueueTimeout 正时长、
  策略词 ∈ {QUEUE,FAIL_FAST}（spawn 与 tool 两处；此前非法值静默归 QUEUE）、tool 组
  maxConcurrentPerTurn 正整数 / toolTimeout 正时长 / permitAcquireTimeout 非负（0=FAIL_FAST 等价）。
- `BuzhouWebhookProperties`：maxAttempts/outboxCapacity 非法值（<1）由静默回退默认改
  BuzhouConfigurationException；null 默认不变。

### Testing Decisions

- core 单测：合法全量构造通过 + 逐键非法值拒绝（消息含键名）；webhook null 默认不变/非法拒绝/
  有效值语义回归。

### Out of Scope

- JSR-303 注解化（仓库既有路线是构造器校验，保持一致）。
