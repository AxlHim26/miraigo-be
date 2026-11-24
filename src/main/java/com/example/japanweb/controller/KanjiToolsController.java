package com.example.japanweb.controller;

import com.example.japanweb.dto.common.ApiResponse;
import com.example.japanweb.exception.ApiException;
import com.example.japanweb.exception.ErrorCode;
import com.example.japanweb.service.StudyProviderClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/kanji")
public class KanjiToolsController {
    private static final URI HANDWRITING_ENGINE = URI.create(
        "https://www.google.com/inputtools/request?ime=handwriting&app=mobilesearch&cs=1&oe=UTF-8"
    );
    private final Map<String, Entry> catalog;
    private final StudyProviderClient client;

    public KanjiToolsController(ObjectMapper mapper, StudyProviderClient client) throws IOException {
        this.client = client;
        try (var input = new ClassPathResource("kanji/map.json").getInputStream()) {
            catalog = mapper.readValue(input, new TypeReference<Map<String, Entry>>() {});
        }
    }

    public record Entry(String radical, List<String> parts, String strokeOrderGif) {}
    public record Relation(String glyph, boolean sameRadical, List<String> sharedParts) {}
    public record KanjiMap(String glyph, String radical, List<String> parts, String strokeOrderGif,
                           List<Relation> related) {}

    @GetMapping("/{glyph}/map")
    public ApiResponse<KanjiMap> map(@PathVariable String glyph) {
        var entry = catalog.get(glyph);
        if (entry == null) throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Kanji not found");

        var related = catalog.entrySet().stream()
            .filter(item -> !glyph.equals(item.getKey()))
            .map(item -> {
                var other = item.getValue();
                var shared = entry.parts().stream().distinct().filter(other.parts()::contains).toList();
                boolean sameRadical = entry.radical() != null && Objects.equals(entry.radical(), other.radical());
                return new Relation(item.getKey(), sameRadical, shared);
            })
            .filter(item -> item.sameRadical() || !item.sharedParts().isEmpty())
            .sorted(Comparator.comparing(Relation::sameRadical).reversed()
                .thenComparing(Comparator.comparingInt((Relation item) -> item.sharedParts().size()).reversed())
                .thenComparing(Relation::glyph))
            .limit(12).toList();

        return ApiResponse.success(new KanjiMap(glyph, entry.radical(), entry.parts(), entry.strokeOrderGif(), related));
    }

    public record Point(@Min(0) @Max(300) int x, @Min(0) @Max(300) int y) {}
    public record InkInput(@NotEmpty @Size(max = 40) List<
        @NotEmpty @Size(max = 128) List<@NotNull @Valid Point>> strokes) {}

    @PostMapping("/handwriting")
    public ApiResponse<List<String>> recognize(@Valid @RequestBody InkInput input) {
        List<List<?>> ink = new ArrayList<>();
        for (var stroke : input.strokes()) {
            ink.add(List.of(stroke.stream().map(Point::x).toList(),
                            stroke.stream().map(Point::y).toList(), List.of()));
        }
        var response = client.json(HANDWRITING_ENGINE, Map.of(
            "options", "enable_pre_space",
            "requests", List.of(Map.of(
                "language", "ja",
                "writing_guide", Map.of("writing_area_width", 300, "writing_area_height", 300),
                "ink", ink
            ))
        ));
        if (!"SUCCESS".equals(response.path(0).asText())) throw StudyProviderClient.unavailable();
        List<String> candidates = new ArrayList<>();
        for (var option : response.path(1).path(0).path(1)) {
            String glyph = option.asText();
            if (glyph.codePointCount(0, glyph.length()) == 1
                    && Character.UnicodeScript.of(glyph.codePointAt(0)) == Character.UnicodeScript.HAN
                    && !candidates.contains(glyph)) {
                candidates.add(glyph);
                if (candidates.size() == 10) break;
            }
        }
        return ApiResponse.success(candidates);
    }
}
