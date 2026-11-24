package com.example.japanweb.controller;

import com.example.japanweb.dto.common.ApiResponse;
import com.example.japanweb.service.StudyProviderClient;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/practice")
public class PracticeToolsController {
    private final StudyProviderClient client;
    private final URI engine;

    public PracticeToolsController(StudyProviderClient client, @Value("${PRACTICE_ENGINE_URL:}") String baseUrl) {
        this.client = client;
        engine = baseUrl.isBlank() ? null : URI.create(baseUrl.strip());
        if (engine != null && (engine.getHost() == null || engine.getUserInfo() != null
                || !List.of("http", "https").contains(engine.getScheme()))) {
            throw new IllegalArgumentException("PRACTICE_ENGINE_URL must be an HTTP(S) origin");
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReadingInput(@NotBlank @Pattern(regexp = "N[1-5]") String proficiencyLevel,
                               @Pattern(regexp = "daily|family|school|travel") String topic,
                               @Pattern(regexp = "short|medium|long") String length,
                               @Min(3) @Max(6) Integer questionCount) {}
    public record GrammarInput(@NotBlank @Pattern(regexp = "N[1-5]") String level,
                               @NotBlank @Size(max = 120) String topic,
                               @NotBlank @Size(max = 250) String structure,
                               @NotBlank @Pattern(regexp = "choice|order|particle") String type,
                               @NotNull @Min(5) @Max(20) Integer questionCount) {}
    public record AnnotateInput(@NotEmpty @Size(max = 30) List<@NotBlank @Size(max = 1000) String> lines) {}
    public record WordInput(@NotBlank @Size(max = 30) String word, @NotBlank @Size(max = 400) String context) {}
    public record ChatSettings(@NotBlank @Pattern(regexp = "N[1-5]") String proficiencyLevel,
                               @Min(70) @Max(130) int speechRate,
                               @Pattern(regexp = "intro|shop|direction|restaurant") String scenario,
                               Boolean includeTranslation) {}
    public record CorrectionInput(@NotBlank @Size(max = 450) String message,
                                  @NotBlank @Pattern(regexp = "N[1-5]") String level) {}
    public record ChatTurn(@NotBlank @Pattern(regexp = "user|assistant") String role,
                           @NotBlank @Size(max = 3000) String content) {}
    public record ChatInput(@NotBlank @Size(max = 1500) String message, @NotNull @Valid ChatSettings settings,
                            @Size(max = 12) List<@NotNull @Valid ChatTurn> history) {}

    @PostMapping("/reading/generate")
    public ApiResponse<JsonNode> generate(@Valid @RequestBody ReadingInput input) {
        return ApiResponse.success(client.json(endpoint("/api/practice/reading/generate"), input));
    }

    @PostMapping("/grammar/generate")
    public ApiResponse<JsonNode> generateGrammar(@Valid @RequestBody GrammarInput input) {
        return ApiResponse.success(client.json(endpoint("/api/practice/grammar/generate"), input));
    }

    @PostMapping("/reading/annotate")
    public ApiResponse<JsonNode> annotate(@Valid @RequestBody AnnotateInput input) {
        return ApiResponse.success(client.json(endpoint("/api/practice/reading/annotate"), input));
    }

    @PostMapping("/reading/word")
    public ApiResponse<JsonNode> word(@Valid @RequestBody WordInput input) {
        return ApiResponse.success(client.json(endpoint("/api/practice/reading/word"), input));
    }

    @PostMapping("/chat/correct")
    public ApiResponse<JsonNode> correctChat(@Valid @RequestBody CorrectionInput input) {
        return ApiResponse.success(client.json(endpoint("/api/practice/chat/correct"), input));
    }

    @PostMapping("/chat")
    public ResponseEntity<StreamingResponseBody> chat(@Valid @RequestBody ChatInput input) {
        var source = client.open(endpoint("/api/practice/realtime"),
            new ChatInput(input.message(), input.settings(), input.history() == null ? List.of() : input.history()),
            MediaType.TEXT_EVENT_STREAM_VALUE);
        StreamingResponseBody body = output -> {
            try (source) {
                byte[] buffer = new byte[2048];
                int remaining = 256 * 1024;
                int read;
                while ((read = source.read(buffer)) != -1) {
                    remaining -= read;
                    if (remaining < 0) throw new IOException("Chat response exceeded size limit");
                    output.write(buffer, 0, read);
                    output.flush();
                }
            }
        };
        return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM)
            .header("Cache-Control", "no-cache, no-transform")
            .header("X-Accel-Buffering", "no").body(body);
    }

    private URI endpoint(String path) {
        if (engine == null) throw StudyProviderClient.unavailable();
        return engine.resolve(path);
    }
}
