# Wayfinder Map — Buzhou 告警静默窗与抑制规则（effort #330，C 会话第 31 轮）

> C 会话第 31 轮。312 立了告警规则（触发/恢复双向 + flap 吸收），但通知
> 侧无策略层：维护窗口期间调整舱容量（320 热调整）会例行触发 Bulkhead
> 告警；根因（如 store DOWN）衍生告警（工具健康劣化）会成刷屏风暴。
> Prometheus Alertmanager 的静默窗（silence）与抑制规则（inhibit rule）
> 正是这两类通知治理的成熟解。

## Destination

`AlertGate`（通知策略层）落进 core/health：静默窗（yml 声明 + 运行时
事故按钮 + 惰性过期，吞通知不吞状态）+ 抑制规则（源机制 firing 时目标
机制通知被抑制——根因遮蔽衍生）；引擎 notify 路径接一道门（未配置零
行为变化）；装配面 `buzhou.alert.silences` / `buzhou.alert.inhibit-rules`。

## Notes

- 号段：spec 330 / T651–T652 / impl-353。
- 借鉴源：Prometheus Alertmanager（silence window / inhibition rule）。
- 纪律：默认未配置 = 零行为变化；被吞/被抑通知必须留痕（WARN + 计数器），
  不做静默黑洞。

## Decisions so far

- 门放引擎 notify 路径（观察先于判定：被抑制的 FIRING 仍参与抑制源的
  firing 视图，源恢复后目标新触发可再通知）。
- 静默匹配以机制为面（`*` 全量）；过期惰性清理（评估路径顺带，无定时器）。

## Out of scope

- 按标签矩阵的通配抑制（Alertmanager equal 子句全等语义——机制等值已覆盖
  本域需求）；通知路由/分组/去重（宿主 listener 职责）。

## Tickets

- [x] [T651 AlertGate 静默+抑制状态机](../tickets/T651-alert-gate.md)
- [x] [T652 引擎接线 + yml 装配 + 收口](../tickets/T652-gate-assembly.md)
