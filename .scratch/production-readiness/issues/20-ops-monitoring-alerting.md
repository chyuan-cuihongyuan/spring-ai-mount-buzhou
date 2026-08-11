# 运营监控、告警与性能基线

Type: grilling
Status: resolved

## Question

> **16 已决(2026-08-11)**:审计事件同步落库、失败不阻断主链路但 ERROR+指标+告警钩子——审计落库失败是本票的告警源之一。

框架在**运营监控面**做到哪?(参考文档 3.3 指标埋点、八.2/八.3 多维告警与告警通道、二.3 内存资源监控;以及性能基线与容量画像——框架级次要事项)

需回答:
1. **做不做**——告警规则/告警通道(钉钉/企微/邮件)是框架职责,还是只保证指标暴露完备、告警交给 Prometheus/Alertmanager/Grafana 生态(**不重新发明轮子**的典型案例);慢调用追踪、资源监控(上下文对象大小/缓存占用)归谁
2. **机制边界**——框架必须暴露的指标清单(成功率/耗时/工具频次/失败分布/token/会话时长/循环触发次数)够不够,缺什么;性能基线做到什么程度(CI 回归门槛 vs 一次性报告)
3. **接缝**——现有 Micrometer 双写/otel exporter 的指标维度盘点与补缺;资源监控与 08 失控检测的联动;压测工具选型与放置模块(examples?独立 benchmark 模块?)

答题要求:同 08 号票(做/不做 + 机制边界 + 接缝;借鉴成熟实现——Micrometer/Prometheus 生态、LangSmith 监控——并注明来源)。

## Answer

(2026-08-11,grilling 收口,三问全采纳推荐)

**决策:部分做**——**指标暴露归框架,告警归生态**(不重新发明轮子的典型);性能基线做 CI 回归门槛。

**机制边界(管什么/不管什么)**:
- **指标暴露(做)**:Micrometer 双写补缺——已有(模型/工具调用耗时、token、观测队列等待、持久化错误)+ 补缺(活跃会话数/会话时长/轮次分布、08 失控触发计数、11 预算动作计数、07 限流拒绝计数、03 熔断状态、14 HITL 拦截/授权计数、16 审计落库失败);**指标清单规范化进 Spec**(命名/维度/标签基数控制);otel 导出对齐 **OTel GenAI 语义约定**(`gen_ai.*` 属性命名直接可借)
- **资源监控(做)**:框架内部状态指标(记忆模块占用、spill 目录大小、观测队列深度)——容量画像与告警的输入
- **告警(归生态)**:告警规则只出**样例**(PrometheusRule 进 docs/examples,非运行时机制);告警通道(钉钉/企微/邮件)不做——Alertmanager 生态全有;dashboard **不内置告警**(嵌入库的半吊子告警与运维体系双头马车;LangSmith 三件套是平台形态,不照搬)
- **性能基线(做)= CI 回归门槛**:复用 ticket 21 模式(JUnit + 阈值断言随 `mvn verify`),覆盖框架热路径(压缩/注入视图构建/脊柱 fan-out 开销);放 examples/evaluation 旁;**不引 JMH**(CI 计时断言粒度已够抓数量级回归);全链路负载测试不做(业务场景相关,归用户);精确微基准需求留 Spec
- **不管**:Prometheus/Grafana/Alertmanager 部署运维、业务侧 SLI/SLO 定义、告警值班流程

**接缝**:
- 全部机制(03/07/08/11/14/16)的治理动作计数进同一 MeterRegistry——治理机制在生产不再是黑盒
- 资源监控指标喂容量画像;08 失控检测的触发计数同源暴露
- 19:评估分与评估器故障作指标源;06:停机排空进度可指标化(细则留 Spec)
- 慢调用 = Span 耗时指标 + 阈值告警样例,无需专门追踪机制(事件流已有完整轨迹)

**借鉴**:
- LangSmith 自托管 Prometheus 指标(暴露标准指标端点、接入既有监控栈——嵌入方正确姿态)— https://docs.langchain.com/langsmith/self-hosted-changelog
- LangSmith 看板+阈值告警+webhook 三件套(运营监控最小共识——平台形态参照,嵌入库不照搬告警)— https://docs.langchain.com/langsmith
- OpenTelemetry GenAI Semantic Conventions(`gen_ai.*` 属性、`invoke_agent`/`execute_tool` span 名——厂商中立命名直接可借)— https://opentelemetry.io/docs/specs/semconv/gen-ai/
- AutoGen 原生 OTel 埋点(框架内建插桩渐成标配)— https://github.com/microsoft/autogen
- Micrometer/Prometheus/Alertmanager 生态(告警通道不重新发明)
