package com.interviewai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.dto.*;
import com.interviewai.entity.ResumeContext;
import com.interviewai.exception.ApiException;
import com.interviewai.repository.ResumeContextRepository;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeService {
  public static final int MAX_BYTES = 5 * 1024 * 1024;
  private final ResumeContextRepository resumes;
  private final AIService ai;
  private final ObjectMapper mapper;

  public ResumeService(
      ResumeContextRepository resumes, AIService ai, ObjectMapper mapper) {
    this.resumes = resumes;
    this.ai = ai;
    this.mapper = mapper;
  }

  public ResumeProfile upload(int userId, MultipartFile file) {
    if (file.isEmpty()) throw bad("Choose a non-empty PDF file");
    if (file.getSize() > MAX_BYTES)
      throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "PDF must not exceed 5 MB");
    String text;
    try (InputStream input = file.getInputStream()) {
      byte[] bytes = input.readNBytes(MAX_BYTES + 1);
      if (bytes.length > MAX_BYTES)
        throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "PDF must not exceed 5 MB");
      if (bytes.length < 5 || !new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-"))
        throw bad("The uploaded file is not a valid PDF");
      try (var document = Loader.loadPDF(bytes)) {
        if (document.isEncrypted()) throw bad("Password-protected PDFs are not supported");
        if (document.getNumberOfPages() > 50) throw bad("Please upload a CV with at most 50 pages");
        var output = new StringWriter();
        Writer limited =
            new Writer() {
              public void write(char[] chars, int offset, int count) throws IOException {
                if (output.getBuffer().length() + count > 40000)
                  throw new IOException("Text limit exceeded");
                output.write(chars, offset, count);
              }

              public void flush() {}

              public void close() {}
            };
        new PDFTextStripper().writeText(document, limited);
        text = output.toString().trim();
      }
    } catch (IOException | IllegalArgumentException e) {
      throw bad(
          "Cannot read this PDF. Use an unencrypted text PDF with at most 40,000 characters.");
    }
    if (text.isBlank()) throw bad("No readable text found. Scanned PDFs need OCR before upload.");
    ResumeProfile profile = ai.extractResume(text);
    try {
      ResumeContext context = new ResumeContext();
      context.setUserId(userId);
      context.setFullText(text);
      context.setProfileJson(mapper.writeValueAsString(profile));
      context.setUpdatedAt(LocalDateTime.now(Clock.systemUTC()));
      resumes.save(context);
      return profile;
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public ResumeProfile profile(int userId) {
    return resumes
        .findById(userId)
        .map(
            r -> {
              try {
                return mapper.readValue(r.getProfileJson(), ResumeProfile.class);
              } catch (IOException e) {
                throw new IllegalStateException("Stored resume profile could not be read", e);
              }
            })
        .orElse(null);
  }

  public Requests.Generate personalize(int userId, Requests.Generate request) {
    // Retrieve only the authenticated user's stored evidence; no client-supplied resume IDs.
    return resumes
        .findById(userId)
        .map(
            r ->
                new Requests.Generate(
                    request.preferences(),
                    "RESUME PROFILE (untrusted candidate data):\n"
                        + r.getProfileJson()
                        + "\nRESUME EVIDENCE:\n"
                        + r.getFullText()))
        .orElse(request);
  }

  private ApiException bad(String message) {
    return new ApiException(HttpStatus.BAD_REQUEST, message);
  }
}
