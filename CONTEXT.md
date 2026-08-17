# CONTEXT

项目领域术语表（只收术语，不收实现细节）。

## 核心概念

- **Harness（马具）** — 挂载在 Spring AI 与业务 Agent 之间的运行时中间层；本项目的定位。叠加而非替代 Spring AI。
- **Agent 运行时（Agent Runtime）** — 让单个 Agent 稳定、可控、可解释地跑在生产里的那层能力：记忆、溢出保护、可观测、能力供给、并发执行。
- **会话（Session / Conversation）** — 一次 Agent 与用户的完整多轮交互，以 sessionId 标识，可跨实例续接。
- **轮次（Turn）** — 一次用户输入到 Agent 最终回复的完整往返；内部可能含多轮"思考—工具调用"递归。
- **完结轮次（Completed Turn）** — 工具调用链完整结束、不再有在途调用的轮次；微压缩的原子单位。

## 记忆与压缩

- **渐进式压缩（Progressive Compaction）** — 信息从高精度原文连续、分级地降级到高密度摘要，永不断崖式丢弃。
- **微压缩（Micro-compaction）** — 纯内存、不调 LLM 的工具结果回收：旧工具返回替换为带证据指针的占位符。
- **证据指针（evidence-id）** — 微压缩占位符中指向持久化层原始工具返回的标识，供排障回查。
- **九段式摘要（Structured Summary）** —  LLM 按固定九段模板生成的结构化对话摘要，段落带优先级（P0 死保，P3 先砍）。
- **动态预算（Dynamic Budget）** — "先扣后算"：窗口减去输出预留、安全缓冲、系统提示词、工具 Schema、当前输入后，剩余才是历史预算。
- **悬空调用（Dangling Tool Call）** — 进程中断导致工具调用发出而结果未落库的残缺消息，加载历史时需自动修复。

## Spill

- **Spill（溢出保护）** — 超大工具返回值自动落盘持久化，上下文中只留预览 + 说明 + 回读路径。
- **回读（Read-back）** — 模型持 spill 路径主动取回数据，支持范围读取（字节区间 / JSON path / 分页）。

## 可观测

- **Span** — 有始有终、可嵌套的执行区间（会话 ⊃ 轮次 ⊃ 模型调用/工具调用）。
- **Event** — Span 内部的关键瞬间：思维链（Thinking）、最终回复、工具入参/出参、错误。
- **认知可观测（Cognitive Observability）** — 记录模型基于什么证据、做出什么推理、得到什么结论，而不只是"调用发生了"。

## 能力供给

- **Skill（技能）** — 按需加载的能力单元；上下文只放清单（name + description），需要时再取正文。分内置（classpath）与 DB 动态两种来源，同名 DB 覆盖内置。
- **MCP 热插拔** — 工具集由配置驱动、运行时热更新；靠差量刷新 + 引用计数延迟关闭保证安全。
- **原子工具（Atomic Tools）** — 框架内置的最小可复用工具集：文件读写、命令执行、HTTP 调用、任务清单等。

## Hook 护栏

- **Hook 链（Hook Chain）** — 框架在模型调用与工具调用前后暴露的 Callback 切面（beforeTool/afterTool/beforeModel/afterModel 等）；护栏逻辑挂于其上，与推理循环解耦。
- **引用句柄（Reference Handle）** — 长内容落盘后留在上下文中的指针文案（含路径与操作指引），LLM 凭句柄按需回读或编辑。
- **Onload（写侧加载）** — 工具执行前由 Hook 从文件加载长内容全文、覆盖工具入参的写侧护栏；与读侧 Offload 对称。
- **失败语义非对称** — 读侧 offload 失败降级透传（不阻断），写侧 onload 失败阻断调用（杜绝残缺产物外流）。
- **HITL 门禁（Dangerous Tool Guard）** — 配置驱动的危险工具拦截：未获真实用户授权，不可逆操作在框架层物理走不通；授权以 state 标记放行。
- **Hook→state→Attachment 闭环** — 补失忆范式：Hook 确定性采集事实写入会话 state，下一轮注入模型前以 Attachment 渲染进 prompt，不靠 LLM 自觉。

## 韧性与成本（effort #5）

