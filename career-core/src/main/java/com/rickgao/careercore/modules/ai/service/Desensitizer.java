package com.rickgao.careercore.modules.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 自由文本脱敏器（Phase 3，移植自 career-ai services/desensitizer.py）。
 * 命中即替换为占位符，并返回各类型命中次数。
 *
 * <p>顺序：长/精确模式优先（身份证 → 手机 → 学号），避免被更短的兜底规则误吞。
 * 边界统一用 (?&lt;!\d)/(?!\d) 环视，避开 Python 中 \b 在中文语境下静默失效的问题。
 *
 * <p>Demo 精简点：当前为纯正则方案；姓名/地址识别后续可接 Presidio 等 NER 方案。
 */
@Component
public class Desensitizer {

    private static final Logger log = LoggerFactory.getLogger(Desensitizer.class);

    private record Rule(String name, Pattern pattern, String placeholder) {}

    private final java.util.List<Rule> rules;

    public Desensitizer() {
        rules = java.util.List.of(
                new Rule("邮箱",
                        Pattern.compile("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}"),
                        "[邮箱]"),
                new Rule("身份证号",
                        Pattern.compile(
                                "(?<!\\d)(?:\\d{6}(?:18|19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]"
                                        + "|\\d{15})(?!\\d)"),
                        "[身份证号]"),
                new Rule("手机号",
                        Pattern.compile(
                                "(?<!\\d)(?:\\+?86[\\s\\-]?)?0?1[3-9]\\d[\\s\\-]?\\d{4}[\\s\\-]?\\d{4}(?!\\d)"),
                        "[手机号]"),
                new Rule("学号",
                        Pattern.compile("(?<!\\d)\\d{8,12}(?!\\d)"),
                        "[学号]")
        );
    }

    /** 脱敏并返回各类型命中次数。空文本原样返回。 */
    public DesensitizeResult desensitizeDetailed(String text) {
        if (text == null || text.isEmpty()) {
            return new DesensitizeResult(text, Map.of());
        }
        String out = text;
        Map<String, Integer> hits = new LinkedHashMap<>();
        for (Rule rule : rules) {
            Matcher m = rule.pattern.matcher(out);
            StringBuffer sb = new StringBuffer();
            int n = 0;
            while (m.find()) {
                m.appendReplacement(sb, Matcher.quoteReplacement(rule.placeholder));
                n++;
            }
            if (n > 0) {
                m.appendTail(sb);
                out = sb.toString();
                hits.put(rule.name, hits.getOrDefault(rule.name, 0) + n);
            }
        }
        return new DesensitizeResult(out, hits);
    }

    /** 仅返回脱敏后文本（兼容旧调用）。 */
    public String desensitize(String text) {
        return desensitizeDetailed(text).masked();
    }

    /** 自由文本脱敏 + 命中数日志（各 AI 入口统一调用）。 */
    public String maskFreeText(String text) {
        DesensitizeResult r = desensitizeDetailed(text);
        if (!r.hits().isEmpty()) {
            log.info("脱敏命中: {}", r.hits());
        }
        return r.masked();
    }

    public record DesensitizeResult(String masked, Map<String, Integer> hits) {}
}
