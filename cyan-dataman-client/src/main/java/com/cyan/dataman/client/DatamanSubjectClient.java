package com.cyan.dataman.client;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.client.dto.SubjectTreeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 主题域 Feign Client
 *
 * @author cy.Y
 * @since 1.1.0
 */
@FeignClient(name = "cyan-dataman", path = "/api/v1/metadata/subjects")
public interface DatamanSubjectClient {

    /**
     * 获取主题树
     */
    @GetMapping("/tree")
    Response<List<SubjectTreeDTO>> treeSubjects(); // API: ready
}
