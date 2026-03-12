package com.cyan.dataman.infra.util;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.Response;
import com.cyan.arch.common.api.SilentException;
import com.cyan.employee.client.EmployeeClient;
import com.cyan.employee.client.dto.EmployeeDTO;
import com.cyan.employee.client.query.EmployeeRPCQuery;
import org.springframework.stereotype.Component;

/**
 * 员工工具类
 * @author cy.Y
 * @since 1.0.0
 */
@Component
public class EmployeeUtil {
    private final EmployeeClient employeeClient;

    public EmployeeUtil(EmployeeClient employeeClient) {
        this.employeeClient = employeeClient;
    }

    /**
     * 验证员工是否存在
     */
    public void validEmployee(String passport){
        Response<EmployeeDTO> resp = employeeClient.query(new EmployeeRPCQuery().setPassport(passport));
        Assert.notNull(resp.getData(), new SilentException("负责人%s不存在".formatted(passport)));
    }
}
