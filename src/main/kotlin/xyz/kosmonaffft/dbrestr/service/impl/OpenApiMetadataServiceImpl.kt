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

package xyz.kosmonaffft.dbrestr.service.impl

import io.swagger.v3.oas.models.*
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.PathParameter
import io.swagger.v3.oas.models.parameters.QueryParameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.servers.Server
import xyz.kosmonaffft.dbrestr.metadata.DatabaseMetadata
import xyz.kosmonaffft.dbrestr.service.api.DatabaseMetadataService
import xyz.kosmonaffft.dbrestr.service.api.OpenApiMetadataService
import xyz.kosmonaffft.dbrestr.service.api.OpenApiMetadataService.Companion.PAGE_PARAMETER_NAME
import xyz.kosmonaffft.dbrestr.service.api.OpenApiMetadataService.Companion.PAGE_SIZE_PARAMETER_NAME
import xyz.kosmonaffft.dbrestr.service.api.OpenApiMetadataService.Companion.TOTAL_HEADER_NAME
import java.sql.JDBCType
import java.util.stream.Collectors

class OpenApiMetadataServiceImpl(private val databaseMetadataService: DatabaseMetadataService) : OpenApiMetadataService {

    override fun generateOpenApiV3Metadata(): OpenAPI {
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

        val oaTotalHeaderName = "totalHeader"
        components.addHeaders(oaTotalHeaderName, Header().schema(IntegerSchema().format("int64")))

        components.addParameters(PAGE_SIZE_PARAMETER_NAME,
                QueryParameter()
                        .name(PAGE_SIZE_PARAMETER_NAME)
                        .required(false)
                        .schema(IntegerSchema().format("int64")))

        components.addParameters(PAGE_PARAMETER_NAME,
                QueryParameter()
                        .name(PAGE_PARAMETER_NAME)
                        .required(false)
                        .schema(IntegerSchema().format("int64")))

        val oaErrorSchemaName = "error"
        val oaErrorSchema = ObjectSchema()
                .addProperties("message", StringSchema())

        val oaErrorResponseName = "error"
        val oaErrorResponse = ApiResponse()
                .description("Error response.")
                .content(Content().addMediaType("application/json", MediaType().schema(ObjectSchema().`$ref`(oaErrorSchemaName))))

        components.addSchemas(oaErrorSchemaName, oaErrorSchema)
        components.addResponses(oaErrorResponseName, oaErrorResponse)

        databaseMetadata.keys.forEach { schemaName ->
            val schemaMetadata = databaseMetadata[schemaName]!!

            schemaMetadata.keys.forEach { tableName ->
                val tableMetadata = schemaMetadata[tableName]!!

                val fullTableName = "$schemaName.$tableName"

                val tableSchemaName = "$fullTableName.schema"
                val insertTableSchemaName = "$fullTableName.insert.schema"

                val oaTableSchema = ObjectSchema()
                        .name(tableSchemaName)
                val oaInsertTableSchema = ObjectSchema()
                        .name(insertTableSchemaName)

                tableMetadata.comment?.also {
                    oaTableSchema.description = it
                    oaInsertTableSchema.description = it
                }

                val oaTableSchemaRef = "#/components/schemas/$tableSchemaName"
                val oaInsertTableSchemaRef = "#/components/schemas/$insertTableSchemaName"

                tableMetadata.allColumns.forEach { columnMetadata ->
                    val oaType = jdbcToOpenApiType(columnMetadata.type)
                    val oaFormat = jdbcToOpenApiFormat(columnMetadata.type)
                    val columnSchema = Schema<Any>()
                            .title(columnMetadata.name)
                            .type(oaType)
                            .nullable(columnMetadata.nullable)
                    oaFormat?.also { columnSchema.format = it }
                    columnMetadata.comment?.also { columnSchema.description = it }

                    if (!columnMetadata.nullable && !columnMetadata.autoIncremented) {
                        oaInsertTableSchema.addRequiredItem(columnMetadata.name)
                    }

                    oaTableSchema.addProperties(columnMetadata.name, columnSchema)
                    oaInsertTableSchema.addProperties(columnMetadata.name, columnSchema)
                }

                components.addSchemas(tableSchemaName, oaTableSchema)
                components.addSchemas(insertTableSchemaName, oaInsertTableSchema)

                val oaSingleResponse = ApiResponse()
                        .description("One record from '$fullTableName' table.")
                        .content(Content().addMediaType("application/json", MediaType().schema(ObjectSchema().`$ref`(oaTableSchemaRef))))
                val oaSingleResponseName = "$fullTableName.row.result"

                val oaListResponse = ApiResponse()
                        .description("List of records from '$fullTableName' table.")
                        .addHeaderObject(TOTAL_HEADER_NAME, Header().`$ref`(oaTotalHeaderName))
                        .content(Content().addMediaType("application/json", MediaType().schema(ArraySchema().items(ObjectSchema().`$ref`(oaTableSchemaRef)))))
                val oaListResponseName = "$fullTableName.list.result"

                components.addResponses(oaSingleResponseName, oaSingleResponse)
                components.addResponses(oaListResponseName, oaListResponse)

                val oaInsertRequestBody = RequestBody()
                        .description("Insert record into '$fullTableName' table.")
                        .content(Content().addMediaType("application/json", MediaType().schema(ObjectSchema().`$ref`(oaInsertTableSchemaRef))))
                val oaInsertRequestBodyName = "$fullTableName.insert.body"

                val oaUpdateRequestBody = RequestBody()
                        .description("Update record in '$fullTableName' table.")
                        .content(Content().addMediaType("application/json", MediaType().schema(ObjectSchema().`$ref`(oaTableSchemaRef))))
                val oaUpdateRequestBodyName = "$fullTableName.update.body"

                components.addRequestBodies(oaInsertRequestBodyName, oaInsertRequestBody)
                components.addRequestBodies(oaUpdateRequestBodyName, oaUpdateRequestBody)

                val oaGetListOperationName = "$fullTableName.get.list.operation"
                val oaGetListOperation = Operation()
                        .operationId(oaGetListOperationName)
                        .addParametersItem(Parameter().`$ref`(PAGE_SIZE_PARAMETER_NAME))
                        .addParametersItem(Parameter().`$ref`(PAGE_PARAMETER_NAME))
//                                .addParametersItem(Parameter().`$ref`(parameterRef(ORDER_PARAMETER_NAME)))
//                                .addParametersItem(Parameter().`$ref`(parameterRef(FILTER_PARAMETER_NAME)))
                        .responses(ApiResponses()
                                .addApiResponse("200", ApiResponse().`$ref`("#/components/responses/$oaListResponseName"))
                                .addApiResponse("500", ApiResponse().`$ref`(oaErrorResponseName))
                        )

                val oaInsertOperationName = "$fullTableName.insert.operation"
                val oaInsertOperation = Operation()
                        .operationId(oaInsertOperationName)
                        .requestBody(RequestBody().`$ref`("#/components/requestBodies/$oaInsertRequestBodyName"))
                        .responses(ApiResponses()
                                .addApiResponse("200", ApiResponse().`$ref`("#/components/responses/$oaSingleResponseName"))
                                .addApiResponse("500", ApiResponse().`$ref`(oaErrorResponseName))
                        )

                val fullTablePath = "$schemaName/$tableName"
                val listPath = "/data/$fullTablePath"

                val oaListPathItem = PathItem()
                        .get(oaGetListOperation)
                        .post(oaInsertOperation)

                paths.addPathItem(listPath, oaListPathItem)

                val oaGetOneOperationName = "$fullTableName.get.one.operation"
                val oaGetSingleOperation = Operation()
                        .operationId(oaGetOneOperationName)
                        .responses(ApiResponses()
                                .addApiResponse("200", ApiResponse().`$ref`("#/components/responses/$oaSingleResponseName"))
                                .addApiResponse("500", ApiResponse().`$ref`(oaErrorResponseName))
                        )

                val oaUpdateOperationName = "$fullTableName.update.operation"
                val oaUpdateOperation = Operation()
                        .operationId(oaUpdateOperationName)
                        .requestBody(RequestBody().`$ref`("#/components/requestBodies/$oaUpdateRequestBodyName"))
                        .responses(ApiResponses()
                                .addApiResponse("200", ApiResponse().`$ref`("#/components/responses/$oaSingleResponseName"))
                                .addApiResponse("500", ApiResponse().`$ref`(oaErrorResponseName))
                        )

                val oaDeleteOperationName = "$fullTableName.delete.operation"
                val oaDeleteOperation = Operation()
                        .operationId(oaDeleteOperationName)
                        .responses(ApiResponses()
                                .addApiResponse("204", ApiResponse().description("Successfully deleted record from '$fullTableName' table."))
                                .addApiResponse("500", ApiResponse().`$ref`(oaErrorResponseName))
                        )

                val keysPathParts = mutableListOf<String>()
                tableMetadata.primaryKeys.forEach { pk ->
                    val oaIdPathParameterName = "$fullTableName.${pk.name}.parameter"
                    val oaIdPathParameterRef = "#/components/parameters/$oaIdPathParameterName"
                    val oaType = jdbcToOpenApiType(pk.type)
                    val oaFormat = jdbcToOpenApiFormat(pk.type)
                    val oaIdPathParameter = PathParameter()
                            .name(pk.name)
                            .description(pk.name)
                            .schema(Schema<Any>().type(oaType))
                            .required(true)
                            .allowEmptyValue(false)
                    oaFormat?.also { oaIdPathParameter.schema.format = it }

                    components.addParameters(oaIdPathParameterName, oaIdPathParameter)
                    oaGetSingleOperation.addParametersItem(Parameter().`$ref`(oaIdPathParameterRef))
                    oaUpdateOperation.addParametersItem(Parameter().`$ref`(oaIdPathParameterRef))
                    oaDeleteOperation.addParametersItem(Parameter().`$ref`(oaIdPathParameterRef))
                    keysPathParts.add("{${pk.name}}")
                }
                val keyParamString = keysPathParts.stream().collect(Collectors.joining("/"))
                val singlePath = "/data/$fullTablePath/$keyParamString"

                val oaSinglePathItem = PathItem()
                        .get(oaGetSingleOperation)
                        .put(oaUpdateOperation)
                        .delete(oaDeleteOperation)

                paths.addPathItem(singlePath, oaSinglePathItem)
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

    private data class PathsAndComponents(val paths: Paths, val components: Components)
}

private fun jdbcToOpenApiType(jdbcType: String): String {
    return when (jdbcType.toLowerCase()) {
        "text", "bytea", "varchar", "binary", "date", "timestamp", "timestamptz", "json", "jsonb", "uuid" -> "string"

        "int4", "int8", "integer", "bigint", "serial", "bigserial" -> "integer"

        "float4", "float8", "double" -> "number"

        "bool", "bit" -> "boolean"

        else -> jdbcType
    }
}

private fun jdbcToOpenApiFormat(jdbcType: String): String? {
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
