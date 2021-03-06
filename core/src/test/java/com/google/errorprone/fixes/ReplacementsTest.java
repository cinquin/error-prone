/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.fixes;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link Replacements}Test */
@RunWith(JUnit4.class)
public class ReplacementsTest {

  @Test
  public void duplicate() {
    Replacements replacements = new Replacements();
    replacements.add(Replacement.create(42, 42, "hello"));
    try {
      replacements.add(Replacement.create(42, 42, "goodbye"));
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage()).contains("conflicts with existing replacement");
    }
  }

  @Test
  public void overlap() {
    Replacements replacements = new Replacements();
    replacements.add(Replacement.create(2, 4, "hello"));
    try {
      replacements.add(Replacement.create(3, 5, "goodbye"));
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected.getMessage())
          .contains("replacement 'goodbye' overlaps with existing replacements");
    }
  }

  private static final Function<Replacement, Range<Integer>> AS_RANGES =
      new Function<Replacement, Range<Integer>>() {
        @Override
        public Range<Integer> apply(Replacement replacement) {
          return replacement.range();
        }
      };

  @Test
  public void descending() {
    assertThat(
            Iterables.transform(
                new Replacements()
                    .add(Replacement.create(0, 0, "hello"))
                    .add(Replacement.create(0, 1, "hello"))
                    .descending(),
                AS_RANGES))
        .containsExactly(Range.closedOpen(0, 1), Range.closedOpen(0, 0))
        .inOrder();
    assertThat(
            Iterables.transform(
                new Replacements()
                    .add(Replacement.create(0, 1, "hello"))
                    .add(Replacement.create(0, 0, "hello"))
                    .descending(),
                AS_RANGES))
        .containsExactly(Range.closedOpen(0, 1), Range.closedOpen(0, 0))
        .inOrder();
  }

  @Test
  public void identicalDuplicatesOK() {
    Replacements replacements = new Replacements();
    replacements.add(Replacement.create(42, 42, "hello"));
    replacements.add(Replacement.create(42, 42, "hello"));
  }
}
