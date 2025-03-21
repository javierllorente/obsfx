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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;

/**
 *
 * @author javier
 */
public class ProjectsTask extends Task<List<String>> {

    private static final Logger logger = Logger.getLogger(ProjectsTask.class.getName());
    private final boolean includeHomePrjs;

    public ProjectsTask(boolean includeHomePrjs) {
        this.includeHomePrjs = includeHomePrjs;
    }    

    @Override
    protected List<String> call() 
            throws IOException {
        logger.log(Level.INFO, "Fetching projects");
        List<String> projects = new ArrayList<>();
        List<OBSEntry> entries = App.getOBS().getProjects().getEntries();
        String userHome = "home:" + App.getOBS().getUsername();
        entries.forEach((t) -> {
            if (includeHomePrjs) {
                projects.add(t.getName());
            } else {
                if (t.getName().startsWith(userHome)) {
                    projects.add(t.getName());
                } else if (!t.getName().startsWith("home")) {
                    projects.add(t.getName());
                }
            }
        });
        return projects;
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        logger.info("Projects fetched");
    }
    
    @Override
    protected void failed() {
        super.failed();
    }
    
}
