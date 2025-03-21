/*
 * Copyright (C) 2023-2025 Javier Llorente <javier@opensuse.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.javierllorente.obsfx.task;

import com.javierllorente.jobs.entity.OBSEntry;
import com.javierllorente.obsfx.App;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;

/**
 *
 * @author javier
 */
public class FilesTask extends Task<List<OBSEntry>> {
    
    private static final Logger logger = Logger.getLogger(FilesTask.class.getName());

    private final String prj;
    private final String pkg;

    public FilesTask(String prj, String pkg) {
        this.prj = prj;
        this.pkg = pkg;
    }

    @Override
    protected List<OBSEntry> call() throws Exception {
        if (prj != null && pkg != null) {
            logger.log(Level.INFO, "Fetching files of {0}/{1}", new Object[]{prj, pkg});
            List<OBSEntry> entries = App.getOBS().getFiles(prj, pkg).getEntries();
            return (entries == null) ? new ArrayList<>() : entries;
        }
        return null;
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        logger.info("Files fetched");
    }
    
    public String getPkg() {
        return pkg;
    }
    
}
