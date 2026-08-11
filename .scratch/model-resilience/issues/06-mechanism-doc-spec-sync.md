# 06 — 机制文档 + docs/spec 同步 + M2 预留位说明

**What to build:** 把韧性机制写入仓库正式 Spec（`docs/spec/` 新增韧性机制详设 + 更新 00-overview 总入口索引，遵守「改机制先改 Spec」）；补机制文档（叠加避让、Advisor order 生效位、配置矩阵、事件清单）；写明 M2 预留扩展位（熔断 / 降级链 / 路由宿主、07 限流 Advisor、08 转人工出口协同点）；release notes。

**Blocked by:** 05

**Status:** done

## 范围

- **`docs/spec/` 新增韧性机制详设**（对齐既有 9 机制详设格式）+ 更新 `00-overview` 总入口索引。
- **机制文档**：与底座 `spring.ai.retry` 的叠加避让、`ResilienceAdvisor` 的 order 生效位（01 实测后固定）、`buzhou.resilience.*` 配置矩阵、observability 事件清单（retry-attempted / retry-exhausted / timeout-fired / error-classified / content-refusal-detected）。
- **M2 预留位说明**：`ResilienceAdvisor` 为熔断 / 降级链 / 调用级路由（04 号票）的同炉宿主；`onModelError` 为 08「转人工出口」的挂载点；与 07「每模型 RPM/TPM 双桶 Advisor」的协同点。
- **交叉链接 + release notes**：`.scratch/model-resilience/spec.md` ↔ `docs/spec/` 互相引用；README/release notes 记录新模块。

## 验收

- [ ] `docs/spec/` 韧性机制详设落盘、`00-overview` 索引更新（遵守「改机制先改 Spec」）
- [ ] 机制文档覆盖叠加避让 / Advisor order / 配置矩阵 / 事件清单
- [ ] M2 预留扩展位写明（熔断 / 降级链 / 路由宿主、07/08 协同点）
- [ ] `.scratch/model-resilience/spec.md` 与 `docs/spec/` 交叉引用一致

## 备注

- 管辖 Spec：`.scratch/model-resilience/spec.md`；落地纪律见 `docs/production-readiness/README.md`「落地纪律」（改机制先改 Spec、星形依赖、行为变更带测试）。
- 本票是整组收口票：待 01–05 行为全部落地后，把最终机制同步进正式 Spec 与文档。
