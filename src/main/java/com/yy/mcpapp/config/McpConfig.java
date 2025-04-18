package com.yy.mcpapp.config;

import io.modelcontextprotocol.client.McpSyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpConfig {
    private static final Logger logger = LoggerFactory.getLogger(McpConfig.class);
    
    @Bean
    public McpSyncClientCustomizer mcpSyncClientCustomizer() {
        return (name, spec) -> {
            logger.info("Configuring MCP client: {}", name);
            
            // 모든 MCP 클라이언트 공통 설정
            spec.loggingConsumer(log -> {
                logger.info("MCP Log [{}] from {}: {}", log.level(), name, log.logger());
            });
            
            // Excel MCP 클라이언트 특정 설정
            if (name.contains("excel")) {
                logger.info("Applying Excel-specific configurations for {}", name);
                
                // Excel MCP 클라이언트에 대한 특정 설정
                spec.toolsChangeConsumer(tools -> {
                    logger.info("Excel tools available: {}", tools.size());
                    tools.forEach(tool -> logger.info("Excel tool: {}", tool.description()));
                });
            }
        };
    }
    
    @Bean
    public void logAvailableMcpClients(List<McpSyncClient> mcpSyncClients) {
        logger.info("Available MCP clients: {}", mcpSyncClients.size());
        mcpSyncClients.forEach(client -> {
            logger.info("MCP client: {}", client.getClientInfo().name());
            client.listTools().tools().forEach(tool -> {
                logger.info("  Tool: {}", tool.description());
            });
        });
    }
}
