package com.cyan.dataman.domain.metadata.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 主体单查询
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Data
public class MetadataSubjectFindQuery {

    /**
     * 主体编码
     */
    private String subjectCode;


    /**
     * 主体名称
     */
    private String subjectName;
}