- **熔断（Circuit Breaker）** — 按 modelName 分桶的进程级失败率闸门：CLOSED/OPEN/HALF_OPEN 三态，OPEN 期调用零重试快速失败，冷却后单探测恢复。
- **备模型降级链（Fallback Chain）** — 主模型终态失败或熔断 OPEN 后，在同一逻辑调用内按序切换备模型；全败上抛主因。
- **会话预算（Session Budget）** — 会话生命周期累计的 token/成本硬顶（microUsd 整数口径），超限拦截下一次模型调用。
- **日配额（Daily Quota）** — per-session 的 turns/tool-calls/tokens 每日限额，UTC 自然日窗口重置。
- **REASK** — 结构化输出解析失败后携带解析错误反馈的重问一次语义（诚实计入轮次预算）。
- **会话分支（Fork）** — 复制源会话全部历史开新会话；State 不复制（预算重置 = 重试语义）。
- **事件外发（Webhook Forwarder）** — 会话事件 at-least-once HTTP 投递（HMAC-SHA256 签名 + 事件幂等键）。
- **工具集漂移（Tools Drift）** — MCP server 端工具列表变更与本地基线的差量（协议 tools/list_changed 订阅）。

## 数据生命周期与可移植（effort #6）

- **持久化 Outbox** — 事件外发前的持久暂存队列（state store 合成会话）：跨重启不丢、记录级退避、死信隔离；at-least-once + 幂等键契约。
- **冷却自适应退避** — 熔断连续跳闸驱动的冷却指数放缓（×2^(trips-1) 封顶 backoff-cap）；探测成功即复位。
- **证据引用计数（Evidence Refcount）** — fork 对源会话 spill 证据的引用登记：源删除被引用证据保留，最后引用者关闭才物理删（EVIDENCE_GONE 容错悬垂读）。
- **媒体引用（MediaRef）** — 多模态输入的 URI 引用形态（mimeType + uri）；只随最近一条带媒体消息重发，历史轮降级文本标记。
- **会话导出/导入（SessionExport）** — 单 JSON 文档承载 messages+summary+state 的跨环境移植面；导入默认 Id 重映射。
- **fsck（Store Integrity）** — 五 store 跨槽对账工具：孤儿摘要/残留 state/泄漏租约/悬挂观测，只读报告 + 按项可选修复。
- **会话索引（SessionIndex）** — 五 store 之外的枚举/过滤查询面（生命周期维护、最终一致；未装配零影响）。
- **工具结果限幅（Result Limit）** — 工具结果入模型上下文前的字符上限（默认 20K + 提示尾 + per-tool 豁免）。
- **黄金轨迹（Golden Trajectory）** — 机制行为的「脚本化输入 → 事件序列断言」回归集（与红队/perf 互补）。

## 工程闭合与防线加密（effort #7）

- **探测槽位不变量** — 熔断半开「在飞探测数 + 已成功数 ≥ 阈值」即占位满员（每成功永久占一槽；连续 N 成功才恢复）。
- **目录注入预算** — skills 清单注入上限（默认 64）+ 溢出提示（「另有 N 个未列出」）。
- **媒体摄取（MediaIntake）** — 字节 → spill 落盘 → MediaRef URI 的闭环（Latin-1 双向无损）。
- **导出扩展段（Export Extension）** — SessionExport.extensions 模块自定义段（如 memory.facts）；导入回放最终一致。
- **DELETED 索引态** — 会话删除级联把索引行置 DELETED（审计留存；默认列表排除，显式过滤可查）。
- **前缀扫描（scanByPrefix）** — state store 键前缀查询面（JDBC/Redis 下推；outbox 消全量读放大）。

## 能力补全与对抗面（effort #8）

- **技能检索（skill_search）** — 目录截断外的运行时子串检索（不截断全集 listAllFor；命中后 load_skill 加载）。
- **死信重放（replayDeadLetters）** — 死信一键迁回 outbox（attempts 清零重投；at-least-once 契约内可能再死信）。
- **索引保留（closed-retention）** — CLOSED/DELETED 行过期惰性清扫（1/64 概率 ≤256 条；ACTIVE 永不扫；-1 永久）。
- **梯子级事件（evictRatio）** — 压缩梯子每级折入都通知，payload 携带当前级比例（级可区分）。
- **跨 store 迁移（SessionMigrator）** — 会话级搬迁复用 export/import 管线（重映射/keepIds）。
- **阻塞背压（观测管线）** — 满队阻塞而非丢弃（at-least-once 不丢；queue.wait Timer 可观测）。

## 静态安全与运行时确定性（effort #9）

- **落盘加密（SpillCipher）** — spill 数据文件 AES-256-GCM 信封加密（魔法前缀 wire 格式、随机 IV）；
  encryption-key 配置即开、缺省关；旧明文文件兼容读，密钥错配快速失败。
