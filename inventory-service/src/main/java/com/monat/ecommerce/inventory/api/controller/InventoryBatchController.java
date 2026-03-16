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

import java.io.File;
import java.io.IOException;

/**
 * Spring Batch operasyonlarını tetikleyen REST Controller.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/inventory/batch")
@RequiredArgsConstructor
public class InventoryBatchController {

    private final JobLauncher jobLauncher;
    private final Job bulkStockUpdateJob;

    /**
     * CSV dosyası yüklenerek RabbitMQ destekli toplu stok güncelleme
     * asenkron Batch Job'ını tetikler.
     *
     * @param file Yüklenen CSV dosyası
     */
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<String>> importCsvToDBJob(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("CSV file is empty"));
        }

        try {
            // Spring Batch FlatFileItemReader'ın okuyabilmesi için dosyayı diske (temp) yazarız.
            String tempFilePath = System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename();
            File tempFile = new File(tempFilePath);
            file.transferTo(tempFile);

            log.info("Batch job tetikleniyor. Dosya: {}", tempFilePath);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("filePath", tempFilePath)
                    .addLong("startAt", System.currentTimeMillis())
                    .toJobParameters();

            // Job asenkron çalışır ve arkaplanda RabbitMQ üzerinden akış başlar
            jobLauncher.run(bulkStockUpdateJob, jobParameters);

            return ResponseEntity.ok(ApiResponse.success("Batch is triggered and processing started asynchronously behind RabbitMQ"));

        } catch (Exception e) {
            log.error("Batch tetikleme sırasında hata oluştu", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to start batch job: " + e.getMessage()));
        }
    }
}
