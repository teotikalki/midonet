/*
 * Copyright 2014 Midokura SARL
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

package org.midonet.api.rest_api;

import javax.ws.rs.WebApplicationException;

import org.midonet.brain.services.rest_api.ResponseUtils;

/**
 * WebApplicationException class to represent 504 status. Thrown when
 * an upstream service returns an invalid response.
 */
public class BadGatewayHttpException  extends WebApplicationException {
    private static final long serialVersionUID = 1L;

    public BadGatewayHttpException(String message) {
        super(ResponseUtils.buildErrorResponse(502, message));
    }
}

