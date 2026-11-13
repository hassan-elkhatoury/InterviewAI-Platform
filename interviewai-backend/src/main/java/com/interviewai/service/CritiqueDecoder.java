package com.interviewai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Emits only decoded explanation text from a partially received JSON grade. */
final class CritiqueDecoder {
  private static final Pattern FIELD = Pattern.compile("\\\"explanation\\\"\\s*:\\s*\\\"");
  private final StringBuilder json = new StringBuilder();
  private final ObjectMapper mapper;
  private final Consumer<String> tokens;
  private int emitted;

  CritiqueDecoder(ObjectMapper mapper, Consumer<String> tokens) {
    this.mapper = mapper;
    this.tokens = tokens;
  }

  void accept(String chunk) throws IOException {
    json.append(chunk);
    if (json.length() > 60000) throw new IOException("AI response too large");
    var match = FIELD.matcher(json);
    if (!match.find()) return;
    int start = match.end(), end = start;
    while (end < json.length()) {
      char c = json.charAt(end);
      if (c == '"') break;
      if (c == '\\') {
        if (end + 1 >= json.length()) break;
        int size = json.charAt(end + 1) == 'u' ? 6 : 2;
        if (end + size > json.length()) break;
        end += size;
      } else end++;
    }
    String decoded = mapper.readValue("\"" + json.substring(start, end) + "\"", String.class);
    int length = decoded.length();
    if (length > 0 && Character.isHighSurrogate(decoded.charAt(length - 1))) length--;
    if (length > emitted) {
      tokens.accept(decoded.substring(emitted, length));
      emitted = length;
    }
  }

  String json() {
    return json.toString();
  }
}
