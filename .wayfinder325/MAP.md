# Wayfinder Map — Buzhou 工具紧急停用（effort #325，C 会话第 26 轮）

> C 会话第 26 轮。工具出事故（坏版本/越权行为/下游污染）时，现有手段都
> 是"结构性"的（熔断要等错误率凑够、干跑要预先开窗）——缺一个人工一键
> 「现在就停这个工具」的事故按钮，且不重启。LaunchDarkly kill switch /
> K8s cordon 思想；热更新通道复用 320 的刷新事件。

## Destination

`ToolKillSwitchHook`（order 15，维护门 10 后）：beforeTool 拦已停用工具
（「[工具已停用]」非错误标记——人为停用≠失败，不污染熔断/预算）；
运行时 disableTools/enableTools/clearAll（事故按钮）；监听
`BuzhouConfigRefreshEvent` 热重读 `buzhou.tool-kill-switch.tools`（320
通道）。装配恒在（空集 = 直通零变化——事故按钮必须预先存在才有用）。

## Notes

- 号段：spec 325 / T641–T642 / impl-348。
- 借鉴：LaunchDarkly kill switch；K8s cordon（标记式隔离，不动实体）。

## Decisions so far

- 恒装配（区别于混沌/干跑 opt-in：它们会主动做事，本开关空集纯直通，
  且事故价值要求按钮预先在场）。
- 停用集运行时可变（volatile Set 不可变副本替换）。
- yml 列表 = 启动预停用 + 每次刷新事件整体重读覆盖运行时改动（yml 是
  事实源——诚实语义入档）。

## Out of scope

- 按会话/按租户粒度停用（全局面）；停用审计事件（事件族可后接）；
- 恢复审批流。

## Tickets

- [x] [T641 ToolKillSwitchHook + 热重载 + 装配](tickets/T641-kill-switch.md)（impl-348）
- [x] [T642 回归与收口](tickets/T642-kill-switch-close.md)（impl-348）
