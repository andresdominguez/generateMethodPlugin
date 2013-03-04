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
                  addNewMethod((Function) jbList.getSelectedValue());
                }
              }
            })
            .createPopup()
            .showCenteredInCurrentWindow(project);
  }

  /**
   * Create a new method that will override the parent.
   *
   * @param function The method that you want ot override.
   */
  private void addNewMethod(Function function) {
    String fnNameFormat = "%s.prototype." + function.getName();
    String methodPrototype = String.format(fnNameFormat, namespaceFinder.getCurrentNamespace());
    String parentMethodPrototype = String.format(fnNameFormat, namespaceFinder.getParentNamespace());


    String arguments = function.getArguments();
    String callArguments = arguments;
    if (callArguments.trim().length() > 0) {
      callArguments = ", " + callArguments;
    }

    final String methodTemplate = String.format("/**\n" +
            " * @override\n" +
            " */\n" +
            "%s = function(%s) {\n" +
            "  // TODO: override function\n" +
            "  %s.call(this%s);\n" +
            "};\n", methodPrototype, arguments, parentMethodPrototype, callArguments);

    CommandUtil.runCommand(project, new Runnable() {
      @Override
      public void run() {
        int offset = editor.getCaretModel().getOffset();
        document.replaceString(offset, offset, methodTemplate);
      }
    });
  }
}
