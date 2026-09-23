package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * JSON Patch 应用（spec 4034 / T6069 / impl 2135）——RFC 6902
 * 六操作思想（add/remove/replace/move/copy/test）+ RFC 6901
 * JSON Pointer（{@code ""} 根、{@code ~0}/{@code ~1} 转义、数组
 * 数字下标、add 的 {@code -} 追加）：配置/状态文档的标准增量
 * 操作面——全量覆盖（大文档重传、并发互踩）与自造 diff 语义
 * （无标准、不可审计）的病解。
 *
 * <p>原子性：深拷贝上应用，任一操作失败整 patch 拒（IAE 带
 * 操作序号），原文档不变；move = 先 remove 后 add 的下标
 * 移位诚实处理；test 失败即整 patch 失败（条件前置语义）。
 */
public final class JsonPatchApplier {

    private static final String OP_ADD = "add";
    private static final String OP_REMOVE = "remove";
    private static final String OP_REPLACE = "replace";
    private static final String OP_MOVE = "move";
    private static final String OP_COPY = "copy";
    private static final String OP_TEST = "test";
    private static final String ARRAY_APPEND_TOKEN = "-";
    private static final String ESCAPED_TILDE = "~0";
    private static final String ESCAPED_SLASH = "~1";

    private JsonPatchApplier() {
    }

    /**
     * 原子应用 patch（操作序即数组序）。
     *
     * @param document 原文档（不被修改）
     * @param patch RFC 6902 操作数组
     * @return 应用后的新文档
     * @throws IllegalArgumentException patch 形态非法或任一操作失败
     */
    public static JsonNode apply(JsonNode document, JsonNode patch) {
        if (document == null || patch == null || !patch.isArray()) {
            throw new IllegalArgumentException("文档与 patch（数组）非空");
        }
        JsonNode working = document.deepCopy();
        for (int i = 0; i < patch.size(); i++) {
            working = applyOp(working, patch.get(i), i);
        }
        return working;
    }

    private static JsonNode applyOp(JsonNode root, JsonNode op, int index) {
        if (op == null || !op.isObject() || !op.has("op") || !op.has("path")
                || !op.get("path").isTextual()) {
            throw new IllegalArgumentException("操作 #" + index + " 需对象且含 op/path");
        }
        String kind = op.get("op").asText();
        List<String> tokens = parsePointer(op.get("path").asText(), index);
        switch (kind) {
            case OP_ADD:
                return addAt(root, tokens, requiredValue(op, index), index);
            case OP_REPLACE:
                return replaceAt(root, tokens, requiredValue(op, index), index);
            case OP_REMOVE:
                return removeAt(root, tokens, index);
            case OP_MOVE:
                return moveOp(root, textField(op, "from", index), tokens, index);
            case OP_COPY:
                return copyOp(root, textField(op, "from", index), tokens, index);
            case OP_TEST:
                return testOp(root, tokens, requiredValue(op, index), index);
            default:
                throw new IllegalArgumentException("操作 #" + index + " 未知 op：" + kind);
        }
    }

    private static JsonNode addAt(JsonNode root, List<String> tokens, JsonNode value, int index) {
        if (tokens.isEmpty()) {
            return value;
        }
        String leaf = tokens.get(tokens.size() - 1);
        JsonNode parent = navigate(root, tokens.subList(0, tokens.size() - 1));
        if (parent == null || parent.isMissingNode()) {
            throw new IllegalArgumentException("操作 #" + index + " 父路径不存在：/" + String.join("/", tokens));
        }
        if (parent.isObject()) {
            ((ObjectNode) parent).set(leaf, value);
            return root;
        }
        if (parent.isArray()) {
            ArrayNode array = (ArrayNode) parent;
            if (ARRAY_APPEND_TOKEN.equals(leaf)) {
                array.add(value);
                return root;
            }
            int position = arrayIndex(leaf, index);
            if (position < 0 || position > array.size()) {
                throw new IllegalArgumentException("操作 #" + index + " 数组下标越界：" + leaf);
            }
            array.insert(position, value);
            return root;
        }
        throw new IllegalArgumentException("操作 #" + index + " 父路径非容器");
    }

    private static JsonNode replaceAt(JsonNode root, List<String> tokens, JsonNode value, int index) {
        if (tokens.isEmpty()) {
            return value;   // 根替换（根恒存在）
        }
        String leaf = tokens.get(tokens.size() - 1);
        JsonNode parent = navigate(root, tokens.subList(0, tokens.size() - 1));
        if (parent == null || parent.isMissingNode()) {
            throw new IllegalArgumentException("操作 #" + index + " 父路径不存在");
        }
        if (parent.isObject()) {
            if (!parent.has(leaf)) {
                throw new IllegalArgumentException("操作 #" + index + " replace 目标不存在（应 add）");
            }
            ((ObjectNode) parent).set(leaf, value);
            return root;
        }
        if (parent.isArray()) {
            ArrayNode array = (ArrayNode) parent;
            int position = arrayIndex(leaf, index);   // "-" 对 replace 非法
            if (position < 0 || position >= array.size()) {
                throw new IllegalArgumentException("操作 #" + index + " replace 目标不存在：" + leaf);
            }
            array.set(position, value);   // 原位替换——非 insert 移位
            return root;
        }
        throw new IllegalArgumentException("操作 #" + index + " 父路径非容器");
    }

