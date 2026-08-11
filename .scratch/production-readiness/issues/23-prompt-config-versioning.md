# Prompt 与配置版本治理、灰度、热更新

Type: grilling
Status: resolved

## Question

框架是否提供**Prompt 与配置的版本治理/灰度/热更新**?(参考文档四:Prompt 动态管理中心——DB 存储/版本管理/灰度/热更新/校验/多环境隔离;十.2 新 Prompt/新工具/新模型小流量灰度)

需回答:
1. **做不做**——Prompt 模板 DB 化+版本管理+灰度发布+热更新是框架职责还是用户业务;Skill 正文/工具集/策略配置的版本治理要不要统一
2. **机制边界**——版本治理对象清单(prompt/Skill/工具集/四层配置);灰度维度(按会话/按租户/按比例);多环境隔离(测试/预发/生产)是框架概念还是部署实践
3. **接缝**——与 Skill 系统(DB 动态覆盖已有)的关系——prompt 治理是否复用 Skill 的存储与覆盖语义;与 MCP 热插拔(差量刷新已有)的关系;与 policy 四层覆盖的关系;版本信息进 observability(回放时知道当时用的是哪个版本——与 18 联动)

答题要求:同 08 号票(做/不做 + 机制边界 + 接缝;借鉴成熟实现——LangSmith prompt hub 等——并注明来源)。

## Answer

(2026-08-11,grilling 收口,三问全采纳推荐)

**决策:部分做**——prompt 模板 + Skill 正文统一**内容制品版本化**(不可变制品 + 钉版引用);灰度做**版本粘滞**;canary 分流不做。

**机制边界(管什么/不管什么)**:
- **内容制品版本化(做)**:prompt 模板与 Skill 正文同一治理形态——**不可变制品 + 钉版引用**(LangSmith commit 钉版蓝本:每次保存产新版,生产钉住不可变版本);DB 存储与覆盖**复用 Skill 已有通道**(内置 classpath + DB 动态、同名覆盖),prompt 模板作为新制品类型接入(细则留 Spec)
- **热更新(已有)**:Skill DB 覆盖 + MCP 差量刷新,无需新建
- **灰度 = 版本粘滞(做)**:spawn 时解析并钉住制品版本,**会话内不漂**(LangGraph flow_version 蓝本:老会话走旧版、新会话走新版,在跑会话不受发版影响);版本解析点在 spawn/绑定加载时
- **按百分比 canary 分流(不做)**:业界无一框架内建;分流决策在请求入口,嵌入库够不着——归网关/部署层
- **治理对象 = 两制品**:工具集归 MCP 热插拔(版本化 = 服务器侧职责);四层配置 = 配置即代码归部署实践(policyVersion 已进注入快照,记录已具);**多环境隔离 = 部署实践**(DB 命名空间/前缀),不做框架概念
- **不管**:prompt 管理平台 UI、评审工作流、canary 分流设施

**接缝**:
- **版本进注入快照与事件流**:policyVersion 先例扩展 promptVersion / skillVersions——18 回放可还原版本现场("同起点"承诺含版本)
- 版本变更操作记 **16 治理事件**(谁在何时把绑定切到哪个版本)
- 按租户灰度 = 21 绑定级按租户实例化自然支持
- 19:版本对照评测(同数据集多版本并行,LangSmith Experiments 离线形态)经 fork 双跑自然支持

**借鉴**:
- LangSmith Prompt Hub(`pull_prompt("name:commit")` 钉住不可变版本,官方推荐生产钉版——prompt 即内容寻址制品,版本治理的最小正确模型)— https://docs.langchain.com/langsmith/manage-prompts-programmatically
- LangGraph Assistants(图的版本化配置:配置与代码分离,同一部署多份配置版本)— https://github.com/langchain-ai/langgraph
- LangGraph 官方 Backward Compatibility(`flow_version` 模式:新线程盖版本戳、版本路由,在跑线程不受发版影响——灰度的状态机解法)— https://docs.langchain.com/oss/python/langgraph/backward-compatibility
- 业界空白确认:线上按流量百分比 canary 无一框架内建(01 票维度 14)
