package org.jboss.tools.intellij.rsp.ui.util;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jboss.tools.intellij.rsp.ui.dialogs.WorkflowDialog;
import org.jboss.tools.intellij.rsp.util.CommandLineUtils;
import org.jboss.tools.intellij.rsp.util.ExecUtilClone;
import org.jboss.tools.rsp.api.dao.Status;
import org.jboss.tools.rsp.api.dao.WorkflowResponse;
import org.jboss.tools.rsp.api.dao.WorkflowResponseItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowUiUtility {

    public static Map<String, Object> displayPromptsSeekWorkflowInput(WorkflowResponse resp) {
        List<WorkflowResponseItem> items = resp.getItems();
        List<WorkflowResponseItem> prompts = new ArrayList<>();
        Project project = ProjectManager.getInstance().getOpenProjects()[0];

        for( WorkflowResponseItem i : items ) {
            String type = i.getItemType();
            if( type == null )
                type = "workflow.prompt.small";
            if( type.equals("workflow.browser.open")) {
                String urlString = i.getContent();
                UIHelper.executeInUI(() -> {
                    BrowserUtil.open(urlString); // TODO check if proper thread?
                });
            } else if( type.equals("workflow.editor.open")) {
                UIHelper.executeInUI(() -> {
                    if (i.getProperties().get("workflow.editor.file.path") != null) {
                        EditorUtil.openFileInEditor(project, new File(i.getProperties().get("workflow.editor.file.path")));
                    } else if (i.getProperties().get("workflow.editor.file.content") != null) {
                        EditorUtil.createAndOpenVirtualFile(i.getId(), i.getProperties().get("workflow.editor.file.content"), project);
                    }
                });
            } else if( type.equals("workflow.terminal.open")) {
                UIHelper.executeInUI(() -> {
                    try {
                        String cmd = i.getProperties().get("workflow.terminal.cmd");
                        String[] asArr = CommandLineUtils.translateCommandline(cmd);
                        File wd = new File(System.getProperty("user.home"));
                        ExecUtilClone.executeWithTerminal(project, "title", wd, false, asArr);
                    } catch (IOException | CommandLineUtils.CommandLineException e) {
                        // TODO
                    }
                });
            } else if( type.equals("workflow.prompt.small") || type.equals("workflow.prompt.large")) {
                prompts.add(i);
            }
        }
        if( prompts.size() > 0 ) {
            final Map<String, Object> values = new HashMap<>();
            UIHelper.executeInUI(() -> {
                WorkflowDialog wd = new WorkflowDialog(prompts.toArray(new WorkflowResponseItem[0]));
                wd.show();
                values.putAll(wd.getAttributes());
            });
            return values;
        }
        // Fallback impl
        return new HashMap<String, Object>();
    }


    public  static boolean workflowComplete(WorkflowResponse resp) {
        if( resp == null || resp.getStatus() == null) {
            return true;
        }
        int statusSev = resp.getStatus().getSeverity();
        if( statusSev == Status.CANCEL || statusSev == Status.ERROR ) {
            return true;
        }
        if( statusSev == Status.OK) {
            return true;
        }
        return false;
    }
}
