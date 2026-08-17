---
Type: task
Status: closed
blocked-by: T173-turn-feedback.md, T175-weighted-canary.md, T176-shadow-fork.md, T177-model-pool-quota.md
---
## Question

G22 反馈捕获轨迹（rateTurn→事件序列）/ G23 加权金丝雀（同会话稳定路由）/ G24 shadow 隔离（主链路 span 不受 shadow 影响）；事件面缺席处以可观测 API 断言，口径修正入档。

## Resolution

spec 51 §A / impl-150 落地：G22 反馈捕获（chat→rateTurn→turn.feedback 事件载荷完整 +
state 落键可枚举）/ G23 金丝雀稳定（30 会话样本：同会话两轮同源、canary.selected 恰一次、
9:1 分流宽幅 15-30）/ G24 shadow 隔离（用户回复来自主模型、shadow.compared 到达、
探测零 fallback 语义）。examples 3 新测试绿 + 模块全绿。
