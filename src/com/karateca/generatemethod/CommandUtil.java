package com.karateca.generatemethod;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;

/**
 * @author Andres Dominguez.
 */
public class CommandUtil {

  public static void runCommand(Project project, final Runnable runnable, String operationName) {
    CommandProcessor.getInstance().executeCommand(project, new Runnable() {
      @Override
      public void run() {
        ApplicationManager.getApplication().runWriteAction(runnable);
      }
    }, operationName, null);
  }
}
