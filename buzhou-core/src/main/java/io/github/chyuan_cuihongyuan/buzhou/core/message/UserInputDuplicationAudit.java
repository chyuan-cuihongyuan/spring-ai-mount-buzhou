package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户输入重复审计（spec 1426 / T2153 / impl 1079）——Rasa 对话分析思想
 * （重复用户输入是最强的挫败信号——「问题没被听懂」用户就开始复读）：
 * 会话历史中 USER 消息的重复形态审计——连续复读对数、最长复读游程、
 * 高频输入 Top（归一化后：去空白、小写——「重新试一次」类短复读不上榜的
 * 噪声抑制口径显式）。
 *
 * <p>纯函数零状态：吃 USER 消息列表（调用方从 history 过滤 role=USER）；
 * 不裁决不拦截（重复输入是否升级人工归宿主）。Top 榜封顶 8、条目截断
 * 64 字符（基数与隐私纪律）。
 */
public final class UserInputDuplicationAudit {

    /** Top 榜容量。 */
    static final int TOP_CAPACITY = 8;

    /** 归一化条目最大长度（超出截断——隐私与基数纪律）。 */
    static final int NORMALIZED_MAX_LEN = 64;

    private UserInputDuplicationAudit() {
    }

    /**
     * @param totalInputs               USER 输入总数
     * @param consecutiveDuplicatePairs 相邻且归一化相同的输入对数（复读直接信号）
     * @param maxRepeatRun              单条输入的最长复读游程（含首条；1 = 无复读）
     * @param distinctInputs            归一化后不同输入数（与 totalInputs 对比看多样性）
     */
    public record DuplicationReport(int totalInputs, int consecutiveDuplicatePairs,
                                    int maxRepeatRun, int distinctInputs,
                                    List<Map.Entry<String, Long>> topRepeated) {
    }

    /** 审计入口：role=USER 的消息内容按序喂入。 */
    public static DuplicationReport analyze(List<String> userInputTexts) {
        if (userInputTexts == null || userInputTexts.isEmpty()) {
            return new DuplicationReport(0, 0, 0, 0, List.of());
        }
        List<String> normalized = userInputTexts.stream()
                .map(UserInputDuplicationAudit::normalize)
                .toList();
        int consecutive = 0;
        int maxRun = 1;
        int run = 1;
        for (int i = 1; i < normalized.size(); i++) {
            if (normalized.get(i).equals(normalized.get(i - 1))) {
                consecutive++;
                run++;
                maxRun = Math.max(maxRun, run);
            } else {
                run = 1;
            }
        }
        Map<String, Long> counts = new LinkedHashMap<>();
        normalized.forEach(t -> counts.merge(t, 1L, Long::sum));
        List<Map.Entry<String, Long>> top = counts.entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP_CAPACITY)
                .toList();
        return new DuplicationReport(normalized.size(), consecutive, maxRun,
                counts.size(), List.copyOf(top));
    }

    /** 归一化：去首尾空白 + 小写 + 内部空白折叠 + 截断 64 字符。 */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String collapsed = text.trim().toLowerCase().replaceAll("\\s+", " ");
        return collapsed.length() > NORMALIZED_MAX_LEN
                ? collapsed.substring(0, NORMALIZED_MAX_LEN) : collapsed;
    }
}
