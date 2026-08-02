# Hook→state→Attachment 上下文联动闭环

Type: grilling
Status: resolved
Blocked by: 06, 23

## Question

「补失忆」的联动闭环如何抽象成框架能力：会话级 state（Hook 采集的事实——如「改了表」「产出了图」）的数据模型与存储（挂持久化 SPI？）；Attachment 注入机制——下一轮注入模型前，把 state 中待消费的事实渲染进 prompt（注入位置、格式、消费后清除还是累积）；业务如何声明采集规则（框架给「afterTool 采集 → state → 下轮注入」的通用脚手架，业务填判定与渲染逻辑？）；与九段式摘要的交互（Attachment 事实要不要进摘要的 Current State 段？）；注入内容的 token 预算归属？

## Answer

**定案：通用 KV 事实模型 + ttl + system-reminder 注入 + 三要素脚手架 + 进摘要固定扣除。**

1. **数据模型**：SessionStateStore（06）上建通用事实模型 `{key, value, producer(hook 名), createdTurn, ttl(轮次)}`；Hook ctx 暴露读写句柄（23）；key 命名空间约定（`fact.*` 事实 / `auth.*` 授权标记等），不建专项表。
2. **Attachment 注入**：注入前统一渲染——待消费事实渲染为 `<system-reminder>` 块插在近期原文前（与 09 摘要插入位一致）；ttl 语义：剩余轮次内累积注入、过期自动停注；ttl=1 即一次性消费，大 ttl 即持久累积，无需独立「消费/累积」双模式。
3. **业务声明**：内置 `FactCollectorHook` 脚手架——业务注册三要素：判定器 `ToolCallContext → Optional<fact>`、渲染器 `fact → prompt 文本`、ttl；框架管存储、注入、过期。
4. **摘要/预算交互**：Attachment 事实写入九段式摘要的 Current State 段（09 已定 P0 死保），压缩不丢现场；注入占用计入 07 动态预算「系统提示词一侧」的固定扣除项，不挤历史预算。
