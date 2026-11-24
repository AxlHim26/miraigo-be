package com.example.japanweb.controller;

import com.example.japanweb.dto.common.ApiResponse;
import com.example.japanweb.entity.User;
import com.example.japanweb.service.VocabularySetService;
import com.example.japanweb.service.VocabularySetService.SetInput;
import com.example.japanweb.service.VocabularySetService.SetView;
import com.example.japanweb.service.VocabularySetService.WordInput;
import com.example.japanweb.service.VocabularySetService.WordView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vocab/sets")
@RequiredArgsConstructor
public class VocabularySetController {
    private final VocabularySetService service;

    @GetMapping
    public ApiResponse<List<SetView>> list(@AuthenticationPrincipal User user,
                                          @RequestParam(defaultValue = "false") boolean community) {
        return ApiResponse.success(service.list(user.getId(), community));
    }

    @GetMapping("/{id}")
    public ApiResponse<SetView> detail(@AuthenticationPrincipal User user, @PathVariable String id) {
        return ApiResponse.success(service.detail(user.getId(), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SetView> create(@AuthenticationPrincipal User user, @Valid @RequestBody SetInput input) {
        return ApiResponse.created(service.create(user.getId(), input));
    }

    @PutMapping("/{id}")
    public ApiResponse<SetView> update(@AuthenticationPrincipal User user, @PathVariable String id,
                                      @Valid @RequestBody SetInput input) {
        return ApiResponse.success(service.update(user.getId(), id, input));
    }

    @PostMapping("/{id}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SetView> copy(@AuthenticationPrincipal User user, @PathVariable String id) {
        return ApiResponse.created(service.copy(user.getId(), id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User user, @PathVariable String id) {
        service.delete(user.getId(), id);
    }

    @GetMapping("/{id}/words")
    public ApiResponse<List<WordView>> listWords(@AuthenticationPrincipal User user, @PathVariable String id) {
        return ApiResponse.success(service.listWords(user.getId(), id));
    }

    @PostMapping("/{id}/words")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WordView> createWord(@AuthenticationPrincipal User user, @PathVariable String id,
                                            @Valid @RequestBody WordInput input) {
        return ApiResponse.created(service.createWord(user.getId(), id, input));
    }

    @PutMapping("/{id}/words/{wordId}")
    public ApiResponse<WordView> updateWord(@AuthenticationPrincipal User user, @PathVariable String id,
                                            @PathVariable String wordId, @Valid @RequestBody WordInput input) {
        return ApiResponse.success(service.updateWord(user.getId(), id, wordId, input));
    }

    @DeleteMapping("/{id}/words/{wordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWord(@AuthenticationPrincipal User user, @PathVariable String id, @PathVariable String wordId) {
        service.deleteWord(user.getId(), id, wordId);
    }
}
