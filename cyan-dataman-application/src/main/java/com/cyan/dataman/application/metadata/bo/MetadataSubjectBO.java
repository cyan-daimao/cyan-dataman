package com.cyan.dataman.application.metadata.bo;

import com.cyan.dataman.enums.OpenStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 元数据主题业务对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class MetadataSubjectBO {
    /**
     * 主键
     */
    private String id;

    /**
     * 主题编码
     */
    private String subjectCode;

    /**
     * 主题名称
     */
    private String subjectName;

    /**
     * 描述
     */
    private String subjectDesc;

    /**
     * 父级主题id
     */
    private String parentId;

    /**
     * 层级：1 一级、2 二级、3 三级
     */
    private int level;

    /**
     * 主题负责人
     */
    private String owner;

    /**
     * 开启状态
     */
    private OpenStatus openStatus;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 修改人
     */
    private String updateBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除
     */
    private LocalDateTime deletedAt;

    /**
     * 子主题
     */
    private List<MetadataSubjectBO> children;

    /**
     * 将扁平的MetadataSubjectBO列表转换为树形结构
     *
     * @param flatList 扁平的主题列表
     * @return 树形结构的根节点列表（一级主题）
     */
    public static List<MetadataSubjectBO> buildTree(List<MetadataSubjectBO> flatList) {
        // 1. 参数校验
        if (flatList == null || flatList.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 将列表转换为Map（key: id, value: MetadataSubjectBO），方便快速查找
        Map<String, MetadataSubjectBO> idToSubjectMap = flatList.stream()
                .collect(Collectors.toMap(
                        MetadataSubjectBO::getId,  // 以id为key
                        subject -> subject,      // 以对象本身为value
                        (existing, replacement) -> existing  // 处理重复id，保留原有值
                ));

        // 3. 构建树形结构
        List<MetadataSubjectBO> rootNodes = new ArrayList<>();
        for (MetadataSubjectBO subject : flatList) {
            String parentId = subject.getParentId();

            // 4. 判断是否是一级节点（parentId为空/空字符串/0 都视为一级）
            if (parentId == null || parentId.isEmpty() || "0".equals(parentId)) {
                rootNodes.add(subject);
            } else {
                // 5. 找到父节点，将当前节点添加到父节点的children中
                MetadataSubjectBO parentSubject = idToSubjectMap.get(parentId);
                if (parentSubject != null) {
                    // 初始化children列表（避免空指针）
                    if (parentSubject.getChildren() == null) {
                        parentSubject.setChildren(new ArrayList<>());
                    }
                    parentSubject.getChildren().add(subject);
                }
            }
        }

        return rootNodes;
    }
}
