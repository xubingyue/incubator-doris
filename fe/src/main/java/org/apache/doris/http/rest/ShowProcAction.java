// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.http.rest;

import org.apache.doris.common.AnalysisException;
import org.apache.doris.common.proc.ProcNodeInterface;
import org.apache.doris.common.proc.ProcResult;
import org.apache.doris.common.proc.ProcService;
import org.apache.doris.http.ActionController;
import org.apache.doris.http.BaseRequest;
import org.apache.doris.http.BaseResponse;
import org.apache.doris.http.IllegalArgException;
import org.apache.doris.http.UnauthorizedException;
import org.apache.doris.mysql.privilege.PrivPredicate;

import com.google.common.base.Strings;
import com.google.gson.Gson;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import io.netty.handler.codec.http.HttpMethod;

// Format:
//   http://username:password@10.73.150.30:8138/api/show_proc?path=/
public class ShowProcAction extends RestBaseAction {
    private static final Logger LOG = LogManager.getLogger(ShowProcAction.class);

    public ShowProcAction(ActionController controller) {
        super(controller);
    }

    public static void registerAction(ActionController controller) throws IllegalArgException {
        controller.registerHandler(HttpMethod.GET, "/api/show_proc", new ShowProcAction(controller));
    }

    @Override
    public void execute(BaseRequest request, BaseResponse response) {
        // check authority
        try {
            AuthorizationInfo authInfo = getAuthorizationInfo(request);
            checkGlobalAuth(authInfo, PrivPredicate.ADMIN);
        } catch (UnauthorizedException e) {
            response.appendContent("Authentication Failed. " + e.getMessage());
            sendResult(request, response);
            return;
        }

        String path = request.getSingleParameter("path");
        ProcNodeInterface procNode = null;
        ProcService instance = ProcService.getInstance();
        try {
            if (Strings.isNullOrEmpty(path)) {
                procNode = instance.open("/");
            } else {
                procNode = instance.open(path);
            }
        } catch (AnalysisException e) {
            LOG.warn(e.getMessage());
            response.getContent().append("[]");
        }

        if (procNode != null) {
            ProcResult result;
            try {
                result = procNode.fetchResult();
                List<List<String>> rows = result.getRows();

                Gson gson = new Gson();
                response.setContentType("application/json");
                response.getContent().append(gson.toJson(rows));
            } catch (AnalysisException e) {
                LOG.warn(e.getMessage());
                response.getContent().append("[]");
            }
        }
        sendResult(request, response);
    }
}
