package com.edstem.interviewprep.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.net.URISyntaxException;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = HttpUrl.Validator.class)
public @interface HttpUrl {

    String message() default "Must be a valid http or https URL";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<HttpUrl, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.isBlank()) {
                return true;
            }
            try {
                URI uri = new URI(value.trim());
                String scheme = uri.getScheme();
                return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                        && uri.getHost() != null && !uri.getHost().isBlank();
            } catch (URISyntaxException e) {
                return false;
            }
        }
    }
}
