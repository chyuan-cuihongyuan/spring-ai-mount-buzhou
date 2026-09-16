# Spec 2030 — 重定向预算（effort #2030，R31）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3161–T3162，impl 1581）。
> 借鉴：curl max-redirs + 浏览器环检测——跳数预算与环双防线。

## Problem Statement

HTTP 工具跟随重定向若无防线：恶意/失控服务端可无限 302 拖死客户端
（跳数无界）；配置错误的 A→B→A 环即使预算大也会空耗——预算防拖死、
环检测防空耗，两者缺一。

## Solution

`RedirectBudget`（buzhou-tools http，单请求一件非共享）：

- 构造：maxRedirects ≥ 0（0 = 不跟随任何重定向；fail-fast）；
- `startFrom(initialUrl)`：锚定起点（环检测基点入访问集）；
- `decide(nextUrl)` 三态：跳数 ≥ 预算 → BUDGET_EXHAUSTED（停，返回
  当前响应）；nextUrl 已在访问集 → LOOP_DETECTED（环识破——服务端
  配置错误信号，环跳不记账）；否则 FOLLOW（hop + 访问集记账）；
- 读数：hops / visitedCount；契约：URL 非空非白 fail-fast。

## User Stories

1. 作为 HTTP 工具作者，max-redirs=5 拖不死——预算硬界。
2. 作为排障者，LOOP_DETECTED 直指服务端配置环——不用等预算空耗。

## Testing Decisions

- 预算内跟随；恰第 N+1 跳 BUDGET_EXHAUSTED（记账不超）；A→B→A 环
  在预算前识破（环跳不记账）；零预算首跳即拒；起点锚定（跳回起点
  也是环）；自指环立即识破；畸形六型 fail-fast。

## Out of Scope

- 不做跨请求共享环缓存（单请求口径）；不接 http 工具执行链（接线
  归后续轮）。

## Further Notes

- 与 ArgvBudgetGate（入参预算）同族：一守入参规模，一守出站跳数。
