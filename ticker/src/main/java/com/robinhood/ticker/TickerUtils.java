/*
 * Copyright (C) 2016 Robinhood Markets, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.robinhood.ticker;

/**
 * Static utility class for the ticker package. This class contains helper methods such as those
 * that generate default character lists to use for animation.
 *
 * @author Jin Cao, Robinhood
 */
public class TickerUtils {
    static final CharSequence EMPTY_CHAR = String.valueOf((char) 0);

    public static String provideNumberList() {
        return "0123456789";
    }

    public static String provideAlphabeticalList() {
        return "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    }
}
