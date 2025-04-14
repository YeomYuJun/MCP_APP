package com.yy.mcpapp.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CustomMcpSyncClientCustomizer implements McpSyncClientCustomizer {
    private static final Logger logger = LoggerFactory.getLogger(CustomMcpSyncClientCustomizer.class);

    private final ChatClient chatClient;

    public CustomMcpSyncClientCustomizer(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void customize(String name, McpClient.SyncSpec spec) {
        // 요청 타임아웃 증가
        spec.requestTimeout(Duration.ofMinutes(3));

        // 로깅 핸들러 추가
        spec.loggingConsumer((log) -> {
            System.out.println("MCP Log [" + log.level() + "]: " + log.logger());
        });
        // 모든 이벤트에 로깅 추가
        spec.toolsChangeConsumer(tools -> {
            logger.info("Tools changed: {}", tools.size());
        });

        spec.resourcesChangeConsumer(resources -> {
            logger.info("Resources changed: {}", resources.size());
        });

        spec.promptsChangeConsumer(prompts -> {
            logger.info("Prompts changed: {}", prompts.size());
        });

        spec.loggingConsumer(log -> {
            logger.info("Server log [{}]: {}", log.level(), log.logger());
        });
        // 샘플링 핸들러 추가
        spec.sampling(messageRequest -> {
            // OpenAI를 통해 메시지 생성
            // ChatClient를 사용하여 응답 생성
            String result = chatClient.prompt()
                    .user(messageRequest.systemPrompt())
                    .call()
                    .content();

            return McpSchema.CreateMessageResult.builder()
                    .model("gpt-4o-mini")
                    .message(result)
                    .build();
        });
    }
}
