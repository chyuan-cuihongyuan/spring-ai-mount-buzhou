package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 提示词角色构成拆解读数（spec 704 / T1008，Langfuse prompt analytics 思想）：
 * 按 MessageType 角色聚合字符量/消息数/占比——181 水位告警后「谁在吃预算」
 * 的证据面。纯函数无状态无行为（观测不拦截——压缩动作归既有机制）。
 *
 * <p>字符口径与 181 同边界（token 精算归装配侧 ContextWindowResolver）；
 * media 字节不计。sections 按字符降序+同值角色名字典序稳定——快照断言可复现。
 */
public final class PromptComposition {

    /** 单角色构成（role=MessageType 名；share=chars/totalChars，total=0 → 0.0）。 */
    public record Section(String role, int messages, long chars, double share) {
    }

    /** 不可变报告（sections 降序；share 精确到角色占比）。 */
    public record Report(List<Section> sections, long totalChars) {
    }

    private PromptComposition() {
    }

    /** 拆解（null fail-fast；空 prompt → 空 sections+total=0）。 */
    public static Report analyze(Prompt prompt) {
        Objects.requireNonNull(prompt, "prompt");
        Map<String, long[]> byRole = new LinkedHashMap<>(); // [0]=messages [1]=chars
        if (prompt.getInstructions() != null) {
            for (Message message : prompt.getInstructions()) {
                if (message == null) {
                    continue;
                }
                String role = roleOf(message);
                long[] agg = byRole.computeIfAbsent(role, k -> new long[2]);
                agg[0]++;
                agg[1] += charsOf(message);
            }
        }
        long total = byRole.values().stream().mapToLong(agg -> agg[1]).sum();
        List<Section> sections = new ArrayList<>(byRole.size());
        byRole.forEach((role, agg) -> sections.add(new Section(role, (int) agg[0], agg[1],
                total == 0 ? 0.0 : (double) agg[1] / total)));
        sections.sort(Comparator.comparingLong(Section::chars).reversed()
                .thenComparing(Section::role));
        return new Report(List.copyOf(sections), total);
    }

    /** 角色名归一（已知类型取名；未知类型兜底 MessageType 名/class 简名——扩展前向兼容）。 */
    private static String roleOf(Message message) {
        if (message instanceof SystemMessage) {
            return "SYSTEM";
        }
        if (message instanceof UserMessage) {
            return "USER";
        }
        if (message instanceof AssistantMessage) {
            return "ASSISTANT";
        }
        if (message instanceof ToolResponseMessage) {
            return "TOOL";
        }
        return message.getMessageType() == null ? "UNKNOWN" : message.getMessageType().name();
    }

    /**
     * 文本字符计量：常规消息取 getText()；TOOL 响应的载荷在 getResponses()
     * （getText 不含 responseData——实测口径），逐条 responseData 长度求和。
     * null 安全。
     */
    private static long charsOf(Message message) {
        if (message instanceof ToolResponseMessage toolMessage && toolMessage.getResponses() != null) {
            long total = 0;
            for (ToolResponseMessage.ToolResponse response : toolMessage.getResponses()) {
                if (response != null && response.responseData() != null) {
                    total += response.responseData().length();
                }
            }
            return total;
        }
        String text = message.getText();
        return text == null ? 0 : text.length();
    }
}
