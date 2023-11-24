package com.services.authorization.annotations;

/**
 * @Author prakashponali
 * @Date 22/11/23
 * @Description
 */
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class EmailValidatorTest {

    private EmailValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new EmailValidator();
        context = mock(ConstraintValidatorContext.class);
    }

    @Test
    void testValidEmail() {
        assertTrue(validator.isValid("test@example.com", context));
    }

    @Test
    void testInvalidEmail() {
        assertFalse(validator.isValid("test@", context));
    }

    @Test
    void testEmptyEmail() {
        assertFalse(validator.isValid("", context));
    }

    @Test
    void testNullEmail() {
        assertFalse(validator.isValid(null, context));
    }

    // Add more tests to cover different edge cases
}
