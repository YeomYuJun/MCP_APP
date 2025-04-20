package com.yy.mcpapp.controller;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Controller
public class WebInterfaceController {
    private final ChatClient.Builder chatClientBuilder;
    private final List<McpSyncClient> mcpSyncClients;
    private final ChatClient mcpChatClient;
    
    // 대화 기록을 저장할 세션 리스트
    private final List<Message> conversationHistory = new ArrayList<>();

    public WebInterfaceController(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpSyncClients) {
        this.chatClientBuilder = chatClientBuilder;
        this.mcpSyncClients = mcpSyncClients;
        
        // MCP 도구를 포함한 ChatClient 생성
        this.mcpChatClient = chatClientBuilder
                .defaultTools(new SyncMcpToolCallbackProvider(mcpSyncClients))
                .build();
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("pageTitle", "MCP 통합 인터페이스");
        model.addAttribute("conversations", conversationHistory);
        model.addAttribute("activeMenu", "home");
        return "index";
    }

    @GetMapping("/chat")
    public String chatPage(Model model) {
        model.addAttribute("pageTitle", "AI 채팅");
        model.addAttribute("conversations", conversationHistory);
        model.addAttribute("activeMenu", "chat");
        return "chat";
    }

    @PostMapping("/chat/submit")
    public String submitChat(@RequestParam("userInput") String userInput, Model model) {
        // 사용자 입력을 대화 기록에 추가
        UserMessage userMessage = new UserMessage(userInput);
        conversationHistory.add(userMessage);
        
        // MCP 도구를 포함한 시스템 프롬프트
        String systemPrompt = """
        당신은 MCP(Model Context Protocol) 도구를 활용하는 AI 어시스턴트입니다.
        파일 시스템 및 Excel 작업을 수행할 수 있으며, 사용자의 요청에 따라 적절한 도구를 사용하세요.
        
        도구를 활용하여:
        1. 파일 읽기(read_file): 파일 내용을 읽을 수 있습니다.
        2. 파일 쓰기(write_file): 새 파일을 생성하거나 기존 파일을 수정할 수 있습니다.
        3. 디렉토리 목록(list_directory): 디렉토리 내용을 나열할 수 있습니다.
        4. Excel 시트 분석: Excel 파일의 데이터를 분석할 수 있습니다.
        
        모든 파일 경로는 /data/ 디렉토리 내에서 접근 가능합니다.
        """;

        String aiResponse = mcpChatClient.prompt()
                .system(systemPrompt)
                .user(userInput)
                .call()
                .content();

        // AI 응답을 대화 기록에 추가
        AssistantMessage assistantMessage = new AssistantMessage(aiResponse);
        conversationHistory.add(assistantMessage);

        // AI 응답 생성
        model.addAttribute("conversations", conversationHistory);
        model.addAttribute("activeMenu", "chat");
        return "chat";
    }

    @GetMapping("/document")
    public String documentPage(Model model) {
        model.addAttribute("pageTitle", "문서 처리");
        model.addAttribute("activeMenu", "document");
        return "document";
    }

    @PostMapping("/document/upload")
    public String uploadDocument(@RequestParam("file") MultipartFile file, Model model) {
        if (file.isEmpty()) {
            model.addAttribute("message", "파일을 선택해주세요.");
            return "document";
        }

        try {
            // 공유 디렉토리에 파일 저장 (서버에서 /data로 마운트)
            String uploadDir = "C:/servers/shared_data";
            Path uploadPath = Paths.get(uploadDir);
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            String fileName = file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            
            // 파일 타입 확인
            String fileType = getFileType(fileName);
            
            // 시스템 프롬프트 설정
            String systemPrompt;
            if ("xlsx".equals(fileType)) {
                systemPrompt = """
                당신은 Excel 파일을 분석하는 데이터 분석 전문가입니다.
                Excel MCP 도구를 활용하여 파일을 분석하세요.
                다음 단계로 분석을 수행하세요:
                
                1. 먼저 read_sheet_names를 사용하여 Excel 파일의 모든 시트를 확인하세요.
                2. 각 시트의 데이터를 read_sheet_data를 사용하여 읽어오세요.
                3. 필요한 경우 read_sheet_formula를 사용하여 수식을 확인하세요.
                4. 데이터를 분석하고 인사이트를 추출하세요.
                """;
            } else {
                systemPrompt = """
                당신은 문서 분석 전문가입니다.
                파일시스템 MCP 도구를 활용하여 파일을 분석하세요.
                파일의 내용을 읽고, 주요 내용과 인사이트를 추출하세요.
                """;
            }
            
            // 사용자 프롬프트 설정
            String userPrompt = String.format("다음 파일을 분석해주세요: /data/%s\n전반적인 분석 결과와 인사이트를 제공해주세요.", fileName);
            
            // AI 응답 생성
            String aiResponse = mcpChatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
            
            model.addAttribute("fileName", fileName);
            model.addAttribute("filePath", "/data/" + fileName);
            model.addAttribute("analysis", aiResponse);
            model.addAttribute("activeMenu", "document");
            
            return "document-analysis";
            
        } catch (IOException e) {
            model.addAttribute("message", "파일 업로드 중 오류가 발생했습니다: " + e.getMessage());
            return "document";
        }
    }

    @GetMapping("/excel")
    public String excelPage(Model model) {
        model.addAttribute("pageTitle", "Excel 처리");
        model.addAttribute("activeMenu", "excel");
        return "excel";
    }

    @PostMapping("/excel/analyze")
    public String analyzeExcel(@RequestParam("filePath") String filePath, 
                              @RequestParam(value = "question", required = false) String question, 
                              Model model) {
        
        // Excel 분석용 시스템 프롬프트
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
        
        // 사용자 프롬프트 설정
        String userPrompt;
        if (question != null && !question.isEmpty()) {
            userPrompt = String.format("다음 Excel 파일을 분석해주세요: %s\n\n질문: %s", filePath, question);
        } else {
            userPrompt = String.format("다음 Excel 파일을 분석해주세요: %s\n\n전반적인 분석 결과와 인사이트를 제공해주세요.", filePath);
        }
        
        // AI 응답 생성
        String aiResponse = mcpChatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
        
        model.addAttribute("filePath", filePath);
        model.addAttribute("question", question);
        model.addAttribute("analysis", aiResponse);
        model.addAttribute("activeMenu", "excel");
        
        return "excel-analysis";
    }

    // 파일 확장자를 추출하는 도우미 메서드
    private String getFileType(String fileName) {
        if (fileName == null) return "";
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // 확장자 없음
        }
        return fileName.substring(lastIndexOf + 1).toLowerCase();
    }
}
