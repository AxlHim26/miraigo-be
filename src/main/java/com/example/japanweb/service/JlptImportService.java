package com.example.japanweb.service;

import com.example.japanweb.dto.request.jlpt.JlptImportRequest;
import com.example.japanweb.dto.response.jlpt.JlptImportResultDTO;

public interface JlptImportService {

    JlptImportResultDTO importFromManifest(JlptImportRequest request);
}
