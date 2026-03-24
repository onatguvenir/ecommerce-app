package com.monat.ecommerce.inventory.api.controller;

import com.monat.ecommerce.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.File;
import java.io.IOException;

/**
 * REST Controller for Batch Inventory Operations.
 *
 * Educational Note:
 * This controller triggers Spring Batch Jobs for bulk operations such as
 * importing stock from CSV files. It decouples the REST request from
 * the long-running background process.
 */
@Slf4j
@RestController
@RequestMapping("/api/inventory/batch")
@RequiredArgsConstructor
@Tag(name = "Inventory Batch", description = "Operations for bulk inventory updates")
public class InventoryBatchController {

    private final JobLauncher jobLauncher;
    private final Job bulkStockUpdateJob;

    /**
     * Triggers an asynchronous Batch Job for bulk stock updates supported by RabbitMQ 
     * by uploading a CSV file.
     *
     * @param file The uploaded CSV file
     */
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<String>> importCsvToDBJob(@RequestParam(name = "file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("CSV file is empty"));
        }

        try {
            // Write the file to disk (temp) so the Spring Batch FlatFileItemReader can read it.
            String tempFilePath = System.getProperty("java.io.tmpdir") + File.separator + file.getOriginalFilename();
            File tempFile = new File(tempFilePath);
            file.transferTo(tempFile);

            log.info("Triggering batch job. File: {}", tempFilePath);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("filePath", tempFilePath)
                    .addLong("startAt", System.currentTimeMillis())
                    .toJobParameters();

            // Job executes asynchronously, and the process begins via RabbitMQ
            jobLauncher.run(bulkStockUpdateJob, jobParameters);

            return ResponseEntity.ok(ApiResponse.success("Batch is triggered and processing started asynchronously behind RabbitMQ"));

        } catch (Exception e) {
            log.error("An error occurred while triggering the batch job", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to start batch job: " + e.getMessage()));
        }
    }
}