- **会话单飞闸（Single-flight Gate）** — 同会话在途轮次未终结时，第二个轮次入口确定拒绝
  （TURN_IN_FLIGHT / NON_RETRYABLE）；终结释放；跨进程仍归租约门。
- **轮换持久化（写而后切）** — 审计签名密钥 rotate 先落盘后切换（失败中止 active 不变）；
  key-dir 约定命名（v<version>.pem）目录扫描重启自动入环。
- **链外锚定（Chain Anchor）** — 校验报告给出链头哈希，运维链外保存期望锚点；链内部一致但
  链头与锚点不符 = 删尾/整链重写可检（纯内部校验盲区的补检面）。
- **时钟注入（Clock Injection）** — 熔断冷却与配额 UTC 日窗的时间行为经可注入 Clock 驱动
  （缺省 systemUTC 零变化）；时间行为测试零真实等待。
- **迁移防护（Migrator Guards）** — 未来版本拒绝（旧构建对新高版本库启动即拒）+ 已应用脚本
  checksum 锚定（事后改动可检；存量行首升回填）。
- **读失败降级（Read Degrade）** — 消息历史读失败可配降级空历史续聊（read-degrade=empty；
  WARN + 计数可感不静默）；缺省 off 上抛不变。
- **输出兜底上限（Output Cap）** — run_command 输出内存兜底可配（max-output-bytes，缺省 5MB；
  截断标记可见）——OOM 防护层，与 Spill offload 的上下文治理层互不替代。

## 运营可观测与流量治理（effort #10）

- **TTFT/TPOT** — 流式体验双指标：首内容信号时延（time to first token）与每输出 token 间时延
  （time per output token）；timer 预注册 + STREAM_FIRST_TOKEN 事件（空块不触发）。
- **流取消三路分类** — `buzhou.stream.cancelled` 计数按 client（下游取消）/ deadline（超时）/
  guard（护栏终结）三标签分流；取消不再是一枚笼统计数。
- **慢滴流累计上限（stream-total-timeout）** — 相邻信号间隔 timeout 挡不住的「每 9s 滴一字」
  慢流由累计时长上限兜底（缺省 10m，≤0 显式关）。
- **MDC 会话轮次关联** — chat 调用线程日志携带会话/轮次两键（try/finally 必清）；stream 路径
  因信号切线程清错线程的结构性限制不入（裁定入档）。
- **turn 反馈（rateTurn）** — 轮次级反馈捕获 API：boolean/numeric/categorical 三型校验、
  state store 持久化（`buzhou.feedback.` 前缀可 scan）、`turn.feedback` 事件外发。
- **反馈导出（FeedbackExporter）** — core.feedback 导出扩展段：负反馈标记 + negativeTurnSeqs
  汇总，衔接评估数据集；无反馈时空段缺席。
- **加权金丝雀（Weighted Canary）** — 降级候选池按会话稳定哈希加权抽取初始目标（同会话粘住）；
  目标失败按链序回退含原主模型；默认关。
- **shadow 探测（Shadow Fork）** — 主调用成功后异步裸调用对照模型（不重放工具循环）；
  并发 + UTC 日预算双护栏；`shadow.compared` 事件；默认关零提交。
- **池配额（Pool Quota）** — 降级/金丝雀候选统一过 RPM/TPM 限流闸、按实际服务模型记账；
  remaining gauge 可感余量。
- **错误码统一（ErrorCode 收口）** — 泛化 throw 渐进挂码（SPILL_IO_FAILED/STORE_READ_FAILED/
  SKILL_OPERATION_INVALID 新码）；断言类 ISE 保留面钉住不迁。
- **未订阅流惰性化** — stream 轮次占用以 Flux.defer 惰性化：未订阅零占用、零计数残留。

## 评估闭环（effort #11）

- **评估数据集（EvalDataset）** — 命名集合 + 带溯源评估项（input/expected/sourceSessionId+
  sourceTurnSeq）；state store 合成会话 `__buzhou.eval__` 持久化，跨重启不丢、fsck 天然豁免。
- **负反馈回流（Feedback Import）** — 会话负反馈轮一键转评估项（isNegative 单一事实源口径）；
  幂等去重；无 assistant 回复轮跳过（不造空期望项）。
- **评估器（Evaluator）** — 判定 SPI：内置 EXACT/CONTAINS/REGEX；宿主实现接口即得领域断言；
  LLM-as-judge 留口不内置不做门禁。
- **评估 run（EvalRun）** — 数据集批次执行：项粒度会话隔离、顺序执行、单项异常不断批；
  三态逐项记录（pass/fail/error）+ passRate 汇总落 store。
