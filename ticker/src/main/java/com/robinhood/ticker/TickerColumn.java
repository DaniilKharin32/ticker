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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ReplacementSpan;

/**
 * Represents a column of characters to be drawn on the screen. This class primarily handles
 * animating within the column from one character to the next and drawing all of the intermediate
 * states.
 *
 * @author Jin Cao, Robinhood
 */
class TickerColumn {
    private TickerCharacterList[] characterLists;
    private final TickerDrawMetrics metrics;

    private CharSequence currentChar = TickerUtils.EMPTY_CHAR;
    private CharSequence targetChar = TickerUtils.EMPTY_CHAR;

    // The indices characters simply signify what positions are for the current and target
    // characters in the assigned characterList. This tells us how to animate from the current
    // to the target characters.
    private CharSequence[] currentCharacterList;
    private int startIndex;
    private int endIndex;

    // Drawing state variables that get updated whenever animation progress gets updated.
    private int bottomCharIndex;
    private float bottomDelta;
    private float charHeight;

    // Drawing state variables for handling size transition
    private float sourceWidth, currentWidth, targetWidth, minimumRequiredWidth;

    // The bottom delta variables signifies the vertical offset that the bottom drawn character
    // is seeing. If the delta is 0, it means that the character is perfectly centered. If the
    // delta is negative, it means that the bottom character is poking out from the bottom and
    // part of the top character is visible. The delta should never be positive because it means
    // that the bottom character is not actually the bottom character.
    private float currentBottomDelta;
    private float previousBottomDelta;
    private int directionAdjustment;

    TickerColumn(TickerCharacterList[] characterLists, TickerDrawMetrics metrics) {
        this.characterLists = characterLists;
        this.metrics = metrics;
    }

    /**
     * Updates the characterLists used in the column.
     */
    void setCharacterLists(TickerCharacterList[] characterLists) {
        this.characterLists = characterLists;
    }

    /**
     * Tells the column that the next character it should show is {@param targetChar}. This can
     * change can either be animated or instant depending on the animation progress set by
     * {@link #setAnimationProgress(float)}.
     */
    void setTargetChar(CharSequence targetChar) {
        // Set the current and target characters for the animation
        this.targetChar = targetChar;
        this.sourceWidth = this.currentWidth;
        this.targetWidth = metrics.getCharWidth(targetChar);
        this.minimumRequiredWidth = Math.max(this.sourceWidth, this.targetWidth);

        // Calculate the current indices
        setCharacterIndices();

        final boolean scrollDown = endIndex >= startIndex;
        directionAdjustment = scrollDown ? 1 : -1;

        // Save the currentBottomDelta as previousBottomDelta in case this call to setTargetChar
        // interrupted a previously running animation. The deltas will then be used to compute
        // offset so that the interruption feels smooth on the UI.
        previousBottomDelta = currentBottomDelta;
        currentBottomDelta = 0f;
    }

    CharSequence getCurrentChar() {
        return currentChar;
    }

    CharSequence getTargetChar() {
        return targetChar;
    }

    float getCurrentWidth() {
        checkForDrawMetricsChanges();
        return currentWidth;
    }

    float getMinimumRequiredWidth() {
        checkForDrawMetricsChanges();
        return minimumRequiredWidth;
    }

    /**
     * A helper method for populating {@link #startIndex} and {@link #endIndex} given the
     * current and target characters for the animation.
     */
    private void setCharacterIndices() {
        currentCharacterList = null;

        for (int i = 0; i < characterLists.length; i++) {
            final TickerCharacterList.CharacterIndices indices =
                    characterLists[i].getCharacterIndices(currentChar, targetChar, metrics.getPreferredScrollingDirection());
            if (indices != null) {
                this.currentCharacterList = this.characterLists[i].getCharacterList();
                this.startIndex = indices.startIndex;
                this.endIndex = indices.endIndex;
            }
        }

        // If we didn't find a list that contains both characters, just perform a default animation
        // going straight from source to target
        if (currentCharacterList == null) {
            if (LevenshteinUtils.equalsCharArrays(currentChar, targetChar)) {
                currentCharacterList = new CharSequence[]{currentChar};
                startIndex = endIndex = 0;
            } else {
                currentCharacterList = new CharSequence[]{currentChar, targetChar};
                startIndex = 0;
                endIndex = 1;
            }
        }
    }

    void onAnimationEnd() {
        checkForDrawMetricsChanges();
        minimumRequiredWidth = currentWidth;
    }

    private void checkForDrawMetricsChanges() {
        final float currentTargetWidth = metrics.getCharWidth(targetChar);
        // Only resize due to DrawMetrics changes when we are done with whatever animation we
        // are running.
        if (currentWidth == targetWidth && targetWidth != currentTargetWidth) {
            this.minimumRequiredWidth = this.currentWidth = this.targetWidth = currentTargetWidth;
        }
    }

