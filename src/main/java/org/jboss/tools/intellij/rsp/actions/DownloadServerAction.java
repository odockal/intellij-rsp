package org.jboss.tools.intellij.rsp.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ssh.process.CommandLineUtil;
import org.jboss.tools.intellij.rsp.client.IntelliJRspClientLauncher;
import org.jboss.tools.intellij.rsp.model.IRsp;
import org.jboss.tools.intellij.rsp.model.IRspCore;
import org.jboss.tools.intellij.rsp.model.impl.RspCore;
import org.jboss.tools.intellij.rsp.ui.dialogs.NewServerDialog;
import org.jboss.tools.intellij.rsp.ui.dialogs.SelectDownloadRuntimeDialog;
import org.jboss.tools.intellij.rsp.ui.dialogs.WorkflowDialog;
import org.jboss.tools.intellij.rsp.ui.util.EditorUtil;
import org.jboss.tools.intellij.rsp.ui.util.UIHelper;
import org.jboss.tools.intellij.rsp.ui.util.WorkflowUiUtility;
import org.jboss.tools.intellij.rsp.util.CommandLineUtils;
import org.jboss.tools.intellij.rsp.util.ExecUtilClone;
import org.jboss.tools.rsp.api.dao.*;

import javax.swing.tree.TreePath;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class DownloadServerAction extends AbstractTreeAction {
    @Override
    protected boolean isEnabled(Object o) {
        return o instanceof IRsp && ((IRsp) o).getState() == IRspCore.IJServerState.STARTED;
    }

    @Override
    protected void actionPerformed(AnActionEvent e, TreePath treePath, Object selected) {
        if (selected instanceof IRsp) {
            final IRsp server = (IRsp) selected;
            if (server.getState() == IRspCore.IJServerState.STARTED) {
                Project project = ProjectManager.getInstance().getOpenProjects()[0];
                IntelliJRspClientLauncher client = RspCore.getDefault().getClient(server);

                try {
                    ListDownloadRuntimeResponse runtimeResponse = client.getServerProxy().listDownloadableRuntimes().get();
                    SelectDownloadRuntimeDialog td = new SelectDownloadRuntimeDialog(server, runtimeResponse);
                    UIHelper.executeInUI(() -> {
                        td.show();
                        DownloadRuntimeDescription chosen = td.getSelected();
                        if (chosen != null && td.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
                            new Thread("Download Runtime Workflow: " + server.getRspType().getName()) {
                                public void run() {
                                    initiateDownloadRuntimeWorkflow(server, client, chosen);
                                }
                            }.start();
                        }
                    });
                } catch (InterruptedException interruptedException) {
                    // TODO
                } catch (ExecutionException executionException) {
                    // TODO
                }
            }
        }
    }

    private void initiateDownloadRuntimeWorkflow(IRsp server, IntelliJRspClientLauncher client, DownloadRuntimeDescription chosen) {
        DownloadSingleRuntimeRequest req = new DownloadSingleRuntimeRequest();
        req.setDownloadRuntimeId(chosen.getId());
        try {
            WorkflowResponse resp = client.getServerProxy().downloadRuntime(req).get();
            boolean done = false;
            while (!done) {
                boolean isComplete = WorkflowUiUtility.workflowComplete(resp);
                if (isComplete)
                    return;
                Map<String, Object> toSend = WorkflowUiUtility.displayPromptsSeekWorkflowInput(resp);
                if (toSend == null) {
                    return; // Give up. User canceled.
                }
                DownloadSingleRuntimeRequest req2 = new DownloadSingleRuntimeRequest();
                req2.setRequestId(resp.getRequestId());
                req2.setDownloadRuntimeId(chosen.getId());
                req2.setData(toSend);
                resp = client.getServerProxy().downloadRuntime(req2).get();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }
}