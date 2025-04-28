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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpSession;

/**
 * MCP 통합 웹 인터페이스 컨트롤러
 * 사용자별 세션 UUID를 부여하고 해당 사용자의 전용 디렉토리를 사용함
 */
@Controller
public class WebInterfaceController {
    private final ChatClient.Builder chatClientBuilder;
    private final List<McpSyncClient> mcpSyncClients;
    private final ChatClient mcpChatClient;
    
    // 각 사용자별 대화 기록을 저장할 맵 (UUID -> 대화 기록)
    private final Map<String, List<Message>> sessionConversations = new ConcurrentHashMap<>();
    
    // 공유 디렉토리 기본 경로 설정
    private final String baseDataDir = "C:/servers/shared_data";
    
    // 실제 서버에서는 "/data"로 마운트됨
    private final String mcpBaseDir = "/data";

    public WebInterfaceController(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpSyncClients) {
        this.chatClientBuilder = chatClientBuilder;
        this.mcpSyncClients = mcpSyncClients;
        
        // MCP 도구를 포함한 ChatClient 생성
        this.mcpChatClient = chatClientBuilder
                .defaultTools(new SyncMcpToolCallbackProvider(mcpSyncClients))
                .build();
    }
    
    /**
     * 사용자 세션 UUID 확인 또는 생성
     */
    private String ensureSessionId(HttpSession session) {
        String sessionId = (String) session.getAttribute("SESSION_UUID");
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            session.setAttribute("SESSION_UUID", sessionId);
            
            // 사용자별 디렉토리 생성
            createUserDirectory(sessionId);
        }
        return sessionId;
    }

    /**
     * 사용자별 디렉토리 생성
     */
    private void createUserDirectory(String sessionId) {
        try {
            // 로컬 파일시스템에 사용자 디렉토리 생성
            Path userDir = Paths.get(baseDataDir, sessionId);
            if (!Files.exists(userDir)) {
                Files.createDirectories(userDir);
            }
        } catch (IOException e) {
            // 실제 프로덕션 코드에서는 더 나은 예외 처리 필요
            throw new RuntimeException("사용자 디렉토리 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 사용자별 대화 기록 가져오기
     */
    private List<Message> getConversationHistory(String sessionId) {
        return sessionConversations.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }

    /**
     * 사용자 파일 경로 변환 (로컬 -> MCP)
     */
    private String getMcpPath(String sessionId, String fileName) {
        return mcpBaseDir + "/" + sessionId + "/" + fileName;
    }

    /**
     * 사용자 파일 경로 변환 (로컬 파일시스템)
     */
    private Path getLocalPath(String sessionId, String fileName) {
        return Paths.get(baseDataDir, sessionId, fileName);
    }

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        String sessionId = ensureSessionId(session);
        
        model.addAttribute("pageTitle", "MCP 통합 인터페이스");
        model.addAttribute("conversations", getConversationHistory(sessionId));
        model.addAttribute("activeMenu", "home");
        model.addAttribute("sessionId", sessionId);
        return "index";
    }

    @GetMapping("/chat")
    public String chatPage(Model model, HttpSession session) {
        String sessionId = ensureSessionId(session);
        
        model.addAttribute("pageTitle", "AI 채팅");
        model.addAttribute("conversations", getConversationHistory(sessionId));
        model.addAttribute("activeMenu", "chat");
        model.addAttribute("sessionId", sessionId);
        return "chat";
    }
    
    @PostMapping("/chat/submit")
    public String submitChat(
            @RequestParam("userInput") String userInput,
            @RequestParam(value = "requestId", required = false) String requestId,
            HttpSession session,
            Model model) {
        
        String sessionId = ensureSessionId(session);
        List<Message> conversationHistory = getConversationHistory(sessionId);
        
        // 중복 요청 체크를 위한 속성 이름 생성
        String lastRequestAttr = "LAST_REQUEST_ID";
        String lastRequestId = (String) session.getAttribute(lastRequestAttr);
        
        // 중복 요청인 경우 그냥 반환
        if (requestId != null && requestId.equals(lastRequestId)) {
            model.addAttribute("conversations", conversationHistory);
            model.addAttribute("activeMenu", "chat");
            model.addAttribute("sessionId", sessionId);
            return "chat";
        }
        
        // 현재 요청 ID 저장
        if (requestId != null) {
            session.setAttribute(lastRequestAttr, requestId);
        }
        
        // 사용자 입력을 대화 기록에 추가
        UserMessage userMessage = new UserMessage(userInput);
        conversationHistory.add(userMessage);
        
        // MCP 도구를 포함한 시스템 프롬프트 (사용자별 경로 포함)
        String systemPrompt = String.format("""
        당신은 MCP(Model Context Protocol) 도구를 활용하는 AI 어시스턴트입니다.
        파일 시스템 및 Excel 작업을 수행할 수 있으며, 사용자의 요청에 따라 적절한 도구를 사용하세요.
        
        도구를 활용하여:
        1. 파일 읽기(read_file): 파일 내용을 읽을 수 있습니다.
        2. 파일 쓰기(write_file): 새 파일을 생성하거나 기존 파일을 수정할 수 있습니다.
        3. 디렉토리 목록(list_directory): 디렉토리 내용을 나열할 수 있습니다.
        4. Excel 시트 분석: Excel 파일의 데이터를 분석할 수 있습니다.
        
        모든 파일 경로는 %s 디렉토리 내에서 접근 가능합니다.
        """, mcpBaseDir + "/" + sessionId);

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
        model.addAttribute("sessionId", sessionId);
        return "chat";
    }

    @GetMapping("/document")
    public String documentPage(Model model, HttpSession session) {
        String sessionId = ensureSessionId(session);
        
        model.addAttribute("pageTitle", "문서 처리");
        model.addAttribute("activeMenu", "document");
        model.addAttribute("sessionId", sessionId);
        return "document";
    }

    @PostMapping("/document/upload")
    public String uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "requestId", required = false) String requestId,
            HttpSession session,
            Model model) {
        
        if (file.isEmpty()) {
            model.addAttribute("message", "파일을 선택해주세요.");
            model.addAttribute("activeMenu", "document");
            return "document";
        }

        String sessionId = ensureSessionId(session);
        
        // 중복 요청 체크를 위한 속성 이름 생성
        String lastRequestAttr = "LAST_DOCUMENT_REQUEST_ID";
        String lastRequestId = (String) session.getAttribute(lastRequestAttr);
        
        // 중복 요청인 경우 그냥 반환
        if (requestId != null && requestId.equals(lastRequestId)) {
            model.addAttribute("activeMenu", "document");
            model.addAttribute("sessionId", sessionId);
            return "document";
        }
        
        // 현재 요청 ID 저장
        if (requestId != null) {
            session.setAttribute(lastRequestAttr, requestId);
        }
        
        try {
            // 사용자별 디렉토리에 파일 저장
            String fileName = file.getOriginalFilename();
            Path filePath = getLocalPath(sessionId, fileName);
            
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // MCP 경로 계산
            String mcpFilePath = getMcpPath(sessionId, fileName);
            
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
            String userPrompt = String.format("다음 파일을 분석해주세요: %s\n전반적인 분석 결과와 인사이트를 제공해주세요.", mcpFilePath);
            
            // AI 응답 생성
            String aiResponse = mcpChatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
            
            model.addAttribute("fileName", fileName);
            model.addAttribute("filePath", mcpFilePath);
            model.addAttribute("analysis", aiResponse);
            model.addAttribute("activeMenu", "document");
            model.addAttribute("sessionId", sessionId);
            
            return "document-analysis";
            
        } catch (IOException e) {
            model.addAttribute("message", "파일 업로드 중 오류가 발생했습니다: " + e.getMessage());
            model.addAttribute("activeMenu", "document");
            model.addAttribute("sessionId", sessionId);
            return "document";
        }
    }

    @GetMapping("/excel")
    public String excelPage(Model model, HttpSession session) {
        String sessionId = ensureSessionId(session);
        
        model.addAttribute("pageTitle", "Excel 처리");
        model.addAttribute("activeMenu", "excel");
        model.addAttribute("sessionId", sessionId);
        return "excel";
    }

    @PostMapping("/excel/analyze")
    public String analyzeExcel(
            @RequestParam("filePath") String filePath, 
            @RequestParam(value = "question", required = false) String question,
            @RequestParam(value = "requestId", required = false) String requestId,
            HttpSession session,
            Model model) {
        
        String sessionId = ensureSessionId(session);
        
        // 중복 요청 체크를 위한 속성 이름 생성
        String lastRequestAttr = "LAST_EXCEL_REQUEST_ID";
        String lastRequestId = (String) session.getAttribute(lastRequestAttr);
        
        // 중복 요청인 경우 그냥 반환
        if (requestId != null && requestId.equals(lastRequestId)) {
            model.addAttribute("activeMenu", "excel");
            model.addAttribute("sessionId", sessionId);
            return "excel";
        }
        
        // 현재 요청 ID 저장
        if (requestId != null) {
            session.setAttribute(lastRequestAttr, requestId);
        }
        
        // 사용자 경로 활용을 위한 파일경로 조정 (만약 사용자가 전체 경로를 입력했을 경우 처리)
        if (!filePath.contains(sessionId) && filePath.startsWith("/data/")) {
            // 파일 경로에 사용자 ID가 없는 경우 (기존 방식의 경로인 경우)
            // 파일 이름만 추출
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            // 사용자 경로로 변환
            filePath = getMcpPath(sessionId, fileName);
        }

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
        model.addAttribute("sessionId", sessionId);
        
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