    void setAnimationProgress(float animationProgress) {
        if (animationProgress == 1f) {
            // Animation finished (or never started), set to stable state.
            this.currentChar = this.targetChar;
            currentBottomDelta = 0f;
            previousBottomDelta = 0f;
        }

        final float charHeight = metrics.getCharHeight();

        // First let's find the total height of this column between the start and end chars.
        final float totalHeight = charHeight * Math.abs(endIndex - startIndex);

        // The current base is then the part of the total height that we have progressed to
        // from the animation. For example, there might be 5 characters, each character is
        // 2px tall, so the totalHeight is 10. If we are at 50% progress, then our baseline
        // in this column is at 5 out of 10 (which is the 3rd character with a -50% offset
        // to the baseline).
        final float currentBase = animationProgress * totalHeight;

        // Given the current base, we now can find which character should drawn on the bottom.
        // Note that this position is a float. For example, if the bottomCharPosition is
        // 4.5, it means that the bottom character is the 4th character, and it has a -50%
        // offset relative to the baseline.
        final float bottomCharPosition = currentBase / charHeight;

        // By subtracting away the integer part of bottomCharPosition, we now have the
        // percentage representation of the bottom char's offset.
        final float bottomCharOffsetPercentage = bottomCharPosition - (int) bottomCharPosition;

        // We might have interrupted a previous animation if previousBottomDelta is not 0f.
        // If that's the case, we need to take this delta into account so that the previous
        // character offset won't be wiped away when we start a new animation.
        // We multiply by the inverse percentage so that the offset contribution from the delta
        // progresses along with the rest of the animation (from full delta to 0).
        final float additionalDelta = previousBottomDelta * (1f - animationProgress);

        // Now, using the bottom char's offset percentage and the delta we have from the
        // previous animation, we can now compute what's the actual offset of the bottom
        // character in the column relative to the baseline.
        bottomDelta = bottomCharOffsetPercentage * charHeight * directionAdjustment
                + additionalDelta;

        // Figure out what the actual character index is in the characterList, and then
        // draw the character with the computed offset.
        bottomCharIndex = startIndex + ((int) bottomCharPosition * directionAdjustment);

        this.charHeight = charHeight;
        this.currentWidth = sourceWidth + (targetWidth - sourceWidth) * animationProgress;
    }

    /**
     * Draw the current state of the column as it's animating from one character in the list
     * to another. This method will take into account various factors such as animation
     * progress and the previously interrupted animation state to render the characters
     * in the correct position on the canvas.
     */
    void draw(Canvas canvas, Paint textPaint) {
        if (drawText(canvas, textPaint, currentCharacterList, bottomCharIndex, bottomDelta)) {
            // Save the current drawing state in case our animation gets interrupted
            if (bottomCharIndex >= 0) {
                currentChar = currentCharacterList[bottomCharIndex];
            }
            currentBottomDelta = bottomDelta;
        }

        // Draw the corresponding top and bottom characters if applicable
        drawText(canvas, textPaint, currentCharacterList, bottomCharIndex + 1,
                bottomDelta - charHeight);
        // Drawing the bottom character here might seem counter-intuitive because we've been
        // computing for the bottom character this entire time. But the bottom character
        // computed above might actually be above the baseline if we interrupted a previous
        // animation that gave us a positive additionalDelta.
        drawText(canvas, textPaint, currentCharacterList, bottomCharIndex - 1,
                bottomDelta + charHeight);
    }

    /**
     * @return whether the text was successfully drawn on the canvas
     */
    private boolean drawText(Canvas canvas, Paint textPaint, CharSequence[] characterPlainList,
                             int index, float verticalOffset) {
        if (index >= 0 && index < characterPlainList.length) {
            CharSequence chars = characterPlainList[index];
            if (chars instanceof Spannable) {
                Spannable spannableChars = ((Spannable) chars);
                int spanStart;
                int spanStartNew;
                int spanEnd = 0;
                int spanEndNew;
                ReplacementSpan[] emojiSpans = spannableChars.getSpans(0, spannableChars.length(), ReplacementSpan.class);
                if (emojiSpans.length != 0) {
                    for (ReplacementSpan span : emojiSpans) {
                        spanStartNew = spannableChars.getSpanStart(span);
                        spanEndNew = spannableChars.getSpanEnd(span);
                        if (spanEnd != spanStartNew) {
                            canvas.drawText(spannableChars, spanEnd, spanStartNew, 0f, verticalOffset, textPaint);
                        }
                        spanStart = spanStartNew;
                        spanEnd = spanEndNew;
                        span.draw(canvas, spannableChars, spanStart, spanEnd, 0f, 0, (int) verticalOffset, 0, textPaint);
                    }
                } else {
                    canvas.drawText(chars, 0, chars.length(), 0f, verticalOffset, textPaint);
                }
            } else {
                canvas.drawText(chars, 0, chars.length(), 0f, verticalOffset, textPaint);
            }
            return true;
        }
        return false;
    }
}
