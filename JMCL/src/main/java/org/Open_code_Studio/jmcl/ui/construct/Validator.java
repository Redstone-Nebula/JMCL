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

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.control.TextInputControl;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.Open_code_Studio.jmcl.util.javafx.SafeStringConverter;

public final class Validator implements Predicate<String> {

    public static Consumer<Predicate<String>> addTo(TextInputControl control) {
        return addTo(control, null);
    }

    /**
     * @see SafeStringConverter#asPredicate(Consumer)
     */
    public static Consumer<Predicate<String>> addTo(TextInputControl control, String message) {
        return predicate -> {
            Validator validator = new Validator(message, predicate);
            InvalidationListener listener = any -> {};
            control.textProperty().addListener(new WeakInvalidationListener(listener));
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

    @Override
    public boolean test(String text) {
        return validator.test(text);
    }
}