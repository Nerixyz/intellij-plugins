// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.prettierjs;

import com.intellij.lang.javascript.linter.GlobPatternUtil;
import com.intellij.lang.javascript.linter.LinterSaveActionsManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PrettierSaveAction extends LinterSaveActionsManager.LinterSaveAction {
  static final ReformatWithPrettierAction.ErrorHandler NOOP_ERROR_HANDLER = new ReformatWithPrettierAction.ErrorHandler() {
    @Override
    public void showError(@NotNull Project project, @Nullable Editor editor, @NotNull String text, @Nullable Runnable onLinkClick) {
      // No need to show any notification in case of 'Prettier on save' failure. Most likely the file is simply not syntactically valid at the moment.
    }
  };

  @Override
  public boolean isEnabledForProject(@NotNull Project project) {
    return PrettierConfiguration.getInstance(project).isRunOnSave();
  }

  @Override
  public void processDocuments(@NotNull Project project, @NotNull Document @NotNull [] documents) {
    PrettierConfiguration prettierConfiguration = PrettierConfiguration.getInstance(project);
    if (!prettierConfiguration.isRunOnSave()) return;

    FileDocumentManager manager = FileDocumentManager.getInstance();
    List<VirtualFile> files = ContainerUtil.mapNotNull(documents, document -> {
      return manager.getFile(document);
    });

    List<VirtualFile> matchingFiles =
      GlobPatternUtil.filterFilesMatchingGlobPattern(project, prettierConfiguration.getFilesPattern(), files);

    if (!matchingFiles.isEmpty()) {
      ReformatWithPrettierAction.processVirtualFiles(project, matchingFiles, NOOP_ERROR_HANDLER);
    }
  }
}
