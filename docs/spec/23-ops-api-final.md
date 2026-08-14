# Spec 23 — 运维文档 / 多实例语义 / API 稳定性 / 终验（mechanisms）

> effort #5（T97–T102 / impl-72~77）。

## 运维 runbook（T97 / impl-72）

`docs/ops-runbook.md` 七节：部署形态与存储选型 / 故障排查树（症状→定位→处置，锚定真实
事件名与指标名）/ 配置调优表（高频键+场景）/ 容量规划 / 升级与回滚（BOM+迁移链+机制级
开关灰度）/ 多实例诚实边界 / 告警项清单（指标→阈值→动作）。SRE 接手第一站文档。

## 多实例语义显式化（T99 / impl-74）

- **文档化**：docs/ops-runbook.md §6——单进程组件清单（限流桶 / 熔断器 / 日配额 /
  InMemory 审计环 / SpawnGate）、多实例实际行为（每实例独立额度）、推荐部署（粘性路由 +
  租约独占 steal 接管）、分布式 out-of-scope 声明。
- **启动告警**：resilience auto-config 检测多实例信号（`buzhou.store.type != memory`）
  且启用任一单进程机制（限流/日配额/熔断）→ 启动 WARN 一次指向 runbook §6；
  不做配置拒绝（知情即可，粘性+独占是合法形态）。
- RunawayCounters 会话累计本就持久化在 SessionStateStore（跨实例语义正确），不在告警之列。

## API 稳定性审计（T100 / impl-75）

- **`docs/api-surface.md`**：14 模块 public 类型清单（404 项，脚本可重跑）；starter 零类型显式标注。
- **internal 契约**：36 个 public-in-internal 类型声明为实现细节（非公开 API）。
- **@since**：0.1.0-SNAPSHOT 预发布期不追溯；1.0.0 起新公开类型起标。
- **政策**（CONTRIBUTING 同步）：语义化版本（minor 只加不改）；废弃 ≥ 2 个 minor +
  `@deprecated` 注替代；`*.internal.*` 与模块 `fromYml(Map)` 私有契约不受约束。
- javadoc 核查：关键接口（AgentRuntime/AgentSession/Hook 链/五 store SPI）既往轮次已齐。

## 全仓终验与收口（T101/T102 / impl-76/77）

- （终验时回填）
