package com.example.shortvideo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagWithSynonymsDTO {
    private Long id;
    private String name;
    private Boolean isCanonical;
    private List<SynonymTagDTO> synonyms;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SynonymTagDTO {
        private Long id;
        private String name;
    }
}
