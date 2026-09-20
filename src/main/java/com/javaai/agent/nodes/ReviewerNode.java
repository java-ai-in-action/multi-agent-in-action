package com.javaai.agent.nodes;

import com.javaai.agent.state.AgentState;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Reviewer Agent：质检 —— 由它决定「打回重做」还是「放行」。
 *
 * <p>它是把一次解决率从 45% 拉到 78% 的关键角色。
 *
 * <p><b>踩坑点③</b>：审核标准必须写进 Prompt 并<b>逐条列明</b>。只写「请审核是否正确」，
 * Reviewer 会变成「讨好型人格」——永远说 OK。
 */
@Component
public class ReviewerNode implements AgentNode {

    private final ChatClient chatClient;

    public ReviewerNode(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        你是严格的客服质检员。逐条检查执行结果，任意一条不满足即判定「不通过」：
                        1. 是否直接回答了用户的原始问题；
                        2. 是否包含具体单号 / 金额 / 时间等事实；
                        3. 是否与订单真实状态一致；
                        4. 是否包含无法兑现的承诺。
                        只输出「通过」或「不通过：<原因>」，不要输出其他内容。
                        """)
                .build();
    }

    @Override
    public Map<String, Object> apply(AgentState state) {
        String verdict = chatClient.prompt()
                .user(u -> u.text("用户原始诉求：{q}\n执行结果：{r}")
                        .param("q", state.latestUserMessage())
                        .param("r", state.getResults().toString()))
                .call()
                .content();

        boolean passed = verdict.trim().startsWith("通过");

        // 注意：无论通过与否都计数，配合图上的条件边形成硬上限
        return Map.of(
                AgentState.REVIEW_PASSED, passed,
                AgentState.RETRY_COUNT, state.getRetryCount() + 1);
    }
}
