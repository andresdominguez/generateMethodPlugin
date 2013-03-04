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
public class GenerateMethodAction extends AnAction {

  private Project project;
  private DocumentImpl document;
  private NamespaceFinder namespaceFinder;
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

    namespaceFinder = new NamespaceFinder(project, document, virtualFile);

    // Async callback to get the search results for it( and describe(
    namespaceFinder.addResultsReadyListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent changeEvent) {
        if (changeEvent.getSource().equals("NamespaceFound")) {
          String namespace = namespaceFinder.getNamespaceFound();

          // TODO: show error: "not found"
          if (namespace != null) {
            addMethod(namespace);
          }
        }
      }
    });

    namespaceFinder.findText("goog.provide", false);
  }

  private void addMethod(final String namespace) {
    CommandUtil.runCommand(project, new Runnable() {
      @Override
      public void run() {
        int offset = editor.getCaretModel().getOffset();

        String methodTemplate = String.format("%s.prototype. = function() {\n" +
                "  // TODO: method block\n" +
                "};", namespace);
        document.replaceString(offset, offset, methodTemplate);

        // Put the caret after "protoype."
        editor.getCaretModel().moveToOffset(offset + namespace.length() + 11);
      }
    });
  }
}
