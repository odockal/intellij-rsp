/*************************************************************************************
 * Copyright (c) 2014-2019 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.intellij.rsp.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class UnzipUtility {
    private static final String EXTRACTING = "Extracting ..."; //$NON-NLS-1$
    private static final String SEPARATOR = "/"; //$NON-NLS-1$

    private File file;
    private String discoveredRoot = null;
    private boolean rootEntryImpossible = false;

    public UnzipUtility(File file) {
        this.file = file;
    }

    public void extract(File destination) throws IOException {
        if (file == null || !file.exists()) {
            return;
        }

        String possibleRoot = null;
        destination.mkdirs();
        try(ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                File entryFile = new File(destination, entryName);
                createEntry(zipFile, entry, entryFile);

                // Lets check for a possible root, to avoid scanning the archive again later
                if (!rootEntryImpossible && discoveredRoot == null) {
                    // Check for a root
                    if (entryName == null || entryName.isEmpty() || entryName.startsWith(SEPARATOR) || entryName.indexOf(SEPARATOR) == -1) {
                        rootEntryImpossible = true;
                        possibleRoot = null;
                    } else {
                        String directory = entryName.substring(0, entryName.indexOf(SEPARATOR));
                        if (possibleRoot == null) {
                            possibleRoot = directory;
                        } else if (!directory.equals(possibleRoot)) {
                            rootEntryImpossible = true;
                            possibleRoot = null;
                        }
                    }
                }
            }
        } catch (IOException e) {
            boolean isZipped = false;
            try (ZipInputStream test = new ZipInputStream(new FileInputStream(file))) {
                isZipped = test.getNextEntry() != null;
            } catch(IOException ioe) {
                // ignore
            }

            String msg = "Error opening zip file " + file.getAbsolutePath();
            if( !isZipped) {
                msg += ";  file may not be a properly formated zip file.";
            }
            throw new IOException(msg, e);
        }
        discoveredRoot = possibleRoot;
    }

    private void createEntry(ZipFile zipFile,
                             ZipEntry entry, File entryFile) throws IOException {
        if (entry.isDirectory()) {
            entryFile.mkdirs();
        } else {
            entryFile.getParentFile().mkdirs();
            try(InputStream in = zipFile.getInputStream(entry);
                OutputStream out = new FileOutputStream(entryFile)) {
                copy(in, out);
            }
        }
    }

    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[16 * 1024];
        int len;
        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }
    }



    /*
     * Discover the new root folder of the extracted runtime.
     */
    public String getRoot() throws IOException {
        // IF we found a root during the extract, use that.
        if( discoveredRoot != null )
            return discoveredRoot;
        if( rootEntryImpossible)
            return null;

        String root = null;
        try (ZipFile zipFile = new ZipFile(file)){
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName == null || entryName.isEmpty()
                        || entryName.startsWith(SEPARATOR) || entryName.indexOf(SEPARATOR) == -1) {
                    return null;
                }
                String directory = entryName.substring(0, entryName.indexOf(SEPARATOR));
                if (root == null) {
                    root = directory;
                    continue;
                }
                if (!directory.equals(root)) {
                    return null;
                }
            }
        } catch(IOException ioe) {
            throw ioe;
        }
        return root;
    }

}
