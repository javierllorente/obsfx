/*
 * Copyright (C) 2025 Javier Llorente <javier@opensuse.org>
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
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;

/**
 *
 * @author javier
 */
public class UploadTask extends Task<Void> {
    
    private static final Logger logger = Logger.getLogger(UploadTask.class.getName());

    private final String prj;
    private final String pkg;
    private final File file;

    public UploadTask(String prj, String pkg, File file) {
        this.prj = prj;
        this.pkg = pkg;
        this.file = file;
    }

    @Override
    protected Void call() throws Exception {
        if (prj != null && pkg != null && file != null) {
            logger.log(Level.INFO, "Uploading {0} to {1}/{2}", new Object[]{file, prj, pkg});
            App.getOBS().uploadFile(prj, pkg, file);            
        }
        return null;
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        logger.log(Level.INFO, "File {0} uploaded", file);
    }
    
}
