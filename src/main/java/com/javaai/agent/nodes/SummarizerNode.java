package com.javaai.agent.nodes;

import com.javaai.agent.state.AgentState;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Summarizer Agent：把结构化结果「翻译成人话」。
 *
 * <p><b>铁律</b>：它只做翻译，<b>不允许新增任何事实</b>——否则就是在编造。
 * 所以它的 Prompt 里第一条就是「不允许新增执行结果里没有的事实」，而不是「写得好听点」。
 */
@Component
public class SummarizerNode implements AgentNode {

    private final ChatClient chatClient;

    public SummarizerNode(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        你是客服话术专家。把结构化执行结果翻译成一段自然、有温度的中文回复。
                        严格要求：
                        1. 不允许新增任何执行结果里没有的事实；
                        2. 先共情、再说明、最后给方案；
                        3. 不超过 200 字。
                        """)
                .build();
    }

    @Override
    public Map<String, Object> apply(AgentState state) {
        String answer = chatClient.prompt()
                .user(u -> u.text("用户诉求：{q}\n执行结果：{r}")
                        .param("q", state.latestUserMessage())
                        .param("r", state.getResults().toString()))
                .call()
                .content();
        return Map.of(AgentState.FINAL_ANSWER, answer);
    }
}
