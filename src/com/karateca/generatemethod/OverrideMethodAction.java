package com.karateca.generatemethod;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.List;

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

    namespaceFinder.addResultsReadyListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent changeEvent) {
        if (changeEvent.getSource().equals("ParentNamespaceFound")) {
          // TODO: show not found.
          showDialog();
        }
      }
    });

    namespaceFinder.findParentClass();
  }

  /**
   * Show the dialog to select the method to override.
   */
  private void showDialog() {
    List<Function> functionNames = namespaceFinder.getFunctionNames();

    // Select the closest element found from the current position.
    final JBList jbList = new JBList(functionNames.toArray());

    // Open a pop-up to select which describe() or it() you want to change.
    JBPopupFactory.getInstance()
            .createListPopupBuilder(jbList)
            .setTitle("Select the method to override")
            .setItemChoosenCallback(new Runnable() {
              public void run() {
                if (jbList.getSelectedValue() != null) {
                  addNewMethod(jbList.getSelectedValue());
                }
              }
            })
            .createPopup()
            .showCenteredInCurrentWindow(project);
  }

  /**
   * Create a new method that will override the parent.
   * @param selectedMethodName The name of the method that you want ot override.
   */
  private void addNewMethod(Object selectedMethodName) {
    String functionFormat = "%s.prototype." + selectedMethodName;
    String newMethodName = String.format(functionFormat, namespaceFinder.getCurrentNamespace());
    String parentMethodName = String.format(functionFormat, namespaceFinder.getParentNamespace());

    final String methodTemplate = String.format("/**\n" +
            " * @override\n" +
            " */\n" +
            "%s = function() {\n" +
            "  // TODO: method block\n" +
            "  %s.apply(this, arguments);\n" +
            "};\n", newMethodName, parentMethodName);

    CommandUtil.runCommand(project, new Runnable() {
      @Override
      public void run() {
        int offset = editor.getCaretModel().getOffset();
        document.replaceString(offset, offset, methodTemplate);
      }
    });
  }
}
