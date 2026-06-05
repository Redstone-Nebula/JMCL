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

import javafx.beans.NamedArg;
import org.glavo.url.WebURL;
import org.Open_code_Studio.jmcl.util.StringUtils;

import java.util.function.Predicate;

import static org.Open_code_Studio.jmcl.util.i18n.I18n.i18n;

public class URLValidator implements Predicate<String> {
    private final boolean nullable;

    public URLValidator() {
        this(false);
    }

    public URLValidator(@NamedArg("nullable") boolean nullable) {
        this.nullable = nullable;
    }

    @Override
    public boolean test(String text) {
        if (StringUtils.isBlank(text))
            return nullable;
        else {
            return WebURL.tryParseBrowserInput(text) != null;
        }
    }
}