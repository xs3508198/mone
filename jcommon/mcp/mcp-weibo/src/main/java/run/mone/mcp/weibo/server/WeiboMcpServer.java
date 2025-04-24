package run.mone.mcp.weibo.server;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import org.springframework.stereotype.Component;
import run.mone.hive.mcp.server.McpServer;
import run.mone.hive.mcp.server.McpSyncServer;
import run.mone.hive.mcp.spec.McpSchema;
import run.mone.hive.mcp.spec.ServerMcpTransport;
import run.mone.mcp.weibo.function.WeiboFunction;

@Component
public class WeiboMcpServer {
    private ServerMcpTransport transport;
    private McpSyncServer syncServer;

    public WeiboMcpServer(ServerMcpTransport transport) {
        this.transport = transport;
    }

    public McpSyncServer start() {
        McpSyncServer syncServer = McpServer.using(transport)
                .serverInfo("weibo_mcp", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .logging()
                        .build())
                .sync();

        WeiboFunction function = new WeiboFunction();
        var toolRegistration = new McpServer.ToolRegistration(
                new McpSchema.Tool(function.getName(), function.getDesc(), function.getToolScheme()), function
        );

        syncServer.addTool(toolRegistration);

        return syncServer;
    }

    @PostConstruct
    public void init() {
        this.syncServer = start();
    }

    @PreDestroy
    public void stop() {
        if (this.syncServer != null) {
            this.syncServer.closeGracefully();
        }
    }
}
