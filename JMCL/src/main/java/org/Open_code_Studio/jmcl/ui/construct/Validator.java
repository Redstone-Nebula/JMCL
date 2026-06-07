/*
 * JMCL
 * Copyright (C) 2026  OCS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.Open_code_Studio.jmcl.ui.construct;

import javafx.scene.control.TextField;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.Open_code_Studio.jmcl.util.javafx.SafeStringConverter;

public final class Validator {
    public static Consumer<Predicate<String>> addTo(TextField control) {
        return addTo(control, null);
    }

    /**
     * @see SafeStringConverter#asPredicate(Consumer)
     */
    public static Consumer<Predicate<String>> addTo(TextField control, String message) {
        // JFoenix validation removed - validation is now done via simple string checks
        return predicate -> {
            // No-op: validation is handled externally via TextField text checks
        };
    }

    private final Predicate<String> validator;

    /**
     * @param validator return true if the input string is valid.
     */
    public Validator(Predicate<String> validator) {
        this.validator = validator;
    }

    public Validator(String message, Predicate<String> validator) {
        this(validator);
    }

    public boolean test(String text) {
        return validator.test(text);
    }
}