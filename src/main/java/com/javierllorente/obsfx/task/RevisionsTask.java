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

import com.javierllorente.jobs.entity.OBSRevision;
import com.javierllorente.jobs.entity.OBSRevisionList;
import com.javierllorente.obsfx.App;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;

/**
 *
 * @author Javier Llorente <javier@opensuse.org>
 */
public class RevisionsTask extends Task<List<OBSRevision>> {
    
    private static final Logger logger = Logger.getLogger(RevisionsTask.class.getName());

    private final String prj;
    private final String pkg;
    private final boolean latest;

    public RevisionsTask(String prj, String pkg, boolean latest) {
        this.prj = prj;
        this.pkg = pkg;
        this.latest = latest;
    }

    @Override
    protected List<OBSRevision> call() throws Exception {
        if (prj != null && pkg != null) {
            logger.log(Level.INFO, "Fetching revisions of {0}/{1}", new Object[]{prj, pkg});            
            OBSRevisionList revisionList = latest ? App.getOBS().getLatestRevision(prj, pkg) 
                    : App.getOBS().getRevisions(prj, pkg);
            List<OBSRevision> revisions = revisionList.getRevisions();
            return (revisions == null) ? new ArrayList<>() : revisions;
        }
        return null;
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        logger.info("Revisions fetched");
    }
    
    public String getPkg() {
        return pkg;
    }
    
}
