/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.intellij.rsp.model;

public interface IRsp {
    IRspCore getModel();

    public IRspType getRspType();
    public String getLatestVersion();
    public String getInstalledVersion();
    public ServerConnectionInfo start();
    public void stop();
    public void terminate();
    IRspCore.IJServerState getState();

    boolean exists();
    void download();
}
