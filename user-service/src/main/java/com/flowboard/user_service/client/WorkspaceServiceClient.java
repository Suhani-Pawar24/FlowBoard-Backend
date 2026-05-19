package com.flowboard.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@FeignClient(name = "workspace-service")
public interface WorkspaceServiceClient {

    @GetMapping("/workspaces")
    List<?> getWorkspaces();
}
