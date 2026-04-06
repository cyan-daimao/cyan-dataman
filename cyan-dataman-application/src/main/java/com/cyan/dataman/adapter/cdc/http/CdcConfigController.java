package com.cyan.dataman.adapter.cdc.http;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.adapter.cdc.http.convert.CdcAdapterConvert;
import com.cyan.dataman.adapter.cdc.http.dto.CdcConfigDTO;
import com.cyan.dataman.adapter.cdc.http.dto.CdcSparkJobDTO;
import com.cyan.dataman.adapter.cdc.http.dto.CdcSparkTaskDTO;
import com.cyan.dataman.application.cdc.CdcConfigService;
import com.cyan.dataman.application.cdc.bo.CdcConfigBO;
import com.cyan.dataman.application.cdc.bo.CdcSparkJobBO;
import com.cyan.dataman.application.cdc.bo.CdcSparkTaskBO;
import com.cyan.dataman.application.cdc.cmd.CdcConfigCmd;
import com.cyan.dataman.application.cdc.cmd.CdcSparkJobCmd;
import com.cyan.dataman.domain.cdc.query.CdcConfigListQuery;
import com.cyan.employee.login.filter.UserContextHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * CDC 配置控制器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/cdc")
public class CdcConfigController {

    private final CdcConfigService cdcConfigService;

    public CdcConfigController(CdcConfigService cdcConfigService) {
        this.cdcConfigService = cdcConfigService;
    }

    // ==================== CDC 配置管理 ====================

    /**
     * 添加 CDC 配置
     */
    @PostMapping
    public Response<CdcConfigDTO> create(@RequestBody @Valid CdcConfigCmd cmd) {
        cmd.setCreateBy(UserContextHolder.getCurrentEmployee().getPassport());
        cmd.setUpdateBy(UserContextHolder.getCurrentEmployee().getPassport());
        CdcConfigBO bo = cdcConfigService.create(cmd);
        return Response.success(CdcAdapterConvert.INSTANCE.toCdcConfigDTO(bo));
    }

    /**
     * 获取 CDC 列表
     */
    @GetMapping
    public Response<List<CdcConfigDTO>> list(CdcConfigListQuery query) {
        List<CdcConfigBO> list = cdcConfigService.list(query);
        List<CdcConfigDTO> dtos = Optional.ofNullable(list).orElse(List.of()).stream()
                .map(CdcAdapterConvert.INSTANCE::toCdcConfigDTO)
                .toList();
        return Response.success(dtos);
    }

    /**
     * 获得 CDC 信息
     */
    @GetMapping("/{cdcName}")
    public Response<CdcConfigDTO> findById(@PathVariable("cdcName") String cdcName) {
        CdcConfigBO bo;
        // 尝试按 ID 或名称查找
        try {
            bo = cdcConfigService.findById(cdcName);
        } catch (Exception e) {
            bo = cdcConfigService.findByName(cdcName);
        }
        return Response.success(CdcAdapterConvert.INSTANCE.toCdcConfigDTO(bo));
    }

    /**
     * 修改 CDC 配置
     */
    @PutMapping("/{cdcName}")
    public Response<CdcConfigDTO> update(@PathVariable("cdcName") String cdcName,
                                         @RequestBody @Valid CdcConfigCmd cmd) {
        cmd.setUpdateBy(UserContextHolder.getCurrentEmployee().getPassport());
        // 先按名称查找 ID
        CdcConfigBO existing = cdcConfigService.findByName(cdcName);
        CdcConfigBO bo = cdcConfigService.update(existing.getId(), cmd);
        return Response.success(CdcAdapterConvert.INSTANCE.toCdcConfigDTO(bo));
    }

    /**
     * CDC 开启和关闭
     */
    @PutMapping("/{cdcName}/open")
    public Response<Void> toggle(@PathVariable("cdcName") String cdcName,
                                 @RequestParam Boolean enabled) {
        CdcConfigBO existing = cdcConfigService.findByName(cdcName);
        cdcConfigService.toggle(existing.getId(), enabled);
        return Response.success();
    }

