package com.tac.filestorage.controller;

import com.tac.filestorage.entities.ReceiptEntity;
import com.tac.filestorage.repos.ReceiptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")


public class ReceiptController {

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB limit
    private static final List<String> ALLOWED_FILE_TYPES = Arrays.asList(
            "application/pdf", "image/jpeg",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    @Autowired
    private ReceiptRepository receiptRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam("randomId") String randomId,
                                        @RequestParam("startDate") String startDate,
                                        @RequestParam("endDate") String endDate,
                                        @RequestParam("receiptDate") String receiptDate) {
        try {
            // Validate file size
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("File size exceeds the 2MB limit.");
            }

            // Validate file type
            String contentType = file.getContentType();
            if (!ALLOWED_FILE_TYPES.contains(contentType)) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Invalid file format. Only PDF, JPG, and Excel files are allowed.");
            }

            // Save receipt entity along with file data
            ReceiptEntity receiptEntity = new ReceiptEntity();
            receiptEntity.setRandomId(randomId);
            receiptEntity.setStartDate(startDate);
            receiptEntity.setEndDate(endDate);
            receiptEntity.setReceiptDate(receiptDate);
            receiptEntity.setFileName(file.getOriginalFilename());
            receiptEntity.setData(file.getBytes());

            receiptRepository.save(receiptEntity);

            return new ResponseEntity<>("File uploaded and data saved successfully!", HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while processing the file.");
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadFile(@PathVariable Long id) {
        Optional<ReceiptEntity> receiptEntityOptional = receiptRepository.findById(id);
        if (receiptEntityOptional.isPresent()) {
            ReceiptEntity receiptEntity = receiptEntityOptional.get();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.attachment().filename(receiptEntity.getFileName()).build());
            ByteArrayResource resource = new ByteArrayResource(receiptEntity.getData());

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(receiptEntity.getData().length)
                    .body(resource);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found.");
        }
    }


    @GetMapping("/files")
    public ResponseEntity<List<ReceiptEntity>>getFiles(){
        List<ReceiptEntity>files= receiptRepository.findAll();
                return ResponseEntity.ok(files);
    }


//    public ReceiptController(ReceiptRepository receiptRepository) {
//        this.receiptRepository = receiptRepository;
//    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadMultipleFiles(@RequestParam List<Long> ids) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);

            for (Long id : ids) {
                Optional<ReceiptEntity> optionalReceipt = receiptRepository.findById(id);
                if (optionalReceipt.isPresent()) {
                    ReceiptEntity receiptEntity = optionalReceipt.get();
                    ZipEntry zipEntry = new ZipEntry(receiptEntity.getFileName());
                    zipOutputStream.putNextEntry(zipEntry);
                    zipOutputStream.write(receiptEntity.getData());
                    zipOutputStream.closeEntry();
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File with ID " + id + " not found.");
                }
            }

            zipOutputStream.close();
            ByteArrayResource resource = new ByteArrayResource(byteArrayOutputStream.toByteArray());

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=files.zip");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while creating the ZIP file.");
        }
    }
}


