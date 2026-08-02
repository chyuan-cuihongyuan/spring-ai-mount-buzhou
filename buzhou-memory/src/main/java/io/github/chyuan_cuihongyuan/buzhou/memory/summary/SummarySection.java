package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

public enum SummarySection {
    USER_INTENT(0, "用户核心诉求"),
    CURRENT_STATE(0, "当前工作现场"),
    NEXT_STEP(0, "下一步"),
    PENDING_TASKS(1, "待办任务"),
    ERRORS_FIXES(1, "错误与修复"),
    KEY_ARTIFACTS(1, "关键产物"),
    PROBLEM_SOLVING(2, "已解决问题与进展"),
    TECHNICAL_CONCEPTS(2, "关键技术概念与决策"),
    USER_MESSAGES_LOG(3, "用户消息清单");

    private final int priority;
    private final String displayName;

    SummarySection(int priority, String displayName) {
        this.priority = priority;
        this.displayName = displayName;
    }

    public int priority() {
        return priority;
    }

    public String displayName() {
        return displayName;
    }
}
