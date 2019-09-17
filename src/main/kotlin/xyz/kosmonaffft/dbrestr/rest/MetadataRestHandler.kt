//  Copyright 2019 Anton V. Kirilchik <kosmonaffft@gmail.com>
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package xyz.kosmonaffft.dbrestr.rest

import io.swagger.v3.core.util.Json
import io.swagger.v3.core.util.Yaml
import org.springframework.stereotype.Service
import xyz.kosmonaffft.dbrestr.service.MetadataService
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType.APPLICATION_JSON

/**
 * @author Anton V. Kirilchik
 * @since 10.06.2019
 */
@Service
@Path("/")
class MetadataRestHandler(private var metadataService: MetadataService) {

    @GET
    @Path("metadata/yaml")
    @Produces(APPLICATION_JSON)
    fun getYamlMetadata(@PathParam("schema") schema: String): String {
        val openAPI = metadataService.generateOpenApiV3Metadata(schema)
        return Yaml.pretty(openAPI)
    }

    @GET
    @Path("metadata/json")
    @Produces(APPLICATION_JSON)
    fun getJsonMetadata(@PathParam("schema") schema: String): String {
        val openAPI = metadataService.generateOpenApiV3Metadata(schema)
        return Json.pretty(openAPI)
    }
}
