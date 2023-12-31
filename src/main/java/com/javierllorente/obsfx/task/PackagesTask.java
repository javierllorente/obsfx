/*
 * Copyright (C) 2023 Javier Llorente <javier@opensuse.org>
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;

/**
 *
 * @author javier
 */
public class PackagesTask extends Task<List<String>> {
    
    private static final Logger logger = Logger.getLogger(PackagesTask.class.getName());

    private final String prj;

    public PackagesTask(String prj) {
        this.prj = prj;
    }

    @Override
    protected List<String> call() throws Exception {
        if (prj != null) {
            logger.log(Level.INFO, "Fetching packages of {0}", prj);
            return App.getOBS().getPackageList(prj);
        }
        return null;
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        logger.info("Packages fetched");
    }
    
}
