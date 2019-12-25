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

import com.opentable.db.postgres.embedded.FlywayPreparer
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import org.junit.Rule
import org.junit.Test
import xyz.kosmonaffft.dbrestr.configuration.ConfigurationProperties
import xyz.kosmonaffft.dbrestr.service.impl.DatabaseMetadataServiceImpl
import xyz.kosmonaffft.dbrestr.service.impl.OpenApiMetadataServiceImpl

class MetadataRestHandlerTest {

    @Rule
    @JvmField
    var db = EmbeddedPostgresRules.preparedDatabase(
            FlywayPreparer.forClasspathLocation("migrations"))

    @Test
    fun getYamlMetadata() {
        val props = ConfigurationProperties().apply {
            schemas = arrayOf("public")
        }

        val metadataService = DatabaseMetadataServiceImpl(db.testDatabase, props)
        val openApiService = OpenApiMetadataServiceImpl(metadataService)

        val restHandler = MetadataRestHandler(openApiService)

        val yaml = restHandler.getYamlMetadata()
    }

    @Test
    fun getJsonMetadata() {
    }
}