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
@RequestMapping("/excel")
public class ExcelController {
    private final ChatClient.Builder chatClientBuilder;
    private final List<McpSyncClient> mcpSyncClients;
    private ChatClient mcpChatClient; // 한 번만 생성하여 재사용

    public ExcelController(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpSyncClients) {
        this.chatClientBuilder = chatClientBuilder;
        this.mcpSyncClients = mcpSyncClients;
        // 초기화 시 mcpChatClient 생성 (모든 MCP 도구 포함)
        this.mcpChatClient = chatClientBuilder
                .defaultTools(new SyncMcpToolCallbackProvider(mcpSyncClients))
                .build();
    }

    @GetMapping("/ai")
    public String excelGeneration(@RequestParam String userInput) {
        // 시스템 프롬프트 정의
        String systemPrompt = """
        당신은 Excel 파일을 처리할 수 있는 AI 어시스턴트입니다.
        Excel MCP 도구를 활용하여 다음 작업을 수행할 수 있습니다:
        
        1. 시트 이름 읽기(read_sheet_names): Excel 파일의 모든 시트 이름을 나열합니다.
        2. 시트 데이터 읽기(read_sheet_data): Excel 시트에서 데이터를 읽어옵니다.
        3. 시트 수식 읽기(read_sheet_formula): Excel 시트에서 수식을 읽어옵니다.
        4. 시트 데이터 쓰기(write_sheet_data): Excel 시트에 데이터를 씁니다.
        5. 시트 수식 쓰기(write_sheet_formula): Excel 시트에 수식을 씁니다.
        
        Excel 파일 및 파일시스템 경로는 모두 /data/ 디렉토리 내에서 접근 가능합니다.
        사용자의 요청에 따라 적절한 도구를 선택하여 사용하세요.
        """;
        
        // 시스템 프롬프트와 사용자 입력으로 프롬프트 전송
        return mcpChatClient.prompt()
                .system(systemPrompt)
                .user(userInput)
                .call()
                .content();
    }

    @GetMapping("/analyse")
    public String analyseExcel(@RequestParam String filePath, @RequestParam(required = false) String question) {
        // Excel 파일 분석을 위한 시스템 프롬프트
        String systemPrompt = """
        당신은 Excel 파일을 분석하는 데이터 분석 전문가입니다.
        Excel MCP 도구를 활용하여 다음 단계로 분석을 수행하세요:
        
        1. 먼저 read_sheet_names를 사용하여 Excel 파일의 모든 시트를 확인하세요.
        2. 각 시트의 데이터를 read_sheet_data를 사용하여 읽어오세요.
        3. 필요한 경우 read_sheet_formula를 사용하여 수식을 확인하세요.
        4. 데이터를 분석하고 인사이트를 추출하세요.
        5. 사용자의 질문에 상세히 답변하세요.
        
        답변 시 다음 형식을 따르세요:
        1. 파일 구조 요약 (시트 목록 및 각 시트 개요)
        2. 주요 데이터 포인트 (중요한 숫자, 트렌드, 패턴)
        3. 분석 결과 (사용자 질문에 대한 직접적인 답변)
        4. 추천 사항 (데이터 기반 제안, 해당되는 경우)
        """;

        // 사용자 입력 구성
        String userPrompt = "";
        if (question != null && !question.isEmpty()) {
            userPrompt = String.format("다음 Excel 파일을 분석해주세요: %s\n\n질문: %s", filePath, question);
        } else {
            userPrompt = String.format("다음 Excel 파일을 분석해주세요: %s\n\n전반적인 분석 결과와 인사이트를 제공해주세요.", filePath);
        }
        
        // 시스템 프롬프트와 사용자 입력으로 프롬프트 전송
        return mcpChatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

    @GetMapping("/simple")
    public Map<String, String> excelInfo(@RequestParam(value = "message", defaultValue = "Tell me about Excel features") String message) {
        ChatClient basicChatClient = chatClientBuilder.build();
        return Map.of("completion", basicChatClient.prompt().user(message).call().content());
    }
}