    private static JsonNode removeAt(JsonNode root, List<String> tokens, int index) {
        Removed removed = removeExisting(root, tokens, index);
        return removed.root();
    }

    private static Removed removeExisting(JsonNode root, List<String> tokens, int index) {
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("操作 #" + index + " 不能移除根");
        }
        String leaf = tokens.get(tokens.size() - 1);
        JsonNode parent = navigate(root, tokens.subList(0, tokens.size() - 1));
        if (parent == null || parent.isMissingNode()) {
            throw new IllegalArgumentException("操作 #" + index + " 父路径不存在");
        }
        if (parent.isObject()) {
            ObjectNode object = (ObjectNode) parent;
            if (!object.has(leaf)) {
                throw new IllegalArgumentException("操作 #" + index + " 成员不存在：" + leaf);
            }
            JsonNode removed = object.get(leaf);
            object.remove(leaf);
            return new Removed(root, removed);
        }
        if (parent.isArray()) {
            ArrayNode array = (ArrayNode) parent;
            int position = arrayIndex(leaf, index);
            if (position < 0 || position >= array.size()) {
                throw new IllegalArgumentException("操作 #" + index + " 数组下标非法：" + leaf);
            }
            JsonNode removed = array.get(position);
            array.remove(position);
            return new Removed(root, removed);
        }
        throw new IllegalArgumentException("操作 #" + index + " 目标父非容器");
    }

    private static JsonNode moveOp(JsonNode root, String from, List<String> tokens, int index) {
        List<String> fromTokens = parsePointer(from, index);
        Removed removed = removeExisting(root, fromTokens, index);
        return addAt(removed.root(), tokens, removed.value(), index);   // 移位后的下标记账
    }

    private static JsonNode copyOp(JsonNode root, String from, List<String> tokens, int index) {
        List<String> fromTokens = parsePointer(from, index);
        JsonNode source = navigate(root, fromTokens);
        if (source == null || source.isMissingNode()) {
            throw new IllegalArgumentException("操作 #" + index + " copy 源不存在：" + from);
        }
        return addAt(root, tokens, source.deepCopy(), index);
    }

    private static JsonNode testOp(JsonNode root, List<String> tokens, JsonNode expected, int index) {
        JsonNode actual = navigate(root, tokens);
        if (actual == null || actual.isMissingNode() || !actual.equals(expected)) {
            throw new IllegalArgumentException("操作 #" + index + " test 断言失败");
        }
        return root;
    }

    /** 指针求值（缺路径返回 null——调用方判错）。 */
    private static JsonNode navigate(JsonNode node, List<String> tokens) {
        JsonNode current = node;
        for (String token : tokens) {
            if (current == null) {
                return null;
            }
            current = childOf(current, token);
        }
        return current;
    }

    private static JsonNode childOf(JsonNode node, String token) {
        if (node.isObject()) {
            return node.has(token) ? node.get(token) : null;
        }
        if (node.isArray()) {
            int position = arrayIndex(token, -1);
            if (position < 0 || position >= node.size()) {
                return null;
            }
            return node.get(position);
        }
        return null;
    }

    /** RFC 6901 指针解析（"" 根；~1→/ 后 ~0→~ 转义序）。 */
    private static List<String> parsePointer(String pointer, int index) {
        if (pointer.isEmpty()) {
            return List.of();
        }
        if (!pointer.startsWith("/")) {
            throw new IllegalArgumentException("操作 #" + index + " 指针需以 / 起或空串为根：" + pointer);
        }
        List<String> tokens = new ArrayList<>(Arrays.asList(pointer.substring(1).split("/", -1)));
        tokens.replaceAll(JsonPatchApplier::unescape);
        return tokens;
    }

    private static String unescape(String token) {
        if (!token.contains("~")) {
            return token;
        }
        return token.replace(ESCAPED_SLASH, "/").replace(ESCAPED_TILDE, "~");
    }

    /** RFC 6901 数组下标（0 或非零起数字；"-" 拒——add 追加走专用分支）。 */
    private static int arrayIndex(String token, int index) {
        if (token.isEmpty() || ARRAY_APPEND_TOKEN.equals(token)) {
            throw new IllegalArgumentException("操作 #" + index + " 数组下标非法：" + token);
        }
        for (int i = 0; i < token.length(); i++) {
            char ch = token.charAt(i);
            if (ch < '0' || ch > '9' || (i == 0 && ch == '0' && token.length() > 1)) {
                throw new IllegalArgumentException("操作 #" + index + " 数组下标非法：" + token);
            }
        }
        return Integer.parseInt(token);
    }

    private static JsonNode requiredValue(JsonNode op, int index) {
        if (!op.has("value")) {
            throw new IllegalArgumentException("操作 #" + index + " 缺 value 字段");
        }
        return op.get("value");
    }

    private static String textField(JsonNode op, String field, int index) {
        if (!op.has(field) || !op.get(field).isTextual()) {
            throw new IllegalArgumentException("操作 #" + index + " 缺 " + field + " 字段");
        }
        return op.get(field).asText();
    }

    private record Removed(JsonNode root, JsonNode value) {
    }
}
