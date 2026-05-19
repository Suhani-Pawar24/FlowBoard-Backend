package com.flowboard.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.List;
import java.util.Map;

@FeignClient(name = "board-service")
public interface BoardServiceClient {

    @GetMapping("/boards")
    List<?> getBoards();

    @GetMapping("/boards/all")
    List<Map<String, Object>> getAllBoards(@RequestHeader("X-user-role") String role);
}
