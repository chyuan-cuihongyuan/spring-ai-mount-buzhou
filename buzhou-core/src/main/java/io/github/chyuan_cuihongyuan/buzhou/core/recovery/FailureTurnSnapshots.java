package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionObserver;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 失败轮快照面（spec 523 / T797，Sentry event payload 思想——错误轮的
 * 复现最小集：轮号/错误类/错误消息/输入预览一屏可读）。SessionObserver
 * 缝（onTurnStart 记输入/onTurnError 落快照——423 采样器同法同缝）；
 * 有界环形（默认 128——Sentry 同量级），实例即会话粒度（经
 * SessionAssemblyContext.addObserver 注册）。
 *
 * <p>诚实边界：输入截断预览（默认 512 字符——完整输入可能含敏感面，
 * 导出走宿主自有管道）；错误消息截断 256；只观测不干预。
 */
public final class FailureTurnSnapshots implements SessionObserver {

    /** 默认环形容量。 */
    public static final int DEFAULT_CAPACITY = 128;
    /** 默认输入预览截断（字符）。 */
    public static final int DEFAULT_INPUT_PREVIEW_CHARS = 512;
    /** 错误消息截断（字符）。 */
    public static final int ERROR_MESSAGE_CHARS = 256;

    /** 单失败轮快照（复现最小集）。 */
    public record Snapshot(int turnSeq, String errorClass, String errorMessage,
            String inputPreview) {
    }

    private final int capacity;
    private final int inputPreviewChars;
    private final Map<Integer, String> inputsByTurn = new LinkedHashMap<>();
    private final Deque<Snapshot> snapshots = new ArrayDeque<>();
    private long totalErrors;

    public FailureTurnSnapshots() {
        this(DEFAULT_CAPACITY, DEFAULT_INPUT_PREVIEW_CHARS);
    }

    public FailureTurnSnapshots(int capacity, int inputPreviewChars) {
        if (capacity < 1 || inputPreviewChars < 1) {
            throw new IllegalArgumentException("capacity 与 input-preview 必须 >= 1");
        }
        this.capacity = capacity;
        this.inputPreviewChars = inputPreviewChars;
    }

    @Override
    public void onTurnStart(int turnSeq, String userInput) {
        inputsByTurn.put(turnSeq, userInput == null ? "" : userInput);
        // LIFO 防泄漏：容量按 Turn 数（非快照数）——轮完结即清理
        while (inputsByTurn.size() > 64) {
            inputsByTurn.remove(inputsByTurn.keySet().iterator().next());
        }
    }

    @Override
    public void onTurnError(int turnSeq, Throwable error) {
        String input = inputsByTurn.remove(turnSeq);
        if (input == null) {
            input = "";
        }
        String errorClass = error == null ? "unknown" : error.getClass().getName();
        String errorMessage = error == null || error.getMessage() == null
                ? "" : error.getMessage();
        if (errorMessage.length() > ERROR_MESSAGE_CHARS) {
            errorMessage = errorMessage.substring(0, ERROR_MESSAGE_CHARS);
        }
        String inputPreview = input.length() > inputPreviewChars
                ? input.substring(0, inputPreviewChars) + "…" : input;
        snapshots.addLast(new Snapshot(turnSeq, errorClass, errorMessage, inputPreview));
        while (snapshots.size() > capacity) {
            snapshots.removeFirst();
        }
        totalErrors++;
    }

    /** 失败快照（旧→新）。 */
    public synchronized List<Snapshot> snapshot() {
        return List.copyOf(snapshots);
    }

    /** 累计失败轮数（环形外总量）。 */
    public long totalErrors() {
        return totalErrors;
    }

    /** JSONL 导出（一行一快照——60/67 导出族同构）。 */
    public long exportJsonl(java.io.Writer out) throws java.io.IOException {
        long lines = 0;
        for (Snapshot s : snapshot()) {
            out.write("{\"turnSeq\":" + s.turnSeq()
                    + ",\"errorClass\":\"" + escape(s.errorClass())
                    + "\",\"errorMessage\":\"" + escape(s.errorMessage())
                    + "\",\"inputPreview\":\"" + escape(s.inputPreview())
                    + "\"}\n");
            lines++;
        }
        return lines;
    }

    private static String escape(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
