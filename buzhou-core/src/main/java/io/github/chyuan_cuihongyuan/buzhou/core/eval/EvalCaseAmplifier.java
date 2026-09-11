package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

/**
 * 评估集合成扩增（spec 525 / T803，Ragas testset generation 借鉴）：种子
 * 用例 → LLM 生成 N 条同语义改写（保持语义与判定等价、改表层表述）→
 * 产出入库候选。**人审教义**：调用方决定入库与否——本类只产候选，不做
 * 语义等价断言（判定等价是语义问题）。
 *
 * <p>解析容错：围栏剥离 + 逐行隔离——单坏行不影响好行（unparseable 计数）；
 * 零可解析 → EVAL_OPERATION_INVALID 带模型输出预览。
 */
public final class EvalCaseAmplifier {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int PREVIEW_CHARS = 500;

    private final ChatModel model;

    public EvalCaseAmplifier(ChatModel model) {
        if (model == null) {
            throw new IllegalArgumentException("ChatModel 必须非空");
        }
        this.model = model;
    }

    /** 内置改写指令（保持语义与判定等价；count 注入）。 */
    static String instruction(String input, String expected, int count) {
        return "你是评估数据集扩增助手。下面是一条种子用例（输入 + 期望输出）。"
                + "请生成 " + count + " 条同语义改写：只改表层表述（措辞/句式/别名），"
                + "绝不改变语义与判定结果。每行输出一个 JSON 对象："
                + "{\"input\":\"...\",\"expected\":\"...\"}，不要输出其他内容。\n\n"
                + "种子输入：" + input + "\n种子期望：" + expected;
    }

    /**
     * 扩增：返回 count 条以内的候选（id=null 的未入库 EvalItem 语义——
     * 入库走 datasetStore.add 与手工项同管道重排）。候选溯源清空（合成项
     * 非回流项）。
     */
    public List<EvalItem> amplify(EvalItem seed, int count) {
        if (seed == null) {
            throw new IllegalArgumentException("种子用例非空");
        }
        if (count < 1) {
            throw new IllegalArgumentException("count >= 1（当前 " + count + "）");
        }
        String raw = callModel(instruction(seed.input(), seed.expected(), count));
        String stripped = stripFences(raw);
        List<EvalItem> out = new ArrayList<>();
        int unparseable = 0;
        for (String line : stripped.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            EvalItem candidate = parseLine(trimmed);
            if (candidate == null) {
                unparseable++;
                continue;
            }
            out.add(candidate);
            if (out.size() >= count) {
                break;
            }
        }
        if (out.isEmpty()) {
            throw new BuzhouException(ErrorCode.EVAL_OPERATION_INVALID,
                    "扩增失败：模型输出无可解析用例（unparseable=" + unparseable + "）——输出预览："
                            + preview(raw));
        }
        return out;
    }

    /** 本轮跳过的坏行数（上一次 amplify 的解析残留——读数面）。 */
    private String callModel(String prompt) {
        ChatResponse response = model.call(new Prompt(prompt));
        if (response == null || response.getResult() == null
                || response.getResult().getOutput() == null) {
            throw new BuzhouException(ErrorCode.EVAL_OPERATION_INVALID,
                    "扩增失败：模型空响应");
        }
        return response.getResult().getOutput().getText();
    }

    /** 剥离 ``` 围栏（模型常见输出形态）。 */
    static String stripFences(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline > 0) {
                text = text.substring(firstNewline + 1);
            }
            int closing = text.lastIndexOf("```");
            if (closing >= 0) {
                text = text.substring(0, closing);
            }
        }
        return text.trim();
    }

    /** 单行解析：{"input","expected"}；坏行返回 null。 */
    static EvalItem parseLine(String line) {
        try {
            JsonNode node = MAPPER.readTree(line);
            String input = node.path("input").asText(null);
            String expected = node.path("expected").asText(null);
            if (input == null || expected == null) {
                return null;
            }
            return new EvalItem(null, input, expected, null, null, null);
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            return null;
        }
    }

    private static String preview(String raw) {
        String text = raw == null ? "" : raw.trim();
        return text.length() <= PREVIEW_CHARS ? text : text.substring(0, PREVIEW_CHARS) + "…";
    }
}
