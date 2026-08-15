# 142 — turn 反馈捕获 API

**Parent:** spec 47 §B / [T173](../tickets/T173-turn-feedback.md)

**What to build:** `AgentSession.rateTurn(turnSeq, type, value, comment, source)`——校验（type 三型/
值域/source 两值/轮次已存在）→ state store 持久化（`buzhou.feedback.<turnSeq>.<epochMillis>` 键 +
URLEncoded k=v 五字段）→ `turn.feedback` 会话事件外发。

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [x] AgentSession default 方法 + DefaultAgentSession 实现（HookEnvironment.stateStore() 访问器）
- [x] 校验：type ∈ boolean|numeric|categorical（值域各自校验）；source ∈ user|implicit（null→user）；turnSeq ∈ [1, currentTurn]；关闭会话拒绝
- [x] 持久化：StateEntry(producer=turn-feedback, createdTurn=turnSeq, 无 TTL)；同轮可多次（epochMillis 键区分）
- [x] 事件：turn.feedback payload 含 turnSeq/type/value/comment(非空才带)/source
- [x] 测试：三型合法落库（scanByPrefix 键 + 解码字段）+ 事件断言；非法 type/值/source/未来轮次/关闭后各拒
- [x] buzhou-core `mvn verify -am` 全绿
