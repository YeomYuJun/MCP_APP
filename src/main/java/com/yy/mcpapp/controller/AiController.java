package com.yy.mcpapp.controller;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class AiController {
    private final ChatClient.Builder chatClientBuilder;
    private final List<McpSyncClient> mcpSyncClients;
    private ChatClient mcpChatClient; // 한 번만 생성하여 재사용


    public AiController(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpSyncClients) {
        this.chatClientBuilder = chatClientBuilder;
        this.mcpSyncClients = mcpSyncClients;
        // 초기화 시 mcpChatClient 생성
        this.mcpChatClient = chatClientBuilder
                .defaultTools(new SyncMcpToolCallbackProvider(mcpSyncClients))
                .build();
    }

    @GetMapping("/ai")
    public String generation(@RequestParam String userInput) {
        // 기본 도구 없는 ChatClient 생성
        ChatClient basicChatClient = chatClientBuilder.build();

        return basicChatClient.prompt()
                .user(userInput)
                .call()
                .content();
    }

    @GetMapping("/ai/mcp")
    public String mcpGeneration(@RequestParam String userInput) {
        // MCP 도구를 포함한 ChatClient 생성

        // 시스템 프롬프트 정의
        String systemPrompt = """
        당신은 MCP(Model Context Protocol) 도구를 활용하는 AI 어시스턴트입니다.
        파일 시스템 작업을 수행할 수 있으며, 사용자의 요청에 따라 적절한 도구를 사용하세요.
        
        파일 시스템 도구를 활용해서:
        1. 파일 읽기(read_file): 파일 내용을 읽을 수 있습니다.
        2. 파일 쓰기(write_file): 새 파일을 생성하거나 기존 파일을 수정할 수 있습니다.
        3. 디렉토리 목록(list_directory): 디렉토리 내용을 나열할 수 있습니다.
        4. 디렉토리 구조(directory_tree): 디렉토리 구조를 트리 형태로 표시할 수 있습니다.
        5. 파일 검색(search_files): 특정 패턴으로 파일을 검색할 수 있습니다.
        
        모든 파일 경로는 /data/ 디렉토리 내에서 접근 가능합니다.
        """;
        // 시스템 프롬프트와 사용자 입력으로 프롬프트 전송
        return mcpChatClient.prompt()
                .system(systemPrompt)
                .user(userInput)
                .call()
                .content();
    }

    @GetMapping("/ai/simple")
    public Map<String, String> completion(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        ChatClient basicChatClient = chatClientBuilder.build();
        return Map.of("completion", basicChatClient.prompt().user(message).call().content());
    }
}
