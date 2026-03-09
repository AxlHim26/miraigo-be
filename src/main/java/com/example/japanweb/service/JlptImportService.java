package com.example.japanweb.service;

import com.example.japanweb.dto.request.jlpt.JlptImportRequest;
import com.example.japanweb.dto.request.jlpt.JlptParsedExamImportRequest;
import com.example.japanweb.dto.response.jlpt.JlptImportResultDTO;
import com.example.japanweb.dto.response.jlpt.JlptParsedExamImportResultDTO;

public interface JlptImportService {

    JlptImportResultDTO importFromManifest(JlptImportRequest request);

    JlptParsedExamImportResultDTO importParsedExam(JlptParsedExamImportRequest request);
}
