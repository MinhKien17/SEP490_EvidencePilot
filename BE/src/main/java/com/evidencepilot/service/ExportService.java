package com.evidencepilot.service;

import com.evidencepilot.model.ExportJob;
import java.util.List;
import java.util.UUID;

public interface ExportService {
    ExportJob createExportJob(UUID projectId, String format);
    ExportJob getJob(UUID jobId);
    byte[] downloadExport(UUID jobId);
    List<ExportJob> getUserExports(UUID projectId);
}
