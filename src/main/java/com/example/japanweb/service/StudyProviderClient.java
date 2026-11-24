package com.example.japanweb.service;

import com.example.japanweb.exception.ApiException;
import com.example.japanweb.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

@Component
@RequiredArgsConstructor
public class StudyProviderClient {
    private static final int RESPONSE_LIMIT = 512 * 1024;
    private final ObjectMapper mapper;

    public InputStream open(URI uri, Object payload, String accept) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(80_000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", accept);
            connection.setDoOutput(true);
            byte[] bytes = mapper.writeValueAsBytes(payload);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (var output = connection.getOutputStream()) {
                output.write(bytes);
            }

            int status = connection.getResponseCode();
            String contentType = connection.getContentType();
            if (status != 200 || contentType == null || !contentType.startsWith(accept)) {
                throw unavailable();
            }

            HttpURLConnection openConnection = connection;
            return new FilterInputStream(connection.getInputStream()) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        openConnection.disconnect();
                    }
                }
            };
        } catch (IOException error) {
            if (connection != null) connection.disconnect();
            throw unavailable();
        } catch (RuntimeException error) {
            if (connection != null) connection.disconnect();
            throw error;
        }
    }

    public JsonNode json(URI uri, Object payload) {
        try (var body = open(uri, payload, "application/json")) {
            byte[] bytes = body.readNBytes(RESPONSE_LIMIT + 1);
            if (bytes.length > RESPONSE_LIMIT) throw unavailable();
            JsonNode result = mapper.readTree(bytes);
            if (result == null || result.isMissingNode()) throw unavailable();
            return result;
        } catch (IOException error) {
            throw unavailable();
        }
    }

    public static ApiException unavailable() {
        return new ApiException(ErrorCode.SERVICE_UNAVAILABLE, "Study provider is temporarily unavailable");
    }
}
