package com.cyan.dataman.adapter.metadata.http.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataman.adapter.metadata.http.dto.AiRelationSuggestionDTO;
import com.cyan.dataman.application.metadata.bo.AiRelationSuggestionBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * AI 关联推荐转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface AiRelationSuggestAdapterConvert {
    AiRelationSuggestAdapterConvert INSTANCE = Mappers.getMapper(AiRelationSuggestAdapterConvert.class);

    /**
     * 转换推荐列表
     */
    List<AiRelationSuggestionDTO> toAiRelationSuggestionDTOList(List<AiRelationSuggestionBO> suggestions);
}
