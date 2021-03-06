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

package com.google.errorprone.bugpatterns;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.MaturityLevel;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "BoxedPrimitiveConstructor",
  category = Category.JDK,
  summary = "valueOf or autoboxing provides better time and space performance",
  severity = SeverityLevel.ERROR,
  maturity = MaturityLevel.MATURE
)
public class BoxedPrimitiveConstructor extends BugChecker implements NewClassTreeMatcher {

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    Type type = ASTHelpers.getType(tree);
    if (type == null) {
      return Description.NO_MATCH;
    }
    Types types = state.getTypes();
    type = types.unboxedTypeOrType(type);
    if (!type.isPrimitive()) {
      // TODO(cushon): consider handling String also
      return Description.NO_MATCH;
    }
    return describeMatch(tree, buildFix(tree, state, type));
  }

  private Fix buildFix(NewClassTree tree, VisitorState state, Type type) {
    boolean autoboxFix = shouldAutoboxFix(state);
    if (state.getTypes().isSameType(type, state.getSymtab().booleanType)) {
      Object value = literalValue(tree.getArguments().iterator().next());
      if (value instanceof Boolean) {
        return SuggestedFix.replace(tree, literalFix((boolean) value, autoboxFix));
      } else if (value instanceof String) {
        return SuggestedFix.replace(
            tree, literalFix(Boolean.parseBoolean((String) value), autoboxFix));
      }
    }
    ExpressionTree arg = Iterables.getOnlyElement(tree.getArguments());
    if (autoboxFix && ASTHelpers.getType(arg).isPrimitive()) {
      return SuggestedFix.builder()
          .replace(((JCTree) tree).getStartPosition(), ((JCTree) arg).getStartPosition(), "")
          .replace(state.getEndPosition((JCTree) arg), state.getEndPosition((JCTree) tree), "")
          .build();
    }
    return SuggestedFix.replace(
        ((JCTree) tree).getStartPosition(),
        ((JCTree) arg).getStartPosition(),
        String.format("%s.valueOf(", state.getSourceForNode(tree.getIdentifier())));
  }

  private boolean shouldAutoboxFix(VisitorState state) {
    switch (state.getPath().getParentPath().getLeaf().getKind()) {
      case METHOD_INVOCATION:
        // autoboxing a method argument affects overload resolution
        return false;
      default:
        return true;
    }
  }

  private String literalFix(boolean value, boolean autoboxFix) {
    if (autoboxFix) {
      return value ? "true" : "false";
    }
    return value ? "Boolean.TRUE" : "Boolean.FALSE";
  }

  private Object literalValue(Tree arg) {
    if (!(arg instanceof LiteralTree)) {
      return null;
    }
    return ((LiteralTree) arg).getValue();
  }
}
