package io.github.chyuan_cuihongyuan.buzhou.tools.todo;

/**
 * 任务清单条目（spec 06 存储 Schema）：status ∈ pending / in_progress / completed。
 */
public record TodoItem(String id, String content, String status, int createdTurn) {

    public static final String PENDING = "pending";
    public static final String IN_PROGRESS = "in_progress";
    public static final String COMPLETED = "completed";

    public TodoItem {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("TodoItem.id must be non-blank");
        }
        if (!PENDING.equals(status) && !IN_PROGRESS.equals(status) && !COMPLETED.equals(status)) {
            throw new IllegalArgumentException("TodoItem.status 非法：" + status);
        }
    }
}