- **评估完成事件（eval.run.completed）** — run 收尾外发（webhook 同通道零改造）；
  空集 run 不发（事件语义 = 评估完成而非建档）。
- **自定义事件出口（emitEvent）** — AgentSession 公共面：宿主领域事件与会话事件同通道派发。

## 精确响应缓存（effort #12）

- **精确响应缓存（Exact Response Cache）** — 同请求（model + 注入后 messages + options 采样）
  二次调用命中短路：零模型调用、零 token、不进熔断窗、无 MODEL_CALL span（诚实：没调模型）。
- **终态缓存边界** — 只缓存无 toolCalls 且内容非空的终态响应（工具副作用安全）；
  LiteLLM 代理层无此约束，agent harness 特有。
- **流式组装缓存** — 流式完整聚合（内容+usage+finishReason）后写；命中 Flux.just 重放；
  取消/错误不写半截。
- **LRU+TTL 惰性过期** — 容量逐出与 TTL 过期都计 evicted；过期不返回陈旧；无后台线程。

## 配置与公共面治理（effort #13）

- **配置绑定矩阵（Bindings Matrix）** — 全模块 metadata 键经真实装配路径逐一绑定断言；
  「键存在但静默不生效」类缺陷（键名/组件漂移、绑定构造器缺失）在 CI 必红。
- **公共面快照（API Snapshot）** — 非 internal public 类型全集黄金快照（466 类型 × 13 模块）；
  新增/移除公开类型未入档即失败；更新流程 = regenerate → 核对 diff → 文档同步。
- **env 直读面** — 无 @ConfigurationProperties 的模块键（guard/memory/tools/leak 等）经
  Environment 直读消费——矩阵以 env 等值断言覆盖（装配链可达性口径）。

## 多实例共享限流（effort #14）

- **限流后端 SPI（RateLimitBackend）** — core.spi 策略/存储分离接口：策略（排队/拒绝/事件）
  留在 ModelRateLimiter，额度存取（tryAcquire/consume/available/capacity/waitHint）抽象到后端；
  kind 标识（memory/redis）供观测。
- **内存令牌桶后端（InMemoryRateLimitBackend）** — 原 TokenBucket 逻辑平移，默认后端；
  单进程部署行为零变化（全量既有测试不改一行全绿即证明）。
- **Redis 固定窗后端（RedisRateLimitBackend）** — 分钟窗 INCR/DECR + 首写 EXPIRE 61s
  （LiteLLM Router 同款）；多实例共享 RPM/TPM 额度（总闸正确）；超限回滚不泄漏；
  epoch 时基窗口键（跨时区无关）；模型名净化防键注入。
- **fail-fast 故障语义** — Redis 不可达按 STORE_WRITE_FAILED 带修法上抛，不静默 fail-open
  （限流失效比暂不可用更危险）；starter 按 store.type=redis 自动装配共享后端
  （ObjectProvider 优先消费，无 bean = 内存默认零变化）。
- **整形差异诚实入档** — 固定窗边界两窗相接可 2× 尖峰；额度总量与拒绝语义同令牌桶
  两档等价；TPM 记账可致负余额（诚实表达超限，下窗重置）。

## 语义缓存（effort #15）

- **语义缓存（Semantic Cache）** — 精确缓存（spec 53）之上加 embedding 相似度命中层：
  问题文本嵌入 → 同桶（model+options 采样）最近邻 cosine ≥ 阈值即命中（LiteLLM
  semantic caching 同思想）；order +460 = 精确缓存（+450）之后——零成本层先行。
- **进程内向量存储** — 桶内线性扫描 + LRU/TTL 惰性过期（ResponseCacheStore 同风格）；
  Redis/RediSearch 向量后端 out-of-scope（非标准 Redis 模块）。
- **嵌入可插拔 + fail-fast** — Spring AI EmbeddingModel bean 注入（Buzhou 不内置
  provider）；enabled 而无 bean 启动即失败带修法（不静默不生效）。
- **嵌入故障旁路降级** — 嵌入查询/写入失败 → 该调用旁路直通主路径（bypass 计数可感，
  不阻断）——嵌入故障不该弄坏主路径（与限流 fail-fast 语义刻意不同：降级不损正确性）。
- **机制与判别力分离（诚实边界）** — 框架保证阈值/分桶/终态边界正确；「X」vs「不是 X」
  的语义判别力归嵌入模型（红队否定对钉住：相近嵌入下框架按阈值诚实命中）——默认关闭
  + 阈值可调 + 适用面自律承担残余误命中风险。
