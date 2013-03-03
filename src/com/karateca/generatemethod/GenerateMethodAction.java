package com.karateca.generatemethod;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author Andres Dominguez.
 */
public class GenerateMethodAction extends AnAction {

  private Project project;
  private DocumentImpl document;
  private NamespaceFinder namespaceFinder;
  private EditorImpl editor;

  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabled(e.getData(PlatformDataKeys.EDITOR) != null);
  }

  public void actionPerformed(AnActionEvent actionEvent) {
    project = actionEvent.getData(PlatformDataKeys.PROJECT);
    editor = (EditorImpl) actionEvent.getData(PlatformDataKeys.EDITOR);
    VirtualFile virtualFile = actionEvent.getData(PlatformDataKeys.VIRTUAL_FILE);
    document = (DocumentImpl) editor.getDocument();

    namespaceFinder = new NamespaceFinder(project, document, editor, virtualFile);

    // Async callback to get the search results for it( and describe(
    namespaceFinder.addResultsReadyListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent changeEvent) {
        if (changeEvent.getSource().equals("NamespaceFound")) {
          String namespace = namespaceFinder.getNamespaceFound();
          addMethodRunningCommand(namespace);
        }
      }
    });

    namespaceFinder.findText("goog.provide", false);
  }

  private void addMethodRunningCommand(final String namespace) {
    CommandUtil.runCommand(project, new Runnable() {
      @Override
      public void run() {
        addMethod(namespace);
      }
    }, "Added method");
  }

  private void addMethod(String namespace) {
    int offset = editor.getCaretModel().getOffset();

    String methodTemplate = String.format("%s.prototype.$END$ = function() {\n\n};", namespace);
    document.replaceString(offset, offset, methodTemplate);
  }
}
