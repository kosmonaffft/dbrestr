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

package xyz.kosmonaffft.dbrestr.service

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.stereotype.Service

/**
 * @author Anton V. Kirilchik
 * @since 17.09.2019
 */
@Service
class MetadataService {

    fun generateOpenApiV3Metadata(schema: String): OpenAPI {
        val info = Info()
                .title("dbrestr")
                .version("1.0.0")

        val openAPI = OpenAPI().info(info)

        return openAPI
    }
}
