# 会话数据生命周期(TTL/归档/删除/冷热分离)

Type: grilling
Status: resolved

## Question

> **16 已决(2026-08-11)**:审计证据链的留存期/合规格式归本票(L3);治理事件(审计族)是否随会话数据删除豁免,细则在本票定。

**会话数据生命周期**是框架职责还是用户运维?(参考文档一.2 会话 TTL/自动过期/冻结解冻、十一.3 冷热分离过期归档、七 合规删除)

需回答:
1. **做不做**——TTL/自动过期/归档/删除是 store SPI 的职责扩展,还是用户自己运维;临时/持久/长/短会话的分类要不要进核心模型
2. **机制边界**——生命周期策略的表达(配置驱动?按会话类型?);**删除的彻底性**:消息/摘要/state/spill 落盘文件/观测事件的全链路删除(合规"被遗忘权"场景)
3. **接缝**——五 SPI 的方法扩展(delete/archive 语义);Spill 落盘文件的清理责任;与 13 脱敏加密(存储形态)的协同;与 18 回放(归档后还能不能回放)的制约

答题要求:同 08 号票(做/不做 + 机制边界 + 接缝;借鉴成熟实现并注明来源)。

## Answer

(2026-08-11,grilling 收口,三问全采纳推荐)

**决策:部分做**——TTL + 删除做(store SPI 职责扩展);归档/冷热分离不建框架机制;会话分类不进核心模型。

**机制边界(管什么/不管什么)**:
- **生命周期策略 = 绑定级 TTL 档位**(ephemeral / standard / long)+ **条目级覆盖**(LangGraph `put(ttl=...)` 蓝本);**记忆类数据(摘要/state)默认 refresh_on_read 读时续期**(LangGraph Store TTL 蓝本——防长期活跃会话记忆被误删);**清扫器内建于存储实现**(LangGraph `start_ttl_sweeper` 蓝本,周期清扫)
- **会话分类不进核心模型**:临时/持久/长短会话不立类型枚举,生命周期档位作绑定级标签
- **删除 = 全链路硬删**:消息/摘要/state/租约/**spill 落盘文件**/观测事件/注入快照成套清理;spill 文件清理责任归框架(框架落的盘框架删);**软删不做**(私有化"被遗忘权"要硬删,回收站反成合规风险)
- **豁免清单**(LangSmith 数据集豁免蓝本):**16 治理事件(审计族)不随会话删除**;评估集样本豁免
- **归档不做框架机制**:观测导出/otel 通道已有,冷存储选型与冷查询归用户运维
- **不管**:冷存储、备份策略、合规认证举证模板

**接缝**:
- 五 SPI 补**全链路 deleteBySession + TTL 清扫**语义;契约测试补生命周期用例(jdbc/redis 语义一致,同 store 契约测试模式)
- 13:高合规档(全链路脱敏)删除面更小(13 已记协同);存储形态档位与生命周期档位**正交**
- 18:**回放窗口 = 数据留存窗口**(删除/过期后不可 fork、不可回放,明示)
- 16:治理事件留存期/合规格式(L3)落本票策略体系(档位+豁免)
- 15:冻结(长期暂停)会话的 TTL 处理留 Spec;21:按租户删除依赖租户透明维度

**借鉴**:
- LangGraph Thread TTL / Store TTL(`default_ttl`+超期策略+周期清扫;`refresh_on_read` 读时续期;`put(ttl=...)` 条目级覆盖;Postgres `start_ttl_sweeper`)— https://github.com/langchain-ai/langgraph
- LangSmith 留存双档 + 按 workspace 自定义 + **数据集豁免**(留存分档+例外清单的完整范本);数据清除 API + 删除语义分级 — https://docs.langchain.com/langsmith/usage-and-billing 、 https://docs.langchain.com/langsmith/data-purging-compliance
- CrewAI `reset-memories`(记忆运维清理 CLI 形态)— https://github.com/crewAIInc/crewAI
