package com.cyan.dataman.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "cyan-dataman", contextId = "datamanClient", path = "/rpc/v1/dataman")
public interface DatamanClient {
    @PostMapping
    String health();
}
