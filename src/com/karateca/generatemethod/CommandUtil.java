package com.karateca.generatemethod;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;

/**
 * @author Andres Dominguez.
 */
class CommandUtil {

  public static void runCommand(Project project, final Runnable runnable) {
    CommandProcessor.getInstance().executeCommand(project, new Runnable() {
      @Override
      public void run() {
        ApplicationManager.getApplication().runWriteAction(runnable);
      }
    }, "Added method", null);
  }
}
