package com.example.japanweb.seeder;

import com.example.japanweb.entity.*;
import com.example.japanweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MockJlptDataSeeder implements CommandLineRunner {

    private final JlptExamRepository examRepository;
    private final JlptSectionRepository sectionRepository;
    private final JlptQuestionRepository questionRepository;
    private final JlptQuestionOptionRepository optionRepository;
    private final JlptAnswerKeyRepository answerKeyRepository;

    @Override
    public void run(String... args) {
        if (examRepository.count() > 0) {
            return;
        }

        System.out.println("Seeding mock JLPT N4 Exam...");

        JlptExam exam = JlptExam.builder()
                .code("N4-2023-07")
                .title("JLPT N4 - Mock Test 1")
                .level("N4")
                .examYear(2023)
                .examMonth(7)
                .totalDurationMinutes(125)
                .contentStatus(JlptContentStatus.COMPLETE)
                .published(true)
                .build();
        exam = examRepository.save(exam);

        JlptSection vocabSection = JlptSection.builder()
                .exam(exam)
                .sectionType(JlptSectionType.LANGUAGE_KNOWLEDGE)
                .title("言語知識（文字・語彙）")
                .sectionOrder(1)
                .durationMinutes(25)
                .build();
        vocabSection = sectionRepository.save(vocabSection);

        seedVocabQuestions(vocabSection);

        JlptSection grammarSection = JlptSection.builder()
                .exam(exam)
                .sectionType(JlptSectionType.GRAMMAR_KNOWLEDGE)
                .title("言語知識（文法）")
                .sectionOrder(2)
                .durationMinutes(20)
                .build();
        grammarSection = sectionRepository.save(grammarSection);
        
        seedGrammarQuestions(grammarSection);

        System.out.println("Mock JLPT N4 Exam seeded successfully.");
    }

    private void seedVocabQuestions(JlptSection section) {
        JlptQuestion q1 = questionRepository.save(JlptQuestion.builder()
                .section(section)
                .questionNumber(1)
                .prompt("私のははは【料理】が上手です。")
                .explanation("料理 = りょうり")
                .build());

        saveOptions(q1, "りょうり", "りょり", "ちょうり", "ちゅうり", "A");

        JlptQuestion q2 = questionRepository.save(JlptQuestion.builder()
                .section(section)
                .questionNumber(2)
                .prompt("明日は【休み】です。")
                .explanation("休み = やすみ")
                .build());

        saveOptions(q2, "やすみ", "なつみ", "よすみ", "はるみ", "A");
    }

    private void seedGrammarQuestions(JlptSection section) {
        JlptQuestion q1 = questionRepository.save(JlptQuestion.builder()
                .section(section)
                .questionNumber(1)
                .prompt("これは私が【　　】本です。")
                .explanation("私が読んだ本 (The book that I read).")
                .build());

        saveOptions(q1, "読む", "読んで", "読んだ", "読みます", "C");
        
        JlptQuestion q2 = questionRepository.save(JlptQuestion.builder()
                .section(section)
                .questionNumber(2)
                .prompt("今、雨が【　　】います。")
                .explanation("雨が降っています (It is raining).")
                .build());

        saveOptions(q2, "降る", "降って", "降った", "降り", "B");
    }

    private void saveOptions(JlptQuestion question, String opt1, String opt2, String opt3, String opt4, String correctKey) {
        optionRepository.saveAll(List.of(
                JlptQuestionOption.builder().question(question).optionKey("A").optionText(opt1).optionOrder(1).build(),
                JlptQuestionOption.builder().question(question).optionKey("B").optionText(opt2).optionOrder(2).build(),
                JlptQuestionOption.builder().question(question).optionKey("C").optionText(opt3).optionOrder(3).build(),
                JlptQuestionOption.builder().question(question).optionKey("D").optionText(opt4).optionOrder(4).build()
        ));

        answerKeyRepository.save(JlptAnswerKey.builder()
                .question(question)
                .correctOptionKey(correctKey)
                .scoreWeight(1)
                .build());
    }
}
