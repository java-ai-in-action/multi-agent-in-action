package com.javaai.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.javaai.agent.state.WorkflowKeys;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Planner Agent：把用户诉求拆解为 1–5 个可执行子任务。
 *
 * <p><b>踩坑点①</b>：计划必须强制 JSON 输出。让模型自由发挥自然语言计划，
 * 下游解析全靠正则，脆得一批。
 *
 * <p>直接实现框架的 {@link NodeAction}，在图中通过 {@code node_async(planner)} 注册。
 */
@Component
public class PlannerNode implements NodeAction {

    private final ChatClient chatClient;

    public PlannerNode(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        你是电商客服的任务规划专家。
                        把用户诉求拆解为 1-5 个可执行的子任务，每个子任务对应一次工具调用。
                        只输出 JSON 字符串数组，元素形如 {"action":"查询订单状态"}，不要输出任何解释。
                        """)
                .build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        // 从 OverAllState 按「键」读取状态
        String message = state.value(WorkflowKeys.MESSAGE, "");

        String answer = chatClient.prompt()
                .user(u -> u.text("用户诉求：{msg}").param("msg", message))
                .call()
                .content();

        return Map.of(WorkflowKeys.TASKS, parseJsonArray(answer));
    }

    /** 解析模型输出的 JSON 数组；解析失败则退化为「整条诉求 = 单个任务」 */
    private List<String> parseJsonArray(String raw) {
        List<String> tasks = new ArrayList<>();
        try {
            String body = raw.substring(raw.indexOf('[') + 1, raw.lastIndexOf(']'));
            for (String part : body.split("\\},\\s*\\{")) {
                String action = part.replaceAll(".*\"action\"\\s*:\\s*\"([^\"]*)\".*", "$1");
                if (!action.isBlank() && !action.equals(part)) {
                    tasks.add(action);
                }
            }
        } catch (Exception e) {
            if (raw != null && !raw.isBlank()) {
                tasks.add(raw.trim());
            }
        }
        return tasks.isEmpty() ? List.of("处理用户诉求") : tasks;
    }
}
