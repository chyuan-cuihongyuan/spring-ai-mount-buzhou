# Spec 48 — 反馈导出衔接与加权金丝雀（effort #10）

> effort #10 第三篇。§A：反馈导出与评估衔接（T174）；§B：加权金丝雀降级链（T175）。
> 外部事实源：Langfuse（~31K★）Prompts/评估数据集（负反馈回流评估的运营范式）；
> LiteLLM Router（~26K★）deployment weight 加权抽取（simple-shuffle 金丝雀语义）。
> 本篇 §B 把 LiteLLM 的随机加权收窄为**会话稳定哈希加权**（同会话不漂移）。

## §A 反馈导出与评估衔接（T174 / impl-143）

### Problem Statement

T173 落地的反馈只在 state store 里——会话导出文档不含反馈段，跨环境移植丢反馈；
负反馈轮次没有显式标记，离线评估筛选要自己解析编码值。

### Solution

`FeedbackExporter`（SessionExportExtension，段名 `core.feedback`）：导出时 scanByPrefix
解码反馈条目并给出 `negative` 极性标记与 `negativeTurnSeqs` 汇总（评估集筛选用）；
导入按原键回放（键含轮次归属，时序可排）。

### User Stories

1. As a 评估工程师, I want 导出文档带反馈段与负反馈轮清单, so that 负反馈轮可直接回流评估集。
2. As a 运维工程师, I want 反馈随会话导入跨环境移植, so that 排障环境能重放用户视角。
3. As a 宿主开发者, I want 无反馈会话导出零变化, so that 既有导出消费方不受影响。

### Implementation Decisions

- 段名 `core.feedback`（模块前缀惯例）；导出 = scanByPrefix(`buzhou.feedback.`) 解码为
  `{key, turnSeq, type, value, comment, source, at, negative}` 行 + 顶层
  `{entries, negativeTurnSeqs}`；空反馈返回 null（段不携带，既有导出零变化）。
- 极性标记 `negative`：boolean 型 value=false、numeric 型 value<0 为负（显式负信号）；
  categorical 无极性假设（不标）。`negativeTurnSeqs` = 负反馈轮次去重升序列表。
- 导入：按原 key 逐条 put（producer=turn-feedback、createdTurn=turnSeq 保留；at 用行内值重建
  失败容忍——StateEntry.updatedAt 取导入时刻，at 字段在行内保留）；键冲突（keepIds 场景）按
  state store put 语义覆盖（幂等）。
- FeedbackExporter 挂 Spring 装配（@Bean 进 ObjectProvider<SessionExportExtension>，
  与 memory.facts 同通道）；`FEEDBACK_PREFIX` 常量收敛到 FeedbackExporter（单一事实源）。
- 编程式宿主：`runtime.setExportExtensions(List.of(new FeedbackExporter(stores.sessionStateStore())))`。

### Testing Decisions

- core：反馈（正/负/categorical 混合）→ 导出段断言 entries 字段 + negative 标记 +
  negativeTurnSeqs；导入回放到新 sessionId 键值保留；无反馈导出段缺席；往返保真
  （导出→导入→再导出等价）。
- 既有 SessionExport/import 测试回归（扩展段空缺零影响）。

### Out of Scope

- 评估集自动回流管道（本篇只到「可筛选的导出面」；自动化进 fog 候选）。
- 反馈进 OTel/dashboard（沿 T173 边界）。

## §B 加权金丝雀降级链（T175 / impl-144）

### Problem Statement

降级链只按序被动降级——无法做「新模型小流量试水」：想给 5% 会话先走候选模型，
要么改主配置全量切、要么自建旁路；主/备选择对同一会话也不稳定（无从谈起会话级亲和）。

### Solution

降级链条目可配 `weight`（默认全 1 = 既有按序语义不变）；启用 `canary-enabled` 时，
**首次**模型调用的初始目标按会话稳定哈希在「主模型 + 备模型」候选池内加权抽取
（同会话粘住首个选择，不随轮次漂移）；候选失败仍按链序回退（权重不影响回退顺序）。

### User Stories

1. As a 平台工程师, I want 给候选模型配 5% 权重, so that 新模型小流量试水再全量。
2. As a 平台用户, I want 同一会话始终同一模型, so that 语气一致性不被 A/B 撕裂。
3. As a 运维工程师, I want 权重不影响故障回退, so that 金丝雀安全（失败仍按序降级）。
4. As a 宿主开发者, I want 不配权重零变化, so that 既有部署行为不变。

### Implementation Decisions

- `NamedFallbackModel` 增第三分量 `weight`（Integer，null/≤0 归一 1；兼容双参构造保留）。
- `ResilienceProperties.Fallback` 增 `canaryEnabled`（默认 false）与 `weights`
  （Map<modelName, weight>，按名套用到链条目——配置态而非构建态，避免 NamedFallbackModel
  构造面膨胀）。
- 选择算法：候选池 = [primary(weight 1 或 weights 配置) + fallbacks]；累计权重区间上取
  `hash(sessionId) % total`（String.hashCode，稳定；文档钉住不换算法——换算法 = 存量会话
  全体漂移一次）；选择结果按会话缓存（advisor 内 per-session 首选记忆，会话终结随会话对象
  消亡）。
- 生效面：advisor 首次调用选初始目标（主或某备）；所选即「本次的主」，其终态失败/熔断
  按链序试剩余候选（含原主模型）——池内循环一次、不回头重复。
- 可观测：`fallback.switched` 事件与 `buzhou.resilience.fallback-switches` 计数沿既有面；
  新增 `canary.selected` 会话事件（首选落点，sessionId + model，一次性）。
- 未启用 canary（默认）：行为与现状逐字节一致（主模型直连，链序降级）。

### Testing Decisions

- 选择分布：大样本（≥1000 会话）权重比例断言（±3pp 宽幅）；同会话多轮稳定（首选粘住）。
- 回退正确性：金丝雀选中备模型失败 → 按链序（跳过已试）回退含原主模型；全败上抛主因。
- 默认关：不配 canary 时主模型直连（既有测试全量回归）。
- 会话终结：per-session 首选记忆不泄漏（随 advisor 会话态消亡——advisor 是 per-session 构造
  或按 sessionId 分桶，以实现为准并测试钉住）。

### Out of Scope

- 按延迟/用量的动态路由（LiteLLM latency-based 策略；观测面不足，fog 候选）。
- 权重热更新（配置 refresh 语义；后续按需）。
- shadow 流量（T176 正题，与金丝雀正交）。
