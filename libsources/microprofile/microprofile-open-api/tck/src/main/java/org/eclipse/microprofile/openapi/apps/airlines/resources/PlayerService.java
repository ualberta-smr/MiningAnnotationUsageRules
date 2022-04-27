/**
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package org.eclipse.microprofile.openapi.apps.airlines.resources;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient
@Path("/")
public interface PlayerService {

    @GET
    @Path("/player/{playerId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getPlayerById(@PathParam("playerId") String id);

    @POST
    @Path("/rank/{playerId}/recordGame")
    public void recordGame(@PathParam("playerId") String id, @QueryParam("place") int place,
            @HeaderParam("Authorization") String token);

}
