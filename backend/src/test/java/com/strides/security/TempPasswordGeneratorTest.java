package com.strides.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Pure generator, no collaborators to mock and no DB involved. */
class TempPasswordGeneratorTest {

    private static final String AMBIGUOUS_CHARS = "0O1lI";

    private final TempPasswordGenerator generator = new TempPasswordGenerator();

    @Test
    void generate_hasTheDocumentedLength() {
        assertEquals(16, generator.generate().length());
    }

    @Test
    void generate_containsAtLeastOneLetterAndOneDigit() {
        String password = generator.generate();

        assertTrue(password.chars().anyMatch(Character::isLetter));
        assertTrue(password.chars().anyMatch(Character::isDigit));
    }

    @Test
    void generate_neverContainsAmbiguousCharacters() {
        // Run several times since it's random - a single sample wouldn't rule out the bug.
        IntStream.range(0, 50).mapToObj(i -> generator.generate()).forEach(password ->
                AMBIGUOUS_CHARS.chars().forEach(c ->
                        assertFalse(password.indexOf(c) >= 0, password + " contains ambiguous char '" + (char) c + "'")));
    }

    @Test
    void generate_producesDifferentValuesAcrossCalls() {
        assertFalse(generator.generate().equals(generator.generate()));
    }
}
