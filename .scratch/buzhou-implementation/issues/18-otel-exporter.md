# 18 — OTel 导出桥

**What to build:** buzhou-observe-otel 可选模块：四类 Span 映射为 OpenTelemetry span 导出（traceId 由 sessionId 派生），Event 映射为 span event/attribute；对接 Collector 验证；默认关，引入并开启即生效。

**Blocked by:** 11

**Status:** done（实现 b008135；OTel 导出桥：四类 Span + HARNESS_INTERNAL 映射为 OTel span，traceId 由 sessionId 经 SHA-256 派生、同会话同 trace，Event 映射为 span event；接入点为 buzhou-observability 新增 PipelineSink SPI，在 BaseSpanRecorder.enqueue 时刻同步旁路——保留 open→event→close 时序，规避批量 drain 把同批 Span/Event 拆分重排导致 event 落到已 end 的 span；缺省 enabled=false，未接 sink 即零开销。映射规则：SESSION/TURN→buzhou.session/turn、MODEL_CALL→chat <model> + gen_ai.*、TOOL_CALL→execute_tool <tool> + gen_ai.*、HARNESS_INTERNAL→buzhou.internal.<action>；status OK/ERROR/CANCELLED→OK/ERROR/UNSET+cancelled；起止时间原样映射。复审修复：wiring 测试改用内联桩 store（原误引 core.internal.memory 越界）、PipelineSink javadoc 去矛盾（澄清为 observe-* 二层边消费的受支持扩展点）、setObject/putObject 用守卫模式收敛重复 switch。spec 03 增补推演 #15（include-content 门控泛化到 content/arguments/result/stacktrace 四类内容型字段，默认关）。AutoConfiguration 装配归 ticket 20 starter。）

- [x] OTLP 导出到本地 Collector 可见完整 trace 树（OtlpExportPathTest：真实 OtlpHttpSpanExporter 经 BatchSpanProcessor flush 后向 in-process OTLP HTTP receiver 送达 200 + 非空 protobuf body；trace 树结构由 OtelBridgeMappingTest 以 InMemorySpanExporter 断言父子链/同 trace。真实 Collector Docker 端到端可后续以 @Testcontainers(disabledWithoutDocker=true) 补充，与 jdbc 模块同口径——本环境 Docker 不可用，不提交未验证用例。）
- [x] Span 种类/属性/耗时映射规则与 03 spec 一致（OtelBridgeMappingTest：四类 span 名字/gen_ai 语义约定属性、buzhou.turn_seq、起止时间原样保真、ERROR 附 exception.* + CANCELLED→UNSET+cancelled）
- [x] 模块缺省关闭，对主链路零开销（OtelBridgeWiringTest.disabledConfigAttachesNoSinkMeansZeroExport：enabled=false → 装配侧不接 sink，主链路零导出、落库照常；enabled 时经真实 SynchronousObservabilityPipeline 句柄生命周期验证 sink 旁路生效 + store 双写）
