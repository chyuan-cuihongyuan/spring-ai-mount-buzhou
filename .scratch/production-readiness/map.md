# Wayfinder Map: 生产就绪缺口盘点(production-readiness)

## Destination

产出《生产就绪缺口盘点 + 取舍路线图》决策文档:盘点"让单个业务 Agent 稳定、可控、可解释地跑在生产里"还缺哪些运行时机制,逐缺口决策 **做/不做/何时做 + 机制边界 + 与既有机制的接缝**,按 **稳定 > 可控 > 可解释** 加权排序;文档附 **《借鉴与引用清单》**——每个"做"项注明借鉴了哪个成熟开源项目的哪个治理实现(或哪篇博客/研究支撑),附一手链接。

## Notes

- 领域:Spring AI 不周山(runtime harness),九大机制已落地(实现票 01–22 收口);术语以根目录 CONTEXT.md 为准
- 参照系:通用企业生产(toB 私有化 + 企业内部助手),无具体落地场景在等
- 决策颗粒度:每票答到 做/不做 + 机制边界(管什么/不管什么) + 与既有机制接缝(Hook 环节/SPI/事件流)
- **决策方法论**:能借鉴成熟开源项目(LangChain/LangGraph 等业内认同度高的)的治理实现就借鉴,不重新发明;无现成借鉴时须有博客/研究支撑;纯自主推演标注 `> 【推演】`;每票答案记录借鉴与引用链接
- 功能方向参考(**仅参考,非需求清单**):[reference/buzhou-feature-list.md](reference/buzhou-feature-list.md)(源自用户桌面 buzhou.md,2026-08-11 纳入)
- grilling 票走 /grilling + /domain-modeling;research 票由研究子代理 AFK 解决
- 单 Agent 边界;忠实度原则(蓝本明确描述的机制严格遵循,留白推演标注)

## Decisions so far

