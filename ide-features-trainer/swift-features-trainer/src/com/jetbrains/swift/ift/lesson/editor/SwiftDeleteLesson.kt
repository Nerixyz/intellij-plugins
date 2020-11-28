package com.jetbrains.swift.ift.lesson.editor

import com.jetbrains.swift.ift.SwiftLessonsBundle
import training.learn.interfaces.Module
import training.learn.lesson.kimpl.KLesson
import training.learn.lesson.kimpl.LessonContext
import training.learn.lesson.kimpl.LessonSample
import training.learn.lesson.kimpl.parseLessonSample

class SwiftDeleteLesson(module: Module) : KLesson("swift.editorbasics.deleteline", SwiftLessonsBundle.message("swift.editor.delete.name"), module, "Swift") {


  private val sample: LessonSample = parseLessonSample("""
import Foundation
import UIKit

class Delete: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()

        let x = 0
        let y = 50

        let tableView = UITableView(frame: CGRect.zero)

        let header = UILabel()
        header.text = "AppCode"
        header.sizeToFit()

        tableView.frame = CGRect(x: x, y: y, width: 320, height: 400)
        tableView.tableHeaderView = header
        self.view.addSubview(tableView)
    }

}""".trimIndent())
  override val lessonContent: LessonContext.() -> Unit = {
    prepareSample(sample)

    caret(12, 9)
    task {
      triggers("EditorDeleteLine")
      text(SwiftLessonsBundle.message("swift.editor.delete.action", action("EditorDeleteLine")))
    }
    task {
      triggers("\$Undo")
      text(SwiftLessonsBundle.message("swift.editor.delete.undo", action("\$Undo")))
    }
  }
}