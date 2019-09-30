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

import io.swagger.v3.oas.models.*
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.servers.Server
import org.springframework.stereotype.Service
import xyz.kosmonaffft.dbrestr.metadata.DatabaseMetadata

/**
 * @author Anton V. Kirilchik
 * @since 17.09.2019
 */
@Service
class OpenAPIMetadataService(private val databaseMetadataService: DatabaseMetadataService) {

    data class PathsAndComponents(val paths: Paths, val components: Components)

    fun generateOpenApiV3Metadata(): OpenAPI {
        val databaseMetadata = databaseMetadataService.getDatabaseMetadata()
        val (paths, components) = generatePathsAndComponents(databaseMetadata)
        val openAPI = OpenAPI()
                .info(generateInfo())
                .servers(generateServers())
                .components(components)
                .paths(paths)
        return openAPI
    }

    private fun generatePathsAndComponents(databaseMetadata: DatabaseMetadata): PathsAndComponents {
        val components = Components()
        val paths = Paths()

        databaseMetadata.keys.forEach { schemaName ->
            val schemaMetadata = databaseMetadata[schemaName]!!

            schemaMetadata.keys.forEach { tableName ->
                val tableMetadata = schemaMetadata[tableName]!!
                val fullTableName = "$schemaName.$tableName"

                val oaTableSchema = ObjectSchema()
                        .name(fullTableName)
                val oaTableSchemaRef = "#/components/schemas/$fullTableName"

                tableMetadata.allColumns.forEach { columnMetadata ->
                    val columnFormat = jdbcToOpenApiFormat(columnMetadata.jdbcType.name)

                    val oaColumnSchema = Schema<Any>()
                            .title(columnMetadata.name)
                            .type(jdbcToOpenApiType(columnMetadata.jdbcType.name))
                            .nullable(columnMetadata.nullable)
                    if (columnFormat != null) {
                        oaColumnSchema.format(columnFormat)
                    }

                    oaTableSchema.addProperties(columnMetadata.name, oaColumnSchema)
                }

                components.addSchemas(fullTableName, oaTableSchema)

                val oaSingleResponse = ApiResponse()
                        .description("One record from $fullTableName table.")
                        .content(Content().addMediaType("application/json", MediaType().schema(ObjectSchema().`$ref`(oaTableSchemaRef))))
                val oaSingleResponseName = fullTableName

                val oaListResponse = ApiResponse()
                        .description("List of records from $fullTableName table.")
                        //.addHeaderObject(TOTAL_COUNT_HEADER_NAME, TOTAL_HEADER)
                        .content(Content().addMediaType("application/json", MediaType().schema(ArraySchema().items(ObjectSchema().`$ref`(oaTableSchemaRef)))))
                val oaListResponseName = "$fullTableName.list"

                components.addResponses(oaSingleResponseName, oaSingleResponse)
                components.addResponses(oaListResponseName, oaListResponse)

                val oaInsertRequestBody = RequestBody()
                        .description(String.format("Insert record into %s table.", fullTableName))
                        .content(Content().addMediaType("application/json", MediaType().schema(ObjectSchema().`$ref`(oaTableSchemaRef))))
                val oaInsertRequestBodyName = "insert.$fullTableName"

                val oaUpdateRequestBody = RequestBody()
                        .description(String.format("Update record from %s table.", fullTableName))
                        .content(Content().addMediaType("application/json", MediaType().schema(ObjectSchema().`$ref`(oaTableSchemaRef))))
                val oaUpdateRequestBodyName = "update.$fullTableName"

                components.addRequestBodies(oaInsertRequestBodyName, oaInsertRequestBody)
                components.addRequestBodies(oaUpdateRequestBodyName, oaUpdateRequestBody)

                val oaGetListOperationName = "list.$fullTableName"
                val oaGetListOperation = Operation()
                        .operationId(oaGetListOperationName)
//                                .addParametersItem(Parameter().`$ref`(parameterRef(PAGE_PARAMETER_NAME)))
//                                .addParametersItem(Parameter().`$ref`(parameterRef(SIZE_PARAMETER_NAME)))
//                                .addParametersItem(Parameter().`$ref`(parameterRef(ORDER_PARAMETER_NAME)))
//                                .addParametersItem(Parameter().`$ref`(parameterRef(FILTER_PARAMETER_NAME)))
                        .responses(ApiResponses()
                                .addApiResponse("200", ApiResponse().`$ref`("#/components/responses/$oaListResponseName"))
                        )

                val oaInsertOperationName = "insert.$fullTableName"
                val oaInsertOperation = Operation()
                        .operationId(oaInsertOperationName)
                        .requestBody(RequestBody().`$ref`("#/components/requestBodies/$oaInsertRequestBodyName"))
                        .responses(ApiResponses()
                                .addApiResponse("200", ApiResponse().`$ref`("#/components/responses/$oaSingleResponseName"))
                        )

                val oaListPathItem = PathItem()
                        .get(oaGetListOperation)
                        .post(oaInsertOperation)
                paths.addPathItem("/$fullTableName", oaListPathItem)
            }
        }

        return PathsAndComponents(paths, components)
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
    return when (jdbcType.toLowerCase()) {
        "text", "bytea", "varchar", "binary", "date", "timestamp", "timestamptz", "json", "jsonb", "uuid" -> "string"

        "int4", "int8", "integer", "bigint", "serial", "bigserial" -> "integer"

        "float4", "float8", "double" -> "number"

        "bool" -> "boolean"

        else -> jdbcType
    }
}

fun jdbcToOpenApiFormat(jdbcType: String): String? {
    return when (jdbcType.toLowerCase()) {
        "int4", "serial", "int" -> "int32"

        "int8", "bigserial", "bigint" -> "int64"

        "float4" -> "float"

        "float8", "double" -> "double"

        "bytea", "binary" -> "binary"

        "date" -> "date"

        "timestamp", "timestamptz" -> "date-time"

        "uuid" -> "uuid"

        else -> null
    }
}
