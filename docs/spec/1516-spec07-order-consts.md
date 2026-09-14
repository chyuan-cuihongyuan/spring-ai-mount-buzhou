# 1516 — spec 07 七切面回写 + Advisor/Hook 序位常量化

> 来源：M 会话第 18 轮 = effort #1516（impl 1119）。design-incompleteness 四-5 闭环 + 五-6 order 部分闭环。

## 背景

- 四-5：spec 07 三处「六切面」未回写（spec 15 落地 onModelError 后已扩七）。
- 五-6：四处 advisor/hook 序位魔法值（+450/+460/100/200）散落——同模块 ResilienceAdvisor.CHAIN_ORDER_OFFSET 已抽常量先例。

## 目标

- spec 07 三处六切面 → 七切面（口径追认）；
- 四处序位抽 static final 常量（ADVISOR_ORDER_OFFSET / HOOK_ORDER）+ 链位注释；行为零变化（同值）。

## 兼容性

纯文档 + 等值重构零行为变化；spill 默认值散落（2048/20/32000 四处）留独立轮。
