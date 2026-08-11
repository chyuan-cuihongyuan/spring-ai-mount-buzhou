# 优雅停机与会话 drain

Type: grilling
Status: resolved

## Question

> **05 已决(2026-08-11)**:持久化强度三档(sync/async/exit)中 `exit` 档与本票强联动——停机时必须 flush 在途写,否则 exit 档丢失整轮。

滚动发布/重启时**在途会话**怎么办?(参考文档一.1 实例销毁回收;Spring Boot graceful shutdown 现状见 02 号票成果)

需回答:
1. **做不做**——会话级 drain 协议(拒新会话、等在途轮次完结、超时强杀)是否框架职责
2. **机制边界**——drain 到什么粒度(等当前轮次完结 vs 等整个会话结束);会话跨实例迁移(handoff)做不做
3. **接缝**——与 Spring Boot graceful shutdown 的组合方式;与租约释放/抢占的关系;停机瞬间 Spill 落盘/持久化写的一致性要求;drain 状态如何进 observability

答题要求:同 03 号票(做/不做 + 机制边界 + 接缝;借鉴成熟实现并注明来源)。

## Answer

(2026-08-11,grilling 收口,三问全采纳推荐)

**决策:做**——会话级 drain 协议;不做显式跨实例迁移。

**机制边界(管什么/不管什么)**:
- **drain 协议**:停机信号后 ①拒新会话(spawn 明确拒绝,调用方可路由到别的实例)②等在途会话的**当前轮次**完结(粒度=轮次,与微压缩"完结轮次"原子单位对齐;会话可能无限长,不等整个会话)③超时强杀(走执行脊柱已有的取消传播)
- **不管**:显式会话迁移协议(实例间 RPC 移交)——"可接管性"由五 SPI + 租约 + 05 恢复语义天然提供:drain 后释放租约 + flush 状态,调用方在实例 B 用同 sessionId 重新 spawn 即续接;"滚动发布会话续接模式"写入运维文档

**接缝**:
- **SmartLifecycle 挂 drain 钩子**,动作清单:拒新会话 → 等在途轮次完结/超时强杀 → exit 档持久化 flush(05 已定联动)→ 观测异步管线排空;超时预算与 `spring.lifecycle.timeout-per-shutdown-phase` 对齐;flush **同步执行**(虚拟线程 daemon 语义下 JVM 退出不等后台任务)
- 相位关系:Boot graceful 管 web 请求生命周期,不周山 drain 管会话生命周期,经 SmartLifecycle 相位衔接(与 web 容器关闭相位的先后留 Spec)
- spill 文件同步落盘(落盘才返回句柄),无需特殊处理

**借鉴**:
- Anthropic Managed Agents 的 harness 无状态化(wake(sessionId) 从持久化日志重建会话,显式迁移是伪需求的架构佐证)— https://www.anthropic.com/engineering/managed-agents
- Spring Boot 4 graceful shutdown + SmartLifecycle 相位模型(底座接缝)— https://docs.spring.io/spring-boot/4.0-SNAPSHOT/reference/web/graceful-shutdown.html
- 底座盲区依据:02 票成果 §7(graceful 只管 web 请求;虚拟线程 daemon 语义)
