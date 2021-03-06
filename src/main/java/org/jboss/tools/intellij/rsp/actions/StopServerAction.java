package org.jboss.tools.intellij.rsp.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jboss.tools.intellij.rsp.client.IntelliJRspClientLauncher;
import org.jboss.tools.intellij.rsp.model.impl.RspCore;
import org.jboss.tools.intellij.rsp.ui.tree.RspTreeModel;
import org.jboss.tools.rsp.api.ServerManagementAPIConstants;
import org.jboss.tools.rsp.api.dao.*;

import javax.swing.tree.TreePath;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class StopServerAction extends AbstractTreeAction {
    @Override
    protected boolean isEnabled(Object o) {
        if( o instanceof RspTreeModel.ServerStateWrapper) {
            int state = ((RspTreeModel.ServerStateWrapper)o).getServerState().getState();
            return state == ServerManagementAPIConstants.STATE_STARTED;
        }
        return false;
    }

    @Override
    protected void actionPerformed(AnActionEvent e, TreePath treePath, Object selected) {
        if (selected instanceof RspTreeModel.ServerStateWrapper) {
            RspTreeModel.ServerStateWrapper sel = (RspTreeModel.ServerStateWrapper) selected;
            Project project = ProjectManager.getInstance().getOpenProjects()[0];
            IntelliJRspClientLauncher client = RspCore.getDefault().getClient(sel.getRsp());
            new Thread("Stop Server: " + sel.getServerState().getServer().getId()) {
                public void run() {
                    actionInternal(sel, project, client);
                }
            }.start();
        }
    }
    private void actionInternal(RspTreeModel.ServerStateWrapper sel, Project project, IntelliJRspClientLauncher client) {
        StopServerAttributes ssa = new StopServerAttributes(sel.getServerState().getServer().getId(), false);
        try {
            Status stat = client.getServerProxy().stopServerAsync(ssa).get();
            if( !stat.isOK()) {
                showError(stat);
            }
        } catch (InterruptedException ex) {
            showError(ex);
        } catch (ExecutionException ex) {
            showError(ex);
        }
    }

    private void showError(Status stat) {
        // TODO
    }
    private void showError(Exception stat) {
        // TODO
    }

}