- [路线图整合 + 借鉴与引用清单](issues/25-roadmap-synthesis.md) — **Destination 达成**:产物落盘 [docs/production-readiness/](../../../docs/production-readiness/)(README=缺口盘点+路线图,references=借鉴与引用清单);五家族收敛;三期里程碑(M1 稳定→M2 可控→M3 可解释);决策统计:做 11/部分做 9/基本不做 1/不做 1
- [RAG 检索治理(边界拷问)](issues/24-rag-governance.md) — 不做专门机制:检索即工具,划界检索层(业界共识);参考文档六诉求全部被 09/14/19/23 吸收(文档 ACL=框架贯穿主体、检索层判可见);bigtool 模式作 ToolSetProvider 候选留 Spec
- [Prompt 与配置版本治理、灰度、热更新](issues/23-prompt-config-versioning.md) — 部分做:prompt+Skill 正文统一内容制品版本化(不可变制品+钉版,LangSmith commit 钉版蓝本,复用 Skill DB 通道);灰度=版本粘滞(spawn 钉版会话内不漂,flow_version 蓝本),canary 分流不做;版本进注入快照+变更记治理事件
- [会话数据生命周期(TTL/归档/删除/冷热分离)](issues/22-data-lifecycle.md) — 部分做:TTL+删除做(SPI 扩展:绑定级档位+条目覆盖+读时续期+清扫器内建,LangGraph 蓝本),归档不做(导出通道归用户);全链路硬删含 spill 文件,治理事件/评估集豁免;会话分类不进核心模型;回放窗口=留存窗口
- [多租户边界拷问](issues/21-multi-tenancy.md) — 部分做:tenantId 透明维度贯穿(存储键命名空间/配置/配额/归因),核心模型不动;隔离定级=命名空间级逻辑隔离(业界共识,强隔离不做);租户配额=11 预算管线实例化+周期滚动(日/月)同炉;租户系统不做
- [运营监控、告警与性能基线](issues/20-ops-monitoring-alerting.md) — 部分做:指标暴露归框架(Micrometer 双写补缺+治理动作计数全覆盖,对齐 OTel GenAI 语义),告警归生态(PrometheusRule 样例,通道不做);性能基线=CI 回归门槛(ticket 21 同模式,不引 JMH)
- [线上评估闭环、流量镜像与沙箱仿真](issues/19-eval-mirror-sandbox.md) — 三做一替代:在线评估做(LangSmith run rule:过滤×采样×评估器,观测管线后置异步)+离线回归做(扩展 ticket 21 提炼评测基建,工具轨迹一等指标)+沙箱做(绑定级 mock 工具集+脚本模型);流量镜像不做实时影子,务实替代=18 fork 离线双跑
- [会话回放、异常快照与调试模式](issues/18-session-replay-debug.md) — 部分做:L1 只读时间旅行(数据已齐:注入快照+消息+事件,dashboard 按轮还原)+ L2 轻量会话分叉(指定轮次复制历史到新 sessionId 续跑,LangGraph fork 蓝本);不承诺确定性复现,承诺同起点;原地 replay/断点调试/异常快照机制不做(被 05/15/观测吸收)
- [Planning 机制(计划自报/审批/可改)](issues/17-planning.md) — 部分做:计划自报=TodoTool 已落地(同构 LangChain TodoListMiddleware),软约束;计划审批=机制可拼装(todo 挂 HITL 门禁即得),不建专门审批机制;不做计划一等对象/Planner SPI/plan-and-execute;补两个模式文档
- [审计证据链(授权/干预/护栏留痕)](issues/16-audit-trail.md) — 做:审计=observability 事件流特化视图(不建独立存储),治理事件族统一 actor/action/target/outcome 四元组;L1 追加写不可变(SPI 契约),L2 防篡改对接外部 WORM 不重新发明,L3 留存归 22;审计事件同步落库不采样、失败告警不阻断
- [运行时干预(暂停/接管/纠偏/kill switch)](issues/15-runtime-intervention.md) — 做:干预平面四能力(暂停/纠偏注入/kill switch/人工接管),接管≠租约转让(控制通道注入);统一挂起-回填原语(状态可序列化外迁,HITL 双模);失控/成本/护栏均可配置转人工
- [工具权限模型(RBAC/黑白名单/工具开关)](issues/14-tool-permission.md) — 做:权限判定平面(主体×工具组矩阵,SPI 可接 OPA;业界空白需推演);L1 结构权限/L2 HITL 两层正交;供给过滤+调用拦截双执行点;主体调用方声明、框架不认证
- [PII 脱敏与数据加密](issues/13-pii-masking-encryption.md) — 做:入模+日志事件双脱敏(识别器 SPI,护栏平面同炉),双模(redact 默认/假名化可逆映射),存储加密装饰器(密钥归用户);存储形态显式双档(默认存原文保证据链,高合规档全链路脱敏)
- [内容安全护栏(注入/越狱/输出合规/水印)](issues/12-content-safety.md) — 部分做:护栏执行平面(挂载点+动作语义+检测器 SPI),三动作 block/redact/flag-only,带原因重试,故障降级透传;注入分类器不自研(声明外部依赖),水印不做;内容拒绝治理落 afterModel
- [成本治理(token 硬顶/预算/配额/计费归因)](issues/11-cost-governance.md) — 做全链:双通道计量(LangSmith 蓝本)+定价表+预算硬顶(业界空白,推演);会话/绑定级两档,三级动作(告警→降级链→阻断)+预算余量 Attachment 注入;租户配额归 21
- [Agent 实例生命周期与异步任务调度(边界拷问)](issues/10-agent-lifecycle-async.md) — 基本不做:实例池(诉求已被 MCP 热插拔/会话边界/租约吸收)与调度器(交 Spring 生态)都不做;只做句柄异步语义补强+异步任务模式文档;长任务挂起-回填原语归 15 同炉
- [工具调用容错与结果缓存](issues/09-tool-fault-tolerance-cache.md) — 做:M1 按工具策略异常处理(重试/清洗/兜底)+ M2 工具熔断(复用 03 状态机概念);声明式结果缓存(幂等声明=可缓存候选,借 LangGraph CachePolicy);脊柱管执行、Hook 管策略;高频问题缓存不做
- [死循环与失控检测(runaway protection)](issues/08-loop-detection.md) — 做:M1 数值硬顶(双窗口+按工具+时长)+ M2 确定性重复检测(业界空白,推演);软退出通道(Attachment 注入剩余步数)+终止携带部分结果;检测在执行脊柱,事件进 observability
- [背压与多层限流](issues/07-backpressure-rate-limit.md) — 做:三维(并发会话上限/工具扇出上限/每模型 RPM+TPM 双桶),过载两档(有界排队/快速失败),单进程内存+每实例配额;网关 QPS 与分布式限流不做,租户配额归 11/21
- [优雅停机与会话 drain](issues/06-graceful-shutdown.md) — 做:drain 粒度=当前轮次完结(拒新会话/等轮次/超时强杀),SmartLifecycle 钩子 + 四步动作清单(含 exit 档 flush、观测管线排空);不做显式迁移协议——可接管性由五 SPI+租约+05 恢复语义天然提供
- [崩溃中轮次恢复 + 幂等控制](issues/05-inflight-recovery-idempotency.md) — 做:恢复语义分档(默认轮次作废,opt-in 自动重驱动)+ 持久化强度三档(sync/async/exit,对标 LangGraph)+ 幂等三件套(声明/幂等键/去重,业界空白需推演);不新增 SPI,消息即检查点
- [多模型路由:负载均衡/主备/降级链/静态兜底](issues/04-model-routing-fallback.md) — 部分做:failover/降级链/静态兜底归框架(与 03 M2 同炉、调用级路由、模型档位制);负载均衡划给网关层,SPI 可对接;模型链进绑定级配置
- [模型层韧性(重试/熔断/超时/429 应对)](issues/03-model-resilience.md) — 做:自研韧性层,ResilienceAdvisor 执行 + Hook 补 onModelError 切面;M1=错误归一化+重试+统一超时,M2=熔断+兜底(与 04 同炉);底座只管网络瞬断;内容拒绝治理归 12
- [业界成熟方案对标(LangChain/LangGraph 为主)](issues/01-research-industry-benchmark.md) — 16 维度对标:12 项业界共识可直接借(durability 三档、HITL 四决策、TTL+refresh_on_read、在线评估规则模型、prompt 钉版等),12 项空白需自主推演(熔断、语义死循环、工具 RBAC、预算硬顶等);各 grilling 票的决策起点(成果在 `research/industry-benchmark` 分支)
- [Spring AI 2.0 / Spring Boot 4 内置能力盘点](issues/02-research-spring-ai-baseline.md) — 底座无熔断/fallback、无统一超时、429 默认不重试、无会话级观测;ToolCallingManager 替换点与不周山执行脊柱对齐;8 条留白清单 = 不周山合法空间(成果在 `research/spring-ai-baseline` 分支)

## Not yet specified

- 各"做"项的选型落点与细案:属 Spec 阶段,不在本地图(25 已收敛为五家族与三期里程碑,见 docs/production-readiness/)

## Out of scope

- **多 Agent 编排**(参考文档第九节:Agent 路由/子 Agent 嵌套/编排/多端同步)——单 Agent 边界,若要做另开 effort
- **多模态、结构化输出可靠性**——Spring AI 层能力,非 Harness 层
- **模型选型、提示词工程实践**等业务侧事项
