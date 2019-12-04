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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import xyz.kosmonaffft.dbrestr.service.OpenAPIMetadataService

/**
 * @author Anton V. Kirilchik
 * @since 10.06.2019
 */
@RestController
class MetadataRestHandler(private var openaMetadataService: OpenAPIMetadataService) {

    @RequestMapping(method = [RequestMethod.GET], path = ["/metadata/yaml"], produces = ["application/yaml"])
    fun getYamlMetadata(): String {
        val openAPI = openaMetadataService.generateOpenApiV3Metadata()
        return Yaml.pretty(openAPI)
    }

    @RequestMapping(method = [RequestMethod.GET], path = ["/metadata/json"], produces = ["application/json"])
    fun getJsonMetadata(): String {
        val openAPI = openaMetadataService.generateOpenApiV3Metadata()
        return Json.pretty(openAPI)
    }
}
