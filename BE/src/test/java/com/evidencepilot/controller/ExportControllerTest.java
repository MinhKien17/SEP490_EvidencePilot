package com.evidencepilot.controller;

import com.evidencepilot.service.ExportService;
import com.evidencepilot.service.ClaimContentConsistencyService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ExportControllerTest {

    @Test
    void downloadStreamsOriginalArchiveWithAttachmentFilename() throws Exception {
        ExportService service = mock(ExportService.class);
        MockMvc mockMvc = standaloneSetup(new ExportController(
                service, mock(ClaimContentConsistencyService.class))).build();
        UUID jobId = UUID.randomUUID();
        byte[] archive = {1, 2, 3, 4};
        when(service.downloadExport(jobId))
                .thenReturn(new InputStreamResource(new ByteArrayInputStream(archive)));

        mockMvc.perform(get("/api/exports/{jobId}/download", jobId))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"export-" + jobId + ".zip\""))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(archive));
    }
}