    /**
     * 删除 CDC
     */
    @DeleteMapping("/{cdcName}")
    public Response<Void> delete(@PathVariable("cdcName") String cdcName) {
        CdcConfigBO existing = cdcConfigService.findByName(cdcName);
        cdcConfigService.delete(existing.getId());
        return Response.success();
    }

    // ==================== Spark 作业配置管理 ====================

    /**
     * 创建 Spark 作业配置
     */
    @PostMapping("/{cdcName}/spark-jobs")
    public Response<CdcSparkJobDTO> createSparkJob(@PathVariable("cdcName") String cdcName,
                                                   @RequestBody @Valid CdcSparkJobCmd cmd) {
        // 先按名称查找 CDC 配置
        CdcConfigBO cdc = cdcConfigService.findByName(cdcName);
        cmd.setCdcConfigId(cdc.getId());
        cmd.setCreateBy(UserContextHolder.getCurrentEmployee().getPassport());
        cmd.setUpdateBy(UserContextHolder.getCurrentEmployee().getPassport());
        CdcSparkJobBO bo = cdcConfigService.createSparkJob(cmd);
        return Response.success(CdcAdapterConvert.INSTANCE.toCdcSparkJobDTO(bo));
    }

    /**
     * 获取 Spark 作业配置列表
     */
    @GetMapping("/{cdcName}/spark-jobs")
    public Response<List<CdcSparkJobDTO>> getSparkJobs(@PathVariable("cdcName") String cdcName) {
        CdcConfigBO cdc = cdcConfigService.findByName(cdcName);
        List<CdcSparkJobBO> list = cdcConfigService.getSparkJobsByCdcConfigId(cdc.getId());
        List<CdcSparkJobDTO> dtos = Optional.ofNullable(list).orElse(List.of()).stream()
                .map(CdcAdapterConvert.INSTANCE::toCdcSparkJobDTO)
                .toList();
        return Response.success(dtos);
    }

    /**
     * 更新 Spark 作业配置
     */
    @PutMapping("/spark-jobs/{jobId}")
    public Response<CdcSparkJobDTO> updateSparkJob(@PathVariable("jobId") String jobId,
                                                   @RequestBody @Valid CdcSparkJobCmd cmd) {
        cmd.setUpdateBy(UserContextHolder.getCurrentEmployee().getPassport());
        CdcSparkJobBO bo = cdcConfigService.updateSparkJob(jobId, cmd);
        return Response.success(CdcAdapterConvert.INSTANCE.toCdcSparkJobDTO(bo));
    }

    /**
     * 删除 Spark 作业配置
     */
    @DeleteMapping("/spark-jobs/{jobId}")
    public Response<Void> deleteSparkJob(@PathVariable("jobId") String jobId) {
        cdcConfigService.deleteSparkJob(jobId);
        return Response.success();
    }

    // ==================== Spark 任务实例管理 ====================

    /**
     * 获取任务实例列表
     */
    @GetMapping("/{cdcName}/tasks")
    public Response<List<CdcSparkTaskDTO>> getTaskInstances(@PathVariable("cdcName") String cdcName) {
        CdcConfigBO cdc = cdcConfigService.findByName(cdcName);
        List<CdcSparkTaskBO> list = cdcConfigService.getTaskInstances(cdc.getId());
        List<CdcSparkTaskDTO> dtos = Optional.ofNullable(list).orElse(List.of()).stream()
                .map(CdcAdapterConvert.INSTANCE::toCdcSparkTaskDTO)
                .toList();
        return Response.success(dtos);
    }

    /**
     * 获取任务实例详情
     */
    @GetMapping("/tasks/{taskId}")
    public Response<CdcSparkTaskDTO> getTaskInstance(@PathVariable("taskId") String taskId) {
        CdcSparkTaskBO bo = cdcConfigService.getTaskInstance(taskId);
        return Response.success(CdcAdapterConvert.INSTANCE.toCdcSparkTaskDTO(bo));
    }

    /**
     * 停止运行中的任务
     */
    @PostMapping("/tasks/{taskId}/stop")
    public Response<Void> stopTask(@PathVariable("taskId") String taskId) {
        cdcConfigService.stopTask(taskId);
        return Response.success();
    }
}
