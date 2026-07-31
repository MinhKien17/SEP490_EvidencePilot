package com.evidencepilot.controller;

import com.evidencepilot.dto.response.SourceCategoryResponse;
import com.evidencepilot.service.SourceCategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SourceCategoryControllerTest {

    @Test
    void listsActiveSourceCategories() throws Exception {
        SourceCategoryService service = mock(SourceCategoryService.class);
        UUID id = UUID.randomUUID();
        when(service.getActiveCategories()).thenReturn(List.of(
                new SourceCategoryResponse(
                        id, "SE", "Software Engineering", null, true, LocalDateTime.now())));
        MockMvc mockMvc = standaloneSetup(new SourceCategoryController(service)).build();

        mockMvc.perform(get("/api/source-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("SE"));

        verify(service).getActiveCategories();
    }
}
