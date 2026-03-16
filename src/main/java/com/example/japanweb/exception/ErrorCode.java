package com.example.japanweb.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Enum defining all application error codes for consistent error handling.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Authentication Errors (1xxx)
    AUTH_INVALID_CREDENTIALS("1001", HttpStatus.UNAUTHORIZED, "Invalid username or password"),
    AUTH_TOKEN_EXPIRED("1002", HttpStatus.UNAUTHORIZED, "Authentication token has expired"),
    AUTH_TOKEN_INVALID("1003", HttpStatus.UNAUTHORIZED, "Invalid authentication token"),
    AUTH_ACCESS_DENIED("1004", HttpStatus.FORBIDDEN, "Access denied"),
    AUTH_USER_NOT_FOUND("1005", HttpStatus.NOT_FOUND, "User not found"),
    AUTH_USERNAME_EXISTS("1006", HttpStatus.CONFLICT, "Username already exists"),
    AUTH_EMAIL_EXISTS("1007", HttpStatus.CONFLICT, "Email already exists"),
    AUTH_ADMIN_REGISTRATION_DISABLED("1008", HttpStatus.FORBIDDEN, "Admin registration is disabled"),
    AUTH_ADMIN_REGISTRATION_INVALID_KEY("1009", HttpStatus.FORBIDDEN, "Invalid admin registration key"),

    // Validation Errors (2xxx)
    VALIDATION_FAILED("2001", HttpStatus.BAD_REQUEST, "Validation failed"),
    VALIDATION_MISSING_FIELD("2002", HttpStatus.BAD_REQUEST, "Required field is missing"),
    VALIDATION_INVALID_FORMAT("2003", HttpStatus.BAD_REQUEST, "Invalid format"),

    // Resource Errors (3xxx)
    RESOURCE_NOT_FOUND("3001", HttpStatus.NOT_FOUND, "Resource not found"),
    RESOURCE_ALREADY_EXISTS("3002", HttpStatus.CONFLICT, "Resource already exists"),
    COURSE_NOT_FOUND("3101", HttpStatus.NOT_FOUND, "Course not found"),
    COURSE_CHAPTER_NOT_FOUND("3102", HttpStatus.NOT_FOUND, "Course chapter not found"),
    COURSE_SECTION_NOT_FOUND("3103", HttpStatus.NOT_FOUND, "Course section not found"),
    COURSE_LESSON_NOT_FOUND("3104", HttpStatus.NOT_FOUND, "Course lesson not found"),

    // Grammar Errors (4xxx)
    GRAMMAR_BOOK_NOT_FOUND("4001", HttpStatus.NOT_FOUND, "Grammar book not found"),
    GRAMMAR_CHAPTER_NOT_FOUND("4002", HttpStatus.NOT_FOUND, "Grammar chapter not found"),
    GRAMMAR_POINT_NOT_FOUND("4003", HttpStatus.NOT_FOUND, "Grammar point not found"),

    // Vocabulary Errors (5xxx)
    VOCAB_COURSE_NOT_FOUND("5001", HttpStatus.NOT_FOUND, "Vocabulary course not found"),
    VOCAB_ENTRY_NOT_FOUND("5002", HttpStatus.NOT_FOUND, "Vocabulary entry not found"),
    VOCAB_IMPORT_FAILED("5003", HttpStatus.INTERNAL_SERVER_ERROR, "Failed to import vocabulary"),
    VOCAB_INVALID_EXCEL("5004", HttpStatus.BAD_REQUEST, "Invalid Excel file format"),

    // Game Errors (6xxx)
    GAME_SESSION_NOT_FOUND("6001", HttpStatus.NOT_FOUND, "Game session not found or expired"),
    GAME_SESSION_EXPIRED("6002", HttpStatus.GONE, "Game session has expired"),
    GAME_INVALID_QUESTION("6003", HttpStatus.BAD_REQUEST, "Invalid question for this session"),
    GAME_INSUFFICIENT_WORDS("6004", HttpStatus.BAD_REQUEST, "Not enough words to start game"),

    // SRS Errors (7xxx)
    SRS_INVALID_RATING("7001", HttpStatus.BAD_REQUEST, "Invalid SRS rating"),
    SRS_PROGRESS_NOT_FOUND("7002", HttpStatus.NOT_FOUND, "Learning progress not found"),

    // JLPT Errors (8xxx)
    JLPT_EXAM_NOT_FOUND("8001", HttpStatus.NOT_FOUND, "JLPT exam not found"),
    JLPT_ATTEMPT_NOT_FOUND("8002", HttpStatus.NOT_FOUND, "JLPT attempt not found"),
    JLPT_ATTEMPT_ALREADY_SUBMITTED("8003", HttpStatus.CONFLICT, "JLPT attempt already submitted"),
    JLPT_QUESTION_NOT_FOUND("8004", HttpStatus.NOT_FOUND, "JLPT question not found"),
    JLPT_QUESTION_NOT_IN_EXAM("8005", HttpStatus.BAD_REQUEST, "Question does not belong to this exam"),
    JLPT_RESULT_NOT_AVAILABLE("8006", HttpStatus.BAD_REQUEST, "JLPT result is not available yet"),
    JLPT_ATTEMPT_EXPIRED("8007", HttpStatus.GONE, "JLPT attempt has expired"),
    JLPT_OPTION_KEY_INVALID("8008", HttpStatus.BAD_REQUEST, "Selected option is invalid for this question"),
    JLPT_EXPIRED("8009", HttpStatus.GONE, "JLPT attempt time has expired"),
    JLPT_EXAM_NOT_PUBLISHED("8010", HttpStatus.BAD_REQUEST, "JLPT exam is not published"),

    // System Errors (9xxx)
    INTERNAL_ERROR("9001", HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred"),
    SERVICE_UNAVAILABLE("9002", HttpStatus.SERVICE_UNAVAILABLE, "Service is temporarily unavailable");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
