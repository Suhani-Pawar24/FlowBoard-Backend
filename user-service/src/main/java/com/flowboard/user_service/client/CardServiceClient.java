package com.flowboard.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import java.util.Map;

@FeignClient(name = "card-service")
public interface CardServiceClient {

    @GetMapping("/cards/overdue")
    List<Map<String, Object>> getOverdueCards();
}
