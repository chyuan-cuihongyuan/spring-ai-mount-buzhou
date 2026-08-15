---
Type: task
Status: closed
---
## Question

降级链条目可配 weight（默认全 1 等价现状）；选路按 session 稳定哈希（同会话不漂移）；熔断/失败仍按序回退不受 weight 影响（weight 只影响健康候选的首选）。LiteLLM Router simple-shuffle 语义收窄为稳定加权。验证：权重分布单测 + 同会话稳定性断言。

## Resolution

spec 48 §B / impl-144 落地：FallbackChain 增 canary-enabled + weights（配置态权重，未列名默认 1）
+ selectInitialTarget（String.hashCode 会话稳定哈希加权——LiteLLM simple-shuffle 收窄为会话稳定，
算法钉住不换）；ResilienceAdvisor per-session 首选记忆（advisor 每会话构造，零泄漏面）+
canary.selected 事件每会话一次 + 金丝雀路径（目标熔断闸 + 单次 deadline + 终态独立记账）+
degradeFromCanary（候选 = [主 + 备链] 跳过已试目标、主模型在链首位、单次尝试、全败上抛所选目标
原始错误）。未启用（默认）行为与现状逐字节一致。4 测试绿（分布/粘住/回退/事件/默认关），
resilience 91 测试全绿。
