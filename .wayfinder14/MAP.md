# Wayfinder Map — Buzhou 多实例共享限流（effort #14）

> effort #14，延续 #5–#13（累计 142 轮 / T1–T221 / impl 1–184）。
> 本 effort 主线：**多实例共享限流（Redis 分布式闸）**——RateLimitBackend SPI 分离
> 策略与存储；内存后端（令牌桶，默认零变化）+ Redis 后端（分钟固定窗，跨实例共享额度）；
> starter 按部署形态装配；testcontainers 真实 Redis 验证。
> 到达 = 9 轮自迭代落地、全仓 verify 绿、对抗/perf/文档齐备、MAP 闭合。

## Destination

多实例部署下 RPM/TPM 额度跨实例共享互斥（总闸正确）；单进程部署行为零变化；
Redis 故障 fail-fast 带修法（不静默 fail-open）；多实例边界文档（§6）从「诚实声明」
升级为「可选共享闸」。

## Notes

- 领域/测试哲学/10K★ 政策/AFK 授权：沿用 effort #6–#13 MAP Notes。
- 外部研究（2026-08-16 已备）：LiteLLM（~26K★）Router per-deployment rpm/tpm 限流为
  Redis 固定窗计数（masterKey 聚合）；DualCache L1 内存 + L2 Redis 分层（本地 V1 不做
  分层——Redis 往返即共享语义本体，分层缓存一致性问题留给证据）。**本地裁定**：Redis
  后端用分钟固定窗（INCR/EXPIRE，LiteLLM 同款）而非分布式令牌桶（Lua 状态复杂度不值）；
  整形特性差异诚实入档（固定窗边界尖峰 vs 令牌桶平滑——额度总量两档等价）。
- 本地勘察（2026-08-16）：ModelRateLimiter 单进程令牌桶（TokenBucket synchronized，
  refill 语义）；store-redis 已有 testcontainers 基建（RedisStoresTestcontainersTest）
  与 spring-data-redis 基础设施；impl-74/T99 的多实例 WARN（限流/熔断/配额单进程）——
  本 effort 消除其中「限流」项的告警义务（熔断/配额仍单进程，边界保留）。
- 过程教训沿用：多构造器 record @ConstructorBinding；下游单跑 -am；键新增走矩阵登记
  （T214 防线）。

## Decisions so far

- **SPI 落位 core.spi**（非 resilience 内）：store-redis 实现若依赖 resilience 会反向依赖，
  core 是唯一公共上游；维度常量（RPM/TPM）留在 resilience 侧（策略语义）。
- **内存后端 = 逐行平移**（非重写）：synchronized 桶 + nanoTime refill 语义不变，
  零变化证明 = 全量既有测试不改一行全绿（resilience 117 项）。
- **Redis 后端选固定窗**（INCR/DECR + 首写 EXPIRE 61s）而非分布式令牌桶：LiteLLM 同款、
  状态简单；窗口边界 2× 尖峰与额度总量两档等价诚实入档（spec 54 §B）。
- **超限即回滚 DECRBY**：拒绝不泄漏额度；TPM consume 不回滚（负余额诚实表达超限）。
- **窗口键 epoch 时基**（epochMillis/60000）：跨时区实例同窗；模型名净化 `[A-Za-z0-9._-]`
  外字符 → `_`（键结构不可注入）。
- **fail-fast 故障语义**：Redis 不可达 = STORE_WRITE_FAILED 带修法上抛，不静默 fail-open；
  secondsUntilAvailable 纯本地计算（断连下等待提示仍可得出，策略层超时兜底）。
- **starter 装配条件化**：store.type=redis 且配置任一容量才供 bean（不白开 Lettuce 连接）；
  resilience 经 ObjectProvider 优先消费；多实例 WARN 消除限流项（熔断/日配额保留）。
- **本机环境教训（过程）**：沙箱会话内 ~/.m2 跨调用写入不可见 + 既往本地仓库陈旧——
  examples 的 BOM import 在本地仓库不完整时出现「新增 versionless 依赖 → BOM 管理连带
  失效」假象；修复 = 全量 install 补齐本地仓库后标准 versionless 写法一切正常（examples
  pom 最终零版本特例）。另：遗留僵尸 surefire 进程（上周日）会挂起整链构建，需清理。

## Not yet specified

- 分布式熔断/分布式会话配额（仍单进程；多实例边界保留）。
- L1 内存 + L2 Redis 分层限流（LiteLLM DualCache 语义；一致性成本待证据）。
- 语义缓存 / outbox SCAN 下推 / 观测 OLAP / store 静态加密 / skill 语义排序（沿用 fog）。

## Out of scope

- 沿用 effort #7–#13 Out of scope 全部条目。
- 分布式令牌桶（Lua 脚本状态机；固定窗证据不足再议）。
- 限流数据跨区复制/多 Redis 集群（单 Redis 主从假设）。

## Tickets

初始 9 张（T222–T230，均含 AFK 决议，按轮逐张闭合；2026-08-17 全闭合）：

- [x] [T222 RateLimitBackend SPI + 内存实现迁移](tickets/T222-ratelimit-backend-spi.md)（impl-185）
- [x] [T223 Redis 后端](tickets/T223-redis-backend.md)（impl-185/186）
- [x] [T224 starter 装配切换](tickets/T224-starter-wiring.md)（impl-186；条件化 bean + WARN 消除）
- [x] [T225 共享额度 containers 验证](tickets/T225-shared-quota-containers.md)（impl-186；本机无 Docker 跳过，CI 验证）
- [x] [T226 Redis 面红队](tickets/T226-redis-redteam.md)（impl-187；并发竞差/时区无关/断连全路径）
- [x] [T227 Redis 面 perf 哨兵](tickets/T227-redis-perf.md)（impl-188；首轮实测值待 CI nightly 补录）
- [x] [T228 双实例演示](tickets/T228-redis-demo.md)（impl-189；examples + 内存对照组）
- [x] [T229 文档 + 元数据 + 矩阵登记](tickets/T229-redis-docs.md)（impl-190；零新键）
- [x] [T230 里程碑 verify + 收口](tickets/T230-effort14-closing.md)（全仓 verify + MAP 闭合）
