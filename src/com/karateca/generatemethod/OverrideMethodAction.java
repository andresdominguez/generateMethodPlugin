package com.karateca.generatemethod;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author Andres Dominguez.
 */
public class OverrideMethodAction extends AnAction {
  private Project project;
  private DocumentImpl document;
  private ParentNamespaceFinder namespaceFinder;
  private EditorImpl editor;

  @Override
  public void update(AnActionEvent e) {
    // TODO: disable for non js.
    e.getPresentation().setEnabled(e.getData(PlatformDataKeys.EDITOR) != null);
  }

  public void actionPerformed(AnActionEvent actionEvent) {
    project = actionEvent.getData(PlatformDataKeys.PROJECT);
    editor = (EditorImpl) actionEvent.getData(PlatformDataKeys.EDITOR);
    VirtualFile virtualFile = actionEvent.getData(PlatformDataKeys.VIRTUAL_FILE);
    document = (DocumentImpl) editor.getDocument();

    namespaceFinder = new ParentNamespaceFinder(project, document, editor, virtualFile);

    // Async callback to get the search results for it( and describe(
    namespaceFinder.addResultsReadyListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent changeEvent) {
        if (changeEvent.getSource().equals("ParentNamespaceFound")) {
          String namespace = namespaceFinder.getCurrentNamespace();
          String parentNamespace = namespaceFinder.getParentNamespace();

          // TODO: show error: "not found"
          if (namespace != null) {
//            addMethod(namespace);
          }
        }
      }
    });

    namespaceFinder.findParentClass();
  }
}
