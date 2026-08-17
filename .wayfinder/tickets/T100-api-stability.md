---
Type: task
Status: closed
assignee: zcode
blocked-by: T82,T85,T87,T88,T89,T90,T91
---
## Question

API 稳定性审计怎么做？决策点：public API surface 清单生成（模块 × public 类/接口清单落档 docs/api-surface.md）、关键接口 javadoc 补齐（AgentRuntime/AgentSession/Hook 链/SPI 五接口）、@since 标注规范（1.0.0 基线）、internal 包约定核查（public class in internal package 清单）、deprecation 政策写入 CONTRIBUTING。产出 spec 23 增量 + impl 75。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **`docs/api-surface.md` 自动生成**：14 模块 × 非 internal 包 public 类型清单（404 项，脚本 grep 生成可重跑）；starter 显式标注零类型。
2. **internal 审计**：36 个 public-in-internal 类型——契约声明为「实现细节非公开 API」（Java 无包结构强制，internal 命名 + 清单即契约）；dashboard 测试违例已在 effort#4 修复，无新违例。
3. **javadoc 核查结论**：AgentRuntime/AgentSession/BuzhouHook 链/五大 store SPI javadoc 在既往轮次已齐（本轮历次新增类型均带 javadoc）——不追溯补标 @since（0.1.0-SNAPSHOT 预发布基线，政策定为 1.0.0 起标）。
4. **政策写入 CONTRIBUTING**：语义化版本（minor 只加不改）、废弃 ≥2 minor + @deprecated 注替代、internal/fromYml map 契约不受约束。
