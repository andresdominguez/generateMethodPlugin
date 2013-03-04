package com.karateca.generatemethod;

import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindResult;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.EventDispatcher;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Find the namespace for the current file and the namespace for the parent.
 */
class ParentNamespaceFinder {

  private final Project project;
  private final DocumentImpl document;
  private final VirtualFile virtualFile;
  private final EditorImpl editor;
  private FindManager findManager;

  private String currentNamespace;

  private final EventDispatcher<ChangeListener> myEventDispatcher = EventDispatcher.create(ChangeListener.class);
  private String parentNamespace;
  private List<String> methodNames;

  public ParentNamespaceFinder(Project project, DocumentImpl document, EditorImpl editor, VirtualFile virtualFile) {
    this.project = project;
    this.document = document;
    this.editor = editor;
    this.virtualFile = virtualFile;
  }

  public String getCurrentNamespace() {
    return currentNamespace;
  }

  public String getParentNamespace() {
    return parentNamespace;
  }

  public List<String> getMethodNames() {
    return methodNames;
  }

  public void findParentClass() {
    ApplicationManager.getApplication().runReadAction(new Runnable() {
      @Override
      public void run() {
        if (!findParentNamespace()) {
          return;
        }

        VirtualFile parentFile = findParentFile();
        if (parentFile == null) {
          return;
        }

        try {
          methodNames = getMethodNames(parentFile);
          broadcastEvent("ParentNamespaceFound");
        } catch (IOException e) {
          System.err.println("Error reading file " + virtualFile.getName());
          e.printStackTrace(System.err);
        }
      }
    });
  }

  private VirtualFile findParentFile() {
    final VirtualFile[] parentFile = {null};

    ProjectRootManager.getInstance(editor.getProject()).getFileIndex().iterateContent(new ContentIterator() {
      @Override
      public boolean processFile(VirtualFile virtualFile) {
        boolean parentFileFound = false;
        try {
          parentFileFound = fileProvidesNamespace(virtualFile, parentNamespace);
        } catch (IOException e) {
          System.err.println("Error reading file " + virtualFile.getName());
          e.printStackTrace(System.err);
        }

        if (parentFileFound) {
          parentFile[0] = virtualFile;
        }

        // Stop the search.
        return !parentFileFound;
      }
    });

    return parentFile[0];
  }

  /**
   * Search for the file providing the namespace.
   *
   * @param virtualFile
   * @param parentNamespace
   * @return
   */
  private boolean fileProvidesNamespace(VirtualFile virtualFile, String parentNamespace) throws IOException {
    String provideSearchLine = "goog.provide('" + parentNamespace;
    String fileName = virtualFile.getName();

    // Ignore non-js files.
    if (!fileName.endsWith(".js")) {
      return false;
    }

    String fileContents = getFileContents(virtualFile);
    return fileContents.contains(provideSearchLine);
  }

  private String getFileContents(VirtualFile virtualFile) throws IOException {
    return new String(virtualFile.contentsToByteArray());
  }

  private List<String> getMethodNames(VirtualFile virtualFile) throws IOException {
    List<String> result = new ArrayList<String>();

    String fileContents = getFileContents(virtualFile);
    String methodPattern = String.format("(%s.prototype.)([\\w]+)", parentNamespace);

    Pattern pattern = Pattern.compile(methodPattern);
    Matcher matcher = pattern.matcher(fileContents);
    while (matcher.find()) {
      result.add(matcher.group(2));
    }

    return result;
  }

  private boolean findParentNamespace() {
    FindModel findModel = createFindModel();

    // TODO: cannot figure out how to use the reg-exp search. Do a two part search instead.
    CharSequence text = document.getCharsSequence();

    // Find goog.inherits first from the beginning.
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
    TextRange textRange = new TextRange(startOffset, result.getEndOffset());
    String inheritsStmt = document.getText(textRange).replaceAll("[\\n\\s]*", "");

    String namespaceRegExp = "(goog.inherits\\()([\\w.]+)(,)([\\w.]+)";
    Matcher matcher = Pattern.compile(namespaceRegExp).matcher(inheritsStmt);
    if (!matcher.find()) {
      return false;
    }

    currentNamespace = matcher.group(2);
    parentNamespace = matcher.group(4);

    return true;
  }

  private FindModel createFindModel() {
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
