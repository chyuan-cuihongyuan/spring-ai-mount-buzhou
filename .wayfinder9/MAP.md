# Wayfinder Map — Buzhou 生产级纵深（effort #9）

> effort #9，延续 #1–#8（#5：22 轮；#6：9 轮；#7：20 轮；#8：20 轮，累计 71 轮 / T1–T150 / impl 1–121）。
> 本 effort 主线：**静态安全与运行时确定性**——spill 落盘加密、会话单飞闸、审计链轮换持久化与外锚、
> 时钟注入、迁移防护、读失败降级、命令资源限额、停机排空补全、观测纪律收口。
> 到达 = 20 轮自迭代落地、全仓 verify 绿、防线（黄金/红队/perf）与文档齐备、MAP 闭合。

## Notes

- 领域/测试哲学/10K★ 政策/AFK 授权：沿用 effort #6 MAP Notes（Spring AI 2.0.0 单 Agent harness；
  examples 端到端主接缝；语义借鉴零新依赖——加密只用 JDK javax.crypto，单飞借鉴 OpenHands 每会话
  事件串行化语义、Dify 凭据 AES-GCM 加密语义，均不引依赖）。
- 图前勘察（2026-08-16，双路只读扫描）：spill 落盘纯明文（DiskSpillStore.writeAtomically 直写）；
  同 sessionId 并发 chat 属未定义使用（仅文档声明）；SigningKeyRing.rotate() 仅内存切换——运行期
  轮换的新钥重启后不在环内（期间签名记录变「签名不可验」断链）；限流/熔断/配额/退避全系统时钟
  （测试只能真等）；SchemaMigrator 无未来版本拒绝与 checksum；BuzhouChatMemory.load 三处无 catch；
  RunCommandTool 内置路径无输出上限/进程树强杀；BuzhouRunawayProperties/BackpressureProperties 零
  fail-fast；SleepTimeScheduler.close()=shutdownNow() 丢 pending（javadoc 自认「属切片 38」未做）；
  webhook close 排空 5s 硬编码且无直接测试；ModelCircuitBreaker gauge 的 model tag 未做截断。
- 过程教训沿用：下游模块单跑一律 `-am`；断言以端上计数为准；破坏性变更 pre-1.0 允许但入档 api-surface。

## Decisions so far

- [T151 spill 落盘静态加密](tickets/T151-spill-encryption.md) — SpillCipher（JDK AES-256-GCM 信封、魔法前缀、随机 IV）+ encryption-key 配置即开、缺省关零变化；旧明文兼容读；密钥错配快速失败；spec 40 §A。
- [T152 会话单飞闸](tickets/T152-singleflight-gate.md) — 在途计数升级 CAS 0→1 单飞闸（三入口同闸）；并发第二轮次 TURN_IN_FLIGHT（NON_RETRYABLE 新码）确定拒绝；终结释放、默认开无开关；跨进程仍归租约门；spec 40 §B。
- [T153 审计轮换持久化与外锚](tickets/T153-audit-rotation-anchor.md) — rotate 写而后切（PemFileKeyPersister + scanDirectory 重启入环 + signing.key-dir）；VerificationReport headHash/anchorMatched 外锚比对（删尾/重写可检测）；spec 41 §A。
- [T154 时钟注入面](tickets/T154-clock-injection.md) — 熔断六处 + 配额 todayKey 注入 Clock（三参构造缺省 systemUTC 零变化）；RateLimiter/Advisor 退避/Outbox.due 显式不注入（诚实边界）；spec 41 §B。
- [T155 迁移器防护](tickets/T155-migrator-guards.md) — 未来版本拒绝（Flyway validateOnMigrate 等价）+ 版本表 checksum 列（存量幂等补列/NULL 回填锚定/篡改拒绝）；spec 42 §A。
- [T156 消息读失败降级](tickets/T156-read-degrade.md) — ReadDegradePolicy/Holder + loadHistory 统一路由（EMPTY=WARN+计数+空历史；OFF 默认上抛不变）+ read-degrade 属性 fail-fast；spec 42 §B。
- [T157 命令执行资源限额](tickets/T157-command-limits.md) — 勘察纠偏（强杀/兜底已有）后缩窄：输出内存兜底上限可配（七参构造 + max-output-bytes 属性）+ 截断语义钉住；rlimit/cgroup 出界；spec 43 §A。
- [T158 配置校验补全](tickets/T158-config-validation.md) — runaway/backpressure 全键 fail-fast + webhook 静默回退改显式拒绝（pre-1.0 破坏性变更入档）；null=不限语义保留；spec 43 §B。

## Not yet specified

- LLM 响应缓存（语义边界未清，长期 fog；沿用 #8）。
- skill 语义排序 / outbox Redis 服务端 SCAN 下推（沿用 #8 fog，量级不抵复杂度）。
- 观测 OLAP/多实例分布式（长期；沿用边界）。
- StateEntry/JDBC/Redis 静态加密（部署层盘加密 + TLS 属运维职责；本 effort 只做 spill 文件面，
  runbook 记运维指引）。

## Out of scope

- 沿用 effort #7/#8 Out of scope 全部条目（多实例分布式、FIDES 二期、sub-agent、LLM-as-judge 硬门）。
- E2B/Firecracker 真实档落地（沿用 #2 边界：接口预留桩 + 显式 fail-fast 即可）。
- dashboard 前端工程化（沿用 #4 边界）。

## Tickets

初始 19 张（T151–T169，均含 AFK 决议，按轮逐张闭合）：

- [T151 spill 落盘静态加密](tickets/T151-spill-encryption.md)
- [T152 会话单飞闸](tickets/T152-singleflight-gate.md)
- [T153 审计轮换持久化与外锚](tickets/T153-audit-rotation-anchor.md)
- [T154 时钟注入面](tickets/T154-clock-injection.md)
- [T155 迁移器防护](tickets/T155-migrator-guards.md)
- [T156 消息读失败降级](tickets/T156-read-degrade.md)
- [T157 命令执行资源限额](tickets/T157-command-limits.md)
- [T158 配置校验补全](tickets/T158-config-validation.md)
- [T159 停机排空补全](tickets/T159-shutdown-drain.md)
- [T160 观测纪律收口](tickets/T160-metrics-enforcer.md)
- [T161 黄金轨迹 E](tickets/T161-golden-e.md)（blocked-by T151/152/153）
- [T162 红队对抗扩展](tickets/T162-redteam-surface3.md)（blocked-by T151/153/157）
- [T163 perf 哨兵第三批](tickets/T163-perf-3.md)（blocked-by T151/152/156）
- [T164 examples 演示第三批](tickets/T164-demo-3.md)（blocked-by T151/152/156）
- [T165 runbook 第五轮](tickets/T165-runbook-5.md)（blocked-by T151/152/155/156/159）
- [T166 CONTEXT/api-surface 增补](tickets/T166-context-api-9.md)（blocked-by T151–T160）
- [T167 配置元数据第三批](tickets/T167-metadata-3.md)（blocked-by T151/T156/T158/T159）
- [T168 里程碑 verify](tickets/T168-milestone-verify.md)（blocked-by T151–T167）
- [T169 收口](tickets/T169-effort9-closing.md)（blocked-by T168）
