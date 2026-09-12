package io.github.chyuan_cuihongyuan.buzhou.core.session;

/**
 * fork 谱系 state 键公共常量（spec 640 / T930）：写入口（AgentRuntime fork/forkFromTurn）
 * 与读入口（面板/模块/导出消费方）共用——字符串复制会漂移（漂移=谱系断），
 * 统一收口于此。键语义见 spec 602/634。
 */
public final class SessionForkKeys {

    /** 谱系源：value = 源会话 id（fork 与 forkFromTurn 都写）。 */
    public static final String SOURCE = "buzhou.fork.source";

    /** 回放起点：value = 轮次字符串（仅 forkFromTurn 写）。 */
    public static final String TURN = "buzhou.fork.turn";

    /** state producer 标识（buzhou.core.fork）。 */
    public static final String PRODUCER = "buzhou.core.fork";

    private SessionForkKeys() {
    }
}
