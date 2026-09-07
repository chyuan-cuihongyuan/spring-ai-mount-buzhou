# Spec 330 — 告警静默窗与抑制规则（effort #330）

> wayfinder map：`.wayfinder330/MAP.md`（T651–T652）。C 会话第 31 轮。

## Problem Statement

312 的告警规则引擎只会「触发就通知」：计划内维护（如 320 舱容量热调整
必然引发短时 Bulkhead 告警）会把值班人叫醒；一次根因故障（store DOWN）
衍生的二级告警（依赖 store 的工具健康劣化）会形成通知风暴，真正的根因
反而被淹没。通知缺一层策略：什么时候故意不叫人，什么时候根因遮蔽衍生。

## Solution

`AlertGate`（core/health）作为告警引擎通知路径上的策略门：

- **静默窗（silence）**：按机制匹配（`*` = 全量）+ 到期时刻 + 备注。
  yml 声明（启动即生效）与运行时事故按钮（`silence`/`cancelSilence`，
  不重启即时生效）双入口；窗口内匹配机制的触发/恢复通知均被吞（状态机
  照常推进——吞的是通知不是事实）；过期惰性清理（评估路径顺带）；被吞
  留痕（WARN + `buzhou.alert.silenced` 计数）。
- **抑制规则（inhibit rule）**：yml 声明「源机制 firing 时，目标机制通知
  被抑制」。门自行维护 firing 视图（观察流经的全部触发/恢复——被抑制的
  告警仍参与视图，源恢复后目标新触发可再通知）；被抑留痕（WARN +
  `buzhou.alert.inhibited` 计数）。
- 引擎接线可选：未配置任何静默/抑制 = 门不存在，通知路径零变化。

## User Stories

1. 作为运维，我想在计划内维护前给相关机制开静默窗，所以维护引发的
   例行告警不会通知任何人。
2. 作为运维，我想在事故现场用运行时按钮一键静默某机制 30 分钟，所以
   不用改配置重启应用就能止住通知轰炸。
3. 作为值班人，我想让根因告警自动抑制它的衍生告警，所以 一次故障
   只收到一条根因通知而不是十条件生告警。
4. 作为值班人，我想在根因恢复后、衍生再次劣化时仍能收到通知，所以
   抑制不会永久吞掉衍生告警。
5. 作为审计者，我想让每条被吞/被抑的通知都留痕（日志 + 计数器），
   所以「没收到通知」和「从未发生」可以区分。
6. 作为使用者，我不想配置任何静默/抑制时行为与 312 完全一致，所以
   升级零风险。
7. 作为贡献者，我想静默窗过期后自动失效（无需人工清理），所以 运行时
   按钮开的窗不会永久残留。
8. 作为运维，我想查看当前活跃静默窗与 firing 视图（观测面），所以
   止不住通知时能定位是哪条策略在吞。

## Implementation Decisions

- 新公共类型 `AlertGate`（含 `Silence`/`InhibitRule`/`Silenced` 观测
  record）落 core/health；引擎 notify 路径插一道可选门。
- 门是自观察状态机：消费引擎的触发/恢复流维护 firing 视图，抑制判定
  基于该视图；静默与抑制判定相互独立，静默优先报告。
- yml 面：`buzhou.alert.silences[]`（mechanisms/duration/comment/created-by）
  与 `buzhou.alert.inhibit-rules[]`（source-mechanism/target-mechanism），
  与 312 的 `buzhou.alert.rules` 同前缀共属性类；声明才装配门。
- 运行时按钮沿用 325 kill switch 的事故响应先例：即时生效、不重启。
- 被吞通知不重放：静默期结束后不补发（诚实语义——吞了就是吞了）。

## Testing Decisions

- 门单测（伪时钟推进过期、firing 视图转换、`*` 匹配、恢复通知同吞、
  抑制解除后再通知）；引擎+门集成测（通知侧效应可数）；装配测
  （声明才装配、未配置引擎行为零变化）。先例：AlertRuleEngineTest /
  AlertRuleAssemblyTest。
- 只测外部行为：通知到达与否、留痕计数、观测面内容。

## Out of Scope

- Alertmanager 的 equal 标签子句/正则匹配器矩阵（机制等值匹配已覆盖本域）；
- 通知路由、分组、去重、升级策略（宿主 listener 职责）；
- 静默窗的持久化与跨重启（运行时窗是进程内事故工具；重启即清——
  yml 声明窗可重建）。

## Further Notes

- 与 312/321 的关系：312 管「何时报」，321 管「报多重」，本轮管「何时
  故意不报」——通知治理三层齐。
- 新公共类型随轮 regenerate API 快照（R30 起 Windows 本机可跑）。
