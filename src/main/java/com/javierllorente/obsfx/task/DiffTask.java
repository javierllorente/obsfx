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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author javier
 */
public class DiffTask extends Task<String> {

    private static final Logger logger = Logger.getLogger(DiffTask.class.getName());
    private final String id;

    public DiffTask(String id) {
        this.id = id;
    }
    
    
    @Override
    protected String call() 
            throws IOException, ParserConfigurationException, SAXException  {
        logger.log(Level.INFO, id);
        if (id != null) {
            return App.getOBS().getRequestDiff(id);
        }
        return null;
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        logger.info("Diff fetched");
    }
    
    @Override
    protected void failed() {
        super.failed();
    }
    
}
