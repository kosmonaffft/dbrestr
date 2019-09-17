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

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.stereotype.Service
import xyz.kosmonaffft.dbrestr.configuration.ConfigurationProperties
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import javax.sql.DataSource

/**
 * @author Anton V. Kirilchik
 * @since 17.09.2019
 */
@Service
class MetadataService(private val dataSource: DataSource,
                      private val configurationProperties: ConfigurationProperties) {

    fun generateOpenApiV3Metadata(): OpenAPI = dataSource.connection.use { connection ->
        val openAPI = OpenAPI()
                .info(generateInfo())
                .servers(generateServers())
                .components(generateComponents(connection))
        return openAPI
    }

    private fun generateComponents(connection: Connection): Components {
        val databaseMetadata = connection.metaData
        val schemas: ArrayList<String> = getSchemasNames(databaseMetadata)
        val tables: ArrayList<String> = getTablesNames(databaseMetadata, schemas)

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun getTablesNames(databaseMetadata: DatabaseMetaData, schemas: java.util.ArrayList<String>): ArrayList<String> {
        val result = ArrayList<String>()
        schemas.forEach { schemaName ->
            databaseMetadata.getTables(null, schemaName, null, arrayOf("TABLE")).use { tablesResultSet ->
                while (tablesResultSet.next()) {
                    val tableName = tablesResultSet.getString("TABLE_NAME")
                    result.add(tableName)
                }
            }
        }

        return result
    }

    private fun getSchemasNames(databaseMetadata: DatabaseMetaData): ArrayList<String> {
        val result = ArrayList<String>()
        val schemasExtractor: (ResultSet) -> Unit = { schemasResultSet ->
            while (schemasResultSet.next()) {
                val schemaName = schemasResultSet.getString("TABLE_SCHEM")
                result.add(schemaName)
            }
        }

        if (configurationProperties.schemas.isEmpty()) {
            databaseMetadata.schemas.use(schemasExtractor)
        } else {
            configurationProperties.schemas.forEach { schemaName ->
                databaseMetadata.getSchemas(null, schemaName).use(schemasExtractor)
            }
        }

        return result
    }

    private fun generateServers(): List<Server> {
        val server = Server().url("/")
        return listOf(server)
    }

    private fun generateInfo(): Info {
        return Info()
                .title("dbrestr")
                .version("1.0.0")
    }
}
