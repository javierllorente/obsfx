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
package com.javierllorente.obsfx.adapter;

import com.javierllorente.jobs.entity.OBSRequest;
import java.util.Date;

/**
 *
 * @author javier
 */
public class RequestAdapter {
    
    private final OBSRequest request;

    public RequestAdapter(OBSRequest request) {
        this.request = request;
    }
    
    public Date getCreated() {
        return request.getState().getCreated().toGregorianCalendar().getTime();
    }
    
    public String getId() {
        return request.getId();
    }
    
    public String getSource() {
        return request.getAction().getSource().toString();
    }
    
    public String getSourceProject() {
        return request.getAction().getSource().getPrj();
    }
    
    public String getSourcePackage() {
        return request.getAction().getSource().getPkg();
    }
    
    public String getTarget() {
        return request.getAction().getTarget().toString();
    }
    
    public String getCreator() {
        return request.getCreator();
    }
    
    public String getActionType() {
        return request.getAction().getType();
    }
    
    public String getState() {
        return request.getState().toString();
    }
    
    public String getDescription() {
        return request.getDescription();
    }
}
