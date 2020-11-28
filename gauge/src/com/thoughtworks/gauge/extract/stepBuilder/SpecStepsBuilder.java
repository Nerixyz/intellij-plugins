/*
 * Copyright (C) 2020 ThoughtWorks, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.thoughtworks.gauge.extract.stepBuilder;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.thoughtworks.gauge.language.psi.impl.SpecStepImpl;

import java.util.List;

final class SpecStepsBuilder extends StepsBuilder {
  SpecStepsBuilder(Editor editor, PsiFile psiFile) {
    super(editor, psiFile);
  }

  @Override
  public List<PsiElement> build() {
    List<PsiElement> specSteps = getPsiElements(SpecStepImpl.class);
    int count = 0;
    for (PsiElement element : specSteps) {
      SpecStepImpl specStep = (SpecStepImpl)element;
      if (specStep.getInlineTable() != null && TextToTableMap.get(specStep.getInlineTable().getText().trim()) == null) {
        tableMap.put("table" + (++count), specStep.getInlineTable().getText().trim());
        TextToTableMap.put(specStep.getInlineTable().getText().trim(), "table" + count);
      }
    }
    return specSteps;
  }
}
