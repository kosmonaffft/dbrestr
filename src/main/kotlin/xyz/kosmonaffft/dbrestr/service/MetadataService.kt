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
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.servers.Server
import org.springframework.stereotype.Service
import xyz.kosmonaffft.dbrestr.configuration.ConfigurationProperties
import java.sql.Connection
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
        val result = Components()
        val databaseMetadata = connection.metaData

        val schemaAction: (ResultSet) -> Unit = { schemasResultSet ->
            while (schemasResultSet.next()) {
                val schemaName = schemasResultSet.getString("TABLE_SCHEM")

                databaseMetadata.getTables(null, schemaName, null, arrayOf("TABLE")).use { tablesResultSet ->
                    while (tablesResultSet.next()) {
                        val tableName = tablesResultSet.getString("TABLE_NAME")
                        val fullTableName = "$schemaName/$tableName"

                        val oaTableSchema = ObjectSchema()
                                .name(fullTableName)
                        val oaTableSchemaRef = "#/components/schemas/$fullTableName"

                        databaseMetadata.getColumns(null, schemaName, tableName, null).use { columnsResultSet ->
                            while (columnsResultSet.next()) {
                                val columnName = columnsResultSet.getString("COLUMN_NAME")
                                val columnTypeName = columnsResultSet.getString("TYPE_NAME")
                                val columnNullable = columnsResultSet.getShort("NULLABLE")
                                val columnFormat = jdbcToOpenApiFormat(columnTypeName)

                                val oaColumnSchema = Schema<Any>()
                                        .title(columnName)
                                        .type(jdbcToOpenApiType(columnTypeName))
                                        .nullable(columnNullable != 0.toShort())
                                if (columnFormat != null) {
                                    oaColumnSchema.format(columnFormat)
                                }

                                oaTableSchema.addProperties(columnName, oaColumnSchema)
                            }
                        }

                        result.addSchemas(fullTableName, oaTableSchema)

                        val oaSingleResponse = ApiResponse()
                                .description("One record from $fullTableName table.")
                                .content(Content().addMediaType("application/json", MediaType().schema(ObjectSchema().`$ref`(oaTableSchemaRef))))
                        val oaSingleResponseName = fullTableName

                        val oaListResponse = ApiResponse()
                                .description("List of records from $fullTableName table.")
                                //.addHeaderObject(TOTAL_COUNT_HEADER_NAME, TOTAL_HEADER)
                                .content(Content().addMediaType("application/json", MediaType().schema(ArraySchema().items(ObjectSchema().`$ref`(oaTableSchemaRef)))))
                        val oaListResponseName = "$fullTableName/list"

                        result.addResponses(oaSingleResponseName, oaSingleResponse)
                        result.addResponses(oaListResponseName, oaListResponse)
                    }
                }
            }
        }

        if (configurationProperties.schemas.isEmpty()) {
            databaseMetadata.schemas.use(schemaAction)
        } else {
            configurationProperties.schemas.forEach { schemaName ->
                databaseMetadata.getSchemas(null, schemaName).use(schemaAction)
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

fun jdbcToOpenApiType(jdbcType: String): String {
    return when (jdbcType) {
        "text", "bytea", "varchar", "date", "timestamp", "timestamptz", "json", "jsonb", "uuid" -> "string"

        "int4", "int8", "serial", "bigserial" -> "integer"

        "float4", "float8" -> "number"

        "bool" -> "boolean"

        else -> jdbcType
    }
}

fun jdbcToOpenApiFormat(jdbcType: String): String? {
    return when (jdbcType) {
        "int4", "serial" -> "int32"

        "int8", "bigserial" -> "int64"

        "float4" -> "float"

        "float8" -> "double"

        "bytea" -> "binary"

        "date" -> "date"

        "timestamp", "timestamptz" -> "date-time"

        "uuid" -> "uuid"

        else -> null
    }
}
