package com.flowboard.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "workspace-service")
public interface WorkspaceServiceClient {

    @PutMapping("/workspaces/{workspaceId}/tier")
    void updateWorkspaceTier(@PathVariable("workspaceId") Long workspaceId, @RequestParam("tier") String tier);
}
