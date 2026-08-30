---
Type: task
Status: closed
---
## Question

BuzhouChatMemory.load 三处无 catch——消息读失败即 Turn 失败，无降级模式：是否提供可配读降级（如 empty-继续，WARN+事件可感）？默认语义如何保持不变？

## Resolution

AFK 自决：是，可配降级。ReadDegradePolicy（OFF/EMPTY 公共枚举）+ ReadDegradeHolder 全局默认
（Holder 模式同 ToolResultLimiterHolder）+ BuzhouChatMemory 三读统一路由 loadHistory（EMPTY=
WARN+计数+空历史；OFF=原样上抛默认）+ buzhou.store.read-degrade 属性（非法值 fail-fast、
auto-config 初始化 bean 下发 Holder，任何 store 形态生效）。降级可感不静默；摘要/状态读出界。
产 spec 42 §B + impl-127。
