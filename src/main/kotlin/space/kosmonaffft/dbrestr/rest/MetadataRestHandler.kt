//  Copyright 2019-2020 Anton V. Kirilchik <kosmonaffft@gmail.com>
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

package space.kosmonaffft.dbrestr.rest

import io.swagger.v3.core.util.Json
import io.swagger.v3.core.util.Yaml
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import space.kosmonaffft.dbrestr.service.api.OpenApiMetadataService

@RestController
class MetadataRestHandler(private val openApiMetadataService: OpenApiMetadataService) {

    @GetMapping(path = ["/openapi.yaml"], produces = ["text/yaml"])
    fun getYamlMetadata(): String {
        val openAPI = openApiMetadataService.generateOpenApiV3Metadata()
        return Yaml.pretty(openAPI)
    }

    @GetMapping(path = ["/openapi.json"], produces = ["application/json"])
    fun getJsonMetadata(): String {
        val openAPI = openApiMetadataService.generateOpenApiV3Metadata()
        return Json.pretty(openAPI)
    }
}
