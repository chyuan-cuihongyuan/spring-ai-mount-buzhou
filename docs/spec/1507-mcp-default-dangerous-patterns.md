# 1507 — MCP 危险工具默认动词模式（S1 硬偏差修复）

> 来源：M 会话第 8 轮 = effort #1507（impl 1110）。design-incompleteness S1 闭环：spec 14 §F 承诺落地。

## 背景

spec 14 §F 承诺 MCP 客户端侧危险工具模式「默认 delete/drop/write/update/remove/send/exec 类动词」，代码缺省空列表（`BuzhouMcpProperties` compact constructor null→`List.of()`）——评审定级硬偏差：恶意 server 的 `delete_*` 类工具默认不进任何登记面。

## 目标

- `BuzhouMcpProperties` 缺省（null/未配置）→ 七动词前缀 glob 默认集（`delete*`/`drop*`/`write*`/`update*`/`remove*`/`send*`/`exec*`）；
- 显式空列表 = 用户显式关闭（yml `[]` 绑定空 List 非 null——逃生门成立）；显式集透传；
- 影响面收敛：`dangerousToolNames()` 零执行面消费方（health size 读数变化），HITL 自动挂接为 S2（starter 编排）另轮处理。

## 兼容性

行为变化 = spec 承诺恢复（safe-by-default 叙事对齐）；显式配置用户（含显式空）零变化。
