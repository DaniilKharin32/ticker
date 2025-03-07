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

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This is the primary class that Ticker uses to determine how to animate from one character
 * to another. The provided string dictates what characters will appear between
 * the start and end characters.
 *
 * <p>For example, given the string "abcde", if the view wants to animate from 'd' to 'b',
 * it will know that it has to go from 'd' to 'c' to 'b', and these are the characters
 * that show up during the animation scroll.
 *
 * @author Jin Cao, Robinhood
 */
class TickerCharacterList {
    private final int numOriginalCharacters;
    // The saved character list will always be of the format: EMPTY, list, list
    private final CharSequence[] characterList;
    // A minor optimization so that we can cache the indices of each character.
    private final Map<String, Integer> characterIndicesMap;

    TickerCharacterList(CharSequence characterList) {
        if (LevenshteinUtils.indexOf(characterList,TickerUtils.EMPTY_CHAR)!=-1) {
            throw new IllegalArgumentException(
                    "You cannot include TickerUtils.EMPTY_CHAR in the character list.");
        }
        final CharSequence[] charsArray = LevenshteinUtils.toCharArrayOfArray(characterList);
        final int length = charsArray.length;
        this.numOriginalCharacters = length;

        characterIndicesMap = new HashMap<>(length);
        for (int i = 0; i < length; i++) {
            characterIndicesMap.put(String.valueOf(charsArray[i]), i);
        }

        this.characterList = new CharSequence[length * 2 + 1];
        this.characterList[0] = TickerUtils.EMPTY_CHAR;
        for (int i = 0; i < length; i++) {
            this.characterList[1 + i] = charsArray[i];
            this.characterList[1 + length + i] = charsArray[i];
        }
    }

    /**
     * @param start the character that we want to animate from
     * @param end the character that we want to animate to
     * @param direction the preferred {@Link TickerView#ScrollingDirection}
     * @return a valid pair of start and end indices, or null if the inputs are not supported.
     */
    CharacterIndices getCharacterIndices(CharSequence start, CharSequence end, TickerView.ScrollingDirection direction) {
        int startIndex = getIndexOfChar(start);
        int endIndex = getIndexOfChar(end);

        if (startIndex < 0 || endIndex < 0) {
            return null;
        }

        switch (direction) {
            case DOWN:
                if (LevenshteinUtils.equalsCharArrays(end, TickerUtils.EMPTY_CHAR)) {
                    endIndex = characterList.length;
                } else if (endIndex < startIndex) {
                    endIndex += numOriginalCharacters;
                }

                break;
            case UP:
                if (startIndex < endIndex) {
                    startIndex += numOriginalCharacters;
                }

                break;
            case ANY:
                // see if the wrap-around animation is shorter distance than the original animation
                if (start != TickerUtils.EMPTY_CHAR && end != TickerUtils.EMPTY_CHAR) {
                    if (endIndex < startIndex) {
                        // If we are potentially going backwards
                        final int nonWrapDistance = startIndex - endIndex;
                        final int wrapDistance = numOriginalCharacters - startIndex + endIndex;
                        if (wrapDistance < nonWrapDistance) {
                            endIndex += numOriginalCharacters;
                        }
                    } else if (startIndex < endIndex) {
                        // If we are potentially going forwards
                        final int nonWrapDistance = endIndex - startIndex;
                        final int wrapDistance = numOriginalCharacters - endIndex + startIndex;
                        if (wrapDistance < nonWrapDistance) {
                            startIndex += numOriginalCharacters;
                        }
                    }
                }

                break;
        }

        return new CharacterIndices(startIndex, endIndex);
    }

    Set<String> getSupportedCharacters() {
       return characterIndicesMap.keySet();
    }

    CharSequence[] getCharacterList() {
        return characterList;
    }

    private int getIndexOfChar(CharSequence c) {
        String s = String.valueOf(c);
        if (LevenshteinUtils.equalsCharArrays(c,TickerUtils.EMPTY_CHAR)) {
            return 0;
        } else if (characterIndicesMap.containsKey(s)) {
            return characterIndicesMap.get(s) + 1;
        } else {
            return -1;
        }
    }

    class CharacterIndices {
        final int startIndex;
        final int endIndex;

        public CharacterIndices(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }
}
