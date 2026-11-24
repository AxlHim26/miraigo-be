package com.example.japanweb.service;

import com.example.japanweb.entity.User;
import com.example.japanweb.entity.VocabularySet;
import com.example.japanweb.entity.VocabularySetWord;
import com.example.japanweb.entity.VocabularySetWordId;
import com.example.japanweb.exception.ApiException;
import com.example.japanweb.exception.ErrorCode;
import com.example.japanweb.repository.VocabularySetRepository;
import com.example.japanweb.repository.VocabularySetWordRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VocabularySetService {
    private final VocabularySetRepository sets;
    private final VocabularySetWordRepository words;
    private final EntityManager entityManager;

    public record SetInput(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @Pattern(regexp = "N[1-5]") String level,
        Boolean isCommunity
    ) {}

    public record WordInput(
        @NotBlank @Size(max = 200) String japanese,
        @NotBlank @Size(max = 200) String reading,
        @NotBlank @Size(max = 1000) String meaning,
        @Size(max = 2000) String example,
        @Size(max = 2000) String exampleMeaning
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SetView(
        String id, String title, String description, String level, long wordCount,
        Instant updatedAt, boolean isCommunity, boolean canEdit
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record WordView(
        String id, String japanese, String reading, String meaning, String example, String exampleMeaning
    ) {}

    public List<SetView> list(Long userId, boolean community) {
        var visible = community ? sets.findByCommunityTrueOrderByUpdatedAtDescIdAsc()
            : sets.findByOwnerIdOrderByUpdatedAtDescIdAsc(userId);
        if (visible.isEmpty()) return List.of();

        var counts = words.countBySetIds(visible.stream().map(VocabularySet::getId).toList())
            .stream().collect(Collectors.toMap(
                VocabularySetWordRepository.WordCount::getSetId, VocabularySetWordRepository.WordCount::getTotal
            ));
        return visible.stream().map(set -> view(set, userId, counts.getOrDefault(set.getId(), 0L))).toList();
    }

    public SetView detail(Long userId, String id) {
        var set = sets.findById(id).orElseThrow(VocabularySetService::missingSet);
        requireReadable(set, userId);
        return view(set, userId, words.countByIdSetId(id));
    }

    @Transactional
    public List<WordView> listWords(Long userId, String setId) {
        var set = sets.lockForRead(setId).orElseThrow(VocabularySetService::missingSet);
        requireReadable(set, userId);
        return words.findByIdSetIdOrderByIdWordIdAsc(setId).stream().map(VocabularySetService::wordView).toList();
    }

    @Transactional
    public SetView create(Long userId, SetInput input) {
        var set = newSet(userId, input);
        entityManager.flush();
        return view(set, userId, 0);
    }

    @Transactional
    public SetView update(Long userId, String id, SetInput input) {
        var set = ownedSet(userId, id);
        applySet(set, input);
        entityManager.flush();
        return view(set, userId, words.countByIdSetId(id));
    }

    @Transactional
    public SetView copy(Long userId, String id) {
        var source = sets.lockForRead(id).orElseThrow(VocabularySetService::missingSet);
        requireReadable(source, userId);
        var copy = newSet(userId, new SetInput(source.getTitle(), source.getDescription(), source.getLevel(), false));
        var sourceWords = words.findByIdSetIdOrderByIdWordIdAsc(id);
        for (var word : sourceWords) {
            newWord(copy, new WordInput(
                word.getJapanese(), word.getReading(), word.getMeaning(), word.getExample(), word.getExampleMeaning()
            ));
        }
        entityManager.flush();
        return view(copy, userId, sourceWords.size());
    }

    @Transactional
    public void delete(Long userId, String id) {
        var set = ownedSet(userId, id);
        words.deleteForSet(id);
        sets.delete(set);
    }

    @Transactional
    public WordView createWord(Long userId, String setId, WordInput input) {
        var set = ownedSet(userId, setId);
        var word = newWord(set, input);
        set.setUpdatedAt(Instant.now());
        return wordView(word);
    }

    @Transactional
    public WordView updateWord(Long userId, String setId, String wordId, WordInput input) {
        var set = ownedSet(userId, setId);
        var word = findWord(setId, wordId);
        applyWord(word, input);
        set.setUpdatedAt(Instant.now());
        return wordView(word);
    }

    @Transactional
    public void deleteWord(Long userId, String setId, String wordId) {
        var set = ownedSet(userId, setId);
        words.delete(findWord(setId, wordId));
        set.setUpdatedAt(Instant.now());
    }

    private VocabularySet newSet(Long userId, SetInput input) {
        var set = new VocabularySet();
        set.setId(UUID.randomUUID().toString());
        set.setOwner(entityManager.getReference(User.class, userId));
        applySet(set, input);
        entityManager.persist(set);
        return set;
    }

    private VocabularySetWord newWord(VocabularySet set, WordInput input) {
        var word = new VocabularySetWord();
        word.setId(new VocabularySetWordId(set.getId(), UUID.randomUUID().toString()));
        word.setVocabularySet(set);
        applyWord(word, input);
        entityManager.persist(word);
        return word;
    }

    private static void applySet(VocabularySet set, SetInput input) {
        set.setTitle(input.title().strip());
        set.setDescription(blankToNull(input.description()));
        set.setLevel(input.level());
        set.setCommunity(Boolean.TRUE.equals(input.isCommunity()));
    }

    private static void applyWord(VocabularySetWord word, WordInput input) {
        word.setJapanese(input.japanese().strip());
        word.setReading(input.reading().strip());
        word.setMeaning(input.meaning().strip());
        word.setExample(blankToNull(input.example()));
        word.setExampleMeaning(blankToNull(input.exampleMeaning()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private VocabularySet ownedSet(Long userId, String id) {
        var set = sets.lockById(id).orElseThrow(VocabularySetService::missingSet);
        if (!isOwner(set, userId)) {
            requireReadable(set, userId);
            throw new ApiException(ErrorCode.AUTH_ACCESS_DENIED);
        }
        return set;
    }

    private VocabularySetWord findWord(String setId, String wordId) {
        return words.findById(new VocabularySetWordId(setId, wordId))
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Vocabulary word not found"));
    }

    private static boolean isOwner(VocabularySet set, Long userId) {
        return set.getOwner() != null && userId.equals(set.getOwner().getId());
    }

    private static void requireReadable(VocabularySet set, Long userId) {
        if (!set.isCommunity() && !isOwner(set, userId)) throw missingSet();
    }

    private static ApiException missingSet() {
        return new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Vocabulary set not found");
    }

    private static SetView view(VocabularySet set, Long userId, long count) {
        return new SetView(set.getId(), set.getTitle(), set.getDescription(), set.getLevel(),
            count, set.getUpdatedAt(), set.isCommunity(), isOwner(set, userId));
    }

    private static WordView wordView(VocabularySetWord word) {
        return new WordView(word.getId().wordId(), word.getJapanese(), word.getReading(), word.getMeaning(),
            word.getExample(), word.getExampleMeaning());
    }
}
