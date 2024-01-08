/*
 * Copyright (C) 2024 Javier Llorente <javier@opensuse.org>
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

import com.javierllorente.obsfx.App;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;

/**
 *
 * @author javier
 */
public class BuildLogTask extends Task<String> {
    
    private static final Logger logger = Logger.getLogger(BuildLogTask.class.getName());

    private final String prj;
    private final String repository;
    private final String arch;
    private final String pkg;

    public BuildLogTask(String prj, String repository, String arch, String pkg) {
        this.prj = prj;
        this.repository = repository;
        this.arch = arch;
        this.pkg = pkg;
    }

    @Override
    protected String call() throws Exception {
        if (prj != null && repository != null && arch != null && pkg != null) {
            logger.log(Level.INFO, "Fetching build log of {0}/{1}/{2}/{3}", 
                    new Object[]{prj, repository, arch, pkg});
            return App.getOBS().getBuildLog(prj, repository,
                    arch, pkg);
        }
        return null;
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        logger.info("Build log fetched");
    }
    
}
