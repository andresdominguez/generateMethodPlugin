package com.karateca.generatemethod;

import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindResult;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.EventDispatcher;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to find the closure provide statement to figure out the namespace.
 */
class ParentNamespaceFinder {

  private final Project project;
  private final DocumentImpl document;
  private final VirtualFile virtualFile;
  private FindManager findManager;

  private String currentNamespace;

  private final EventDispatcher<ChangeListener> myEventDispatcher = EventDispatcher.create(ChangeListener.class);
  private String parentNamespace;

  public ParentNamespaceFinder(Project project, DocumentImpl document, EditorImpl editor, VirtualFile virtualFile) {
    this.project = project;
    this.document = document;
    this.virtualFile = virtualFile;
  }

  public String getCurrentNamespace() {
    return currentNamespace;
  }

  public String getParentNamespace() {
    return parentNamespace;
  }

  public void findParentClass() {
    ApplicationManager.getApplication().runReadAction(new Runnable() {
      @Override
      public void run() {
        if (findParentNamespace()) {
          broadcastEvent("ParentNamespaceFound");
        }
      }
    });
  }

  private boolean findParentNamespace() {
    FindModel findModel = createFindModel();

    // TODO: cannot figure out how to use the reg-exp search. Do a two part search instead.
    CharSequence text = document.getCharsSequence();

    // Find goog.inherits first.
    findModel.setStringToFind("goog.inherits(");
    FindResult result = findManager.findString(text, 0, findModel, virtualFile);
    if (!result.isStringFound()) {
      return false;
    }

    // Now found the closing ")".
    int startOffset = result.getStartOffset();
    int endOffset = result.getEndOffset();

    findModel.setStringToFind(")");
    result = findManager.findString(text, endOffset, findModel, virtualFile);
    if (!result.isStringFound()) {
      return false;
    }

    // Get the whole inherits stmt and extract the current namespace and the
    // parent namespace.
    String inheritsStmt = document.getText(new TextRange(startOffset, result.getEndOffset()))
            .replaceAll("[\\n\\s]*", "");

    String namespaceRegExp = "(goog.inherits\\()([\\w.]+)(,)([\\w.]+)";
    Matcher matcher = Pattern.compile(namespaceRegExp).matcher(inheritsStmt);
    if (!matcher.find()) {
      return false;
    }

    currentNamespace = matcher.group(2);
    parentNamespace = matcher.group(4);

    return true;
  }

  FindModel createFindModel() {
    findManager = FindManager.getInstance(project);
    FindModel clone = (FindModel) findManager.getFindInFileModel().clone();
    clone.setFindAll(true);
    clone.setFromCursor(true);
    clone.setForward(true);
    clone.setMultiline(true);
    clone.setRegularExpressions(true);
    clone.setWholeWordsOnly(false);
    clone.setCaseSensitive(true);
    clone.setSearchHighlighters(true);
    clone.setPreserveCase(false);

    return clone;
  }

  private void broadcastEvent(String eventName) {
    myEventDispatcher.getMulticaster().stateChanged(new ChangeEvent(eventName));
  }

  /**
   * Register for change events.
   *
   * @param changeListener The listener to be added.
   */
  public void addResultsReadyListener(ChangeListener changeListener) {
    myEventDispatcher.addListener(changeListener);
  }
}
