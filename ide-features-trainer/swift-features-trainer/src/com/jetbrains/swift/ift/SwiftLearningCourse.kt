// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.swift.ift

import com.jetbrains.swift.ift.lesson.codegeneration.SwiftCreateFromUsageLesson
import com.jetbrains.swift.ift.lesson.codegeneration.SwiftGenerateLesson
import com.jetbrains.swift.ift.lesson.codegeneration.SwiftOverrideImplementLesson
import com.jetbrains.swift.ift.lesson.codegeneration.SwiftQuickFixesAndIntentionsLesson
import com.jetbrains.swift.ift.lesson.editor.*
import com.jetbrains.swift.ift.lesson.navigation.*
import com.jetbrains.swift.ift.lesson.refactorings.*
import com.jetbrains.swift.ift.lesson.rundebugtest.SwiftDebugLesson
import com.jetbrains.swift.ift.lesson.rundebugtest.SwiftRunLesson
import com.jetbrains.swift.ift.lesson.rundebugtest.SwiftTestLesson
import training.learn.LearningModule
import training.learn.course.LearningCourseBase
import training.learn.interfaces.LessonType

class SwiftLearningCourse : LearningCourseBase("Swift") {
  override fun modules() = listOf(
    LearningModule(name = SwiftLessonsBundle.message("swift.editor.module.name"),
                   description = SwiftLessonsBundle.message("swift.editor.module.description"),
                   sampleFileName = "Editor",
                   primaryLanguage = langSupport,
                   moduleType = LessonType.PROJECT) {
      listOf(
        SwiftCompletionLesson(it),
        SwiftSelectionLesson(it),
        SwiftCommentLesson(it),
        SwiftDeleteLesson(it),
        SwiftDuplicateLesson(it),
        SwiftMoveLesson(it),
        SwiftFoldingLesson(it),
        SwiftMultipleSelectionsLesson(it),
        SwiftCodeFormattingLesson(it),
        SwiftQuickPopupsLesson(it),
      )
    },
    LearningModule(name = SwiftLessonsBundle.message("swift.code.generations.module.name"),
                   description = SwiftLessonsBundle.message("swift.code.generations.module.description"),
                   sampleFileName = "CodeGeneration",
                   primaryLanguage = langSupport,
                   moduleType = LessonType.PROJECT) {
      listOf(
        SwiftGenerateLesson(it),
        SwiftOverrideImplementLesson(it),
        SwiftCreateFromUsageLesson(it),
        SwiftQuickFixesAndIntentionsLesson(it),
      )
    },
    LearningModule(name = SwiftLessonsBundle.message("swift.navigation.module.name"),
                   description = SwiftLessonsBundle.message("swift.navigation.module.description"),
                   sampleFileName = "Navigation",
                   primaryLanguage = langSupport,
                   moduleType = LessonType.PROJECT) {
      listOf(
        SwiftMainWindowsViewsLesson(it),
        SwiftTODOsBookmarksLesson(it),
        SwiftCodeNavigationLesson(it),
        SwiftPreciseNavigationLesson(it),
        SwiftSearchLesson(it),
      )
    },
    LearningModule(name = SwiftLessonsBundle.message("swift.refactorings.module.name"),
                   description = SwiftLessonsBundle.message("swift.refactorings.module.description"),
                   sampleFileName = "Refactorings",
                   primaryLanguage = langSupport,
                   moduleType = LessonType.PROJECT) {
      listOf(
        SwiftRenameLesson(it),
        SwiftExtractVariableLesson(it),
        SwiftExtractMethodLesson(it),
        SwiftExtractClosureLesson(it),
        SwiftChangeSignatureLesson(it),
      )
    },
    LearningModule(name = SwiftLessonsBundle.message("swift.run.debug.test.module.name"),
                   description = SwiftLessonsBundle.message("swift.run.debug.test.module.description"),
                   sampleFileName = "RunDebugTest",
                   primaryLanguage = langSupport,
                   moduleType = LessonType.PROJECT) {
      listOf(
        SwiftRunLesson(it),
        SwiftDebugLesson(it),
        SwiftTestLesson(it),
      )
    },
  )
}