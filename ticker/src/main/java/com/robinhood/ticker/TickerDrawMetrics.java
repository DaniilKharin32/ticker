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

import android.graphics.Paint;
import android.text.Spannable;
import android.text.style.ReplacementSpan;

import java.util.HashMap;
import java.util.Map;

/**
 * This wrapper class represents some core drawing metrics that {@link TickerView} and
 * {@link TickerColumnManager} require to calculate the positions and offsets for rendering
 * the text onto the canvas.
 *
 * @author Jin Cao
 */
class TickerDrawMetrics {
    private final Paint textPaint;

    // These are attributes on the text paint used for measuring and drawing the text on the
    // canvas. These attributes are reset whenever anything on the text paint changes.
    private final Map<CharSequence, Float> charWidths = new HashMap<>(256);
    private float charHeight, charBaseline;

    private TickerView.ScrollingDirection preferredScrollingDirection = TickerView.ScrollingDirection.ANY;

    TickerDrawMetrics(Paint textPaint) {
        this.textPaint = textPaint;
        invalidate();
    }

    void invalidate() {
        charWidths.clear();
        final Paint.FontMetrics fm = textPaint.getFontMetrics();
        charHeight = fm.bottom - fm.top;
        charBaseline = -fm.top;
    }

    float getCharWidth(CharSequence character) {
        if (LevenshteinUtils.equalsCharArrays(character, TickerUtils.EMPTY_CHAR)) {
            return 0;
        }

        // This method will lazily initialize the char width map.
        final Float value = charWidths.get(character);
        if (value != null) {
            return value;
        } else {
            float width = 0;
            if (character instanceof Spannable) {
                Spannable spannableChars = ((Spannable) character);
                int spanStart = 0;
                int spanStartNew;
                int spanEnd = 0;
                int spanEndNew;
                ReplacementSpan[] emojiSpans = spannableChars.getSpans(0, spannableChars.length(), ReplacementSpan.class);
                if (emojiSpans.length != 0) {
                    for (ReplacementSpan span : emojiSpans) {
                        spanStartNew = spannableChars.getSpanStart(span);
                        spanEndNew = spannableChars.getSpanEnd(span);
                        if (spanEnd != spanStartNew) {
                            width += textPaint.measureText(character, spanEnd, spanStartNew);
                        }
                        if (spanStart != spanStartNew) {
                            width += textPaint.measureText(character, spanStart, spanStartNew);
                        }
                        spanStart = spanStartNew;
                        spanEnd = spanEndNew;
                        width += span.getSize(textPaint, spannableChars, spanStart, spanEnd, textPaint.getFontMetricsInt());
                    }
                }else {
                    width = textPaint.measureText(character, 0, character.length());
                }
                charWidths.put(character, width);
            } else {
                width = textPaint.measureText(character, 0, character.length());
                charWidths.put(character, width);
            }
            return width;
        }
    }

    float getCharHeight() {
        return charHeight;
    }

    float getCharBaseline() {
        return charBaseline;
    }

    TickerView.ScrollingDirection getPreferredScrollingDirection() {
        return preferredScrollingDirection;
    }

    void setPreferredScrollingDirection(TickerView.ScrollingDirection preferredScrollingDirection) {
        this.preferredScrollingDirection = preferredScrollingDirection;
    }
}
