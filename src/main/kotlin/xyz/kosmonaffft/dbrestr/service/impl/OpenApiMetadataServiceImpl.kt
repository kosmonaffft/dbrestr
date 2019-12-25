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
import org.springframework.stereotype.Service
import xyz.kosmonaffft.dbrestr.metadata.DatabaseMetadata
import xyz.kosmonaffft.dbrestr.service.api.OpenApiMetadataService
import xyz.kosmonaffft.dbrestr.service.api.OpenApiMetadataService.Companion.PAGE_PARAMETER_NAME
import xyz.kosmonaffft.dbrestr.service.api.OpenApiMetadataService.Companion.PAGE_SIZE_PARAMETER_NAME
import xyz.kosmonaffft.dbrestr.service.api.OpenApiMetadataService.Companion.TOTAL_HEADER_NAME
import java.util.stream.Collectors

/**
 * @author Anton V. Kirilchik
 * @since 17.09.2019
 */
@Service
class OpenApiMetadataServiceImpl(private val databaseMetadataService: DatabaseMetadataServiceImpl) : OpenApiMetadataService {

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
                        .required(false)
                        .schema(IntegerSchema().format("int64")))

        components.addParameters(PAGE_PARAMETER_NAME,
                QueryParameter()
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
                val fullTablePath = "$schemaName/$tableName"

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
                        .description("One record from '$fullTableName' table.")
                        .content(Content().addMediaType("application/json", MediaType().schema(ObjectSchema().`$ref`(oaTableSchemaRef))))
                val oaSingleResponseName = fullTableName

                val oaListResponse = ApiResponse()
                        .description("List of records from '$fullTableName' table.")
                        .addHeaderObject(TOTAL_HEADER_NAME, Header().`$ref`(oaTotalHeaderName))
                        .content(Content().addMediaType("application/json", MediaType().schema(ArraySchema().items(ObjectSchema().`$ref`(oaTableSchemaRef)))))
                val oaListResponseName = "$fullTableName.list"

                components.addResponses(oaSingleResponseName, oaSingleResponse)
                components.addResponses(oaListResponseName, oaListResponse)

                val oaInsertRequestBody = RequestBody()
                        .description("Insert record into '$fullTableName' table.")
                        .content(Content().addMediaType("application/json", MediaType().schema(ObjectSchema().`$ref`(oaTableSchemaRef))))
                val oaInsertRequestBodyName = "insert.$fullTableName"

                val oaUpdateRequestBody = RequestBody()
                        .description("Update record in '$fullTableName' table.")
                        .content(Content().addMediaType("application/json", MediaType().schema(ObjectSchema().`$ref`(oaTableSchemaRef))))
                val oaUpdateRequestBodyName = "update.$fullTableName"

                components.addRequestBodies(oaInsertRequestBodyName, oaInsertRequestBody)
                components.addRequestBodies(oaUpdateRequestBodyName, oaUpdateRequestBody)

                val oaGetListOperationName = "list.$fullTableName"
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

                val oaInsertOperationName = "insert.$fullTableName"
                val oaInsertOperation = Operation()
                        .operationId(oaInsertOperationName)
                        .requestBody(RequestBody().`$ref`("#/components/requestBodies/$oaInsertRequestBodyName"))
                        .responses(ApiResponses()
                                .addApiResponse("200", ApiResponse().`$ref`("#/components/responses/$oaSingleResponseName"))
                                .addApiResponse("500", ApiResponse().`$ref`(oaErrorResponseName))
                        )

                val listPath = "/data/$fullTablePath"

                val oaListPathItem = PathItem()
                        .get(oaGetListOperation)
                        .post(oaInsertOperation)

                paths.addPathItem(listPath, oaListPathItem)

                val oaGetOneOperationName = "getOne.$fullTableName"
                val oaGetSingleOperation = Operation()
                        .operationId(oaGetOneOperationName)
                        .responses(ApiResponses()
                                .addApiResponse("200", ApiResponse().`$ref`("#/components/responses/$oaSingleResponseName"))
                                .addApiResponse("500", ApiResponse().`$ref`(oaErrorResponseName))
                        )

                val oaUpdateOperationName = "update.$fullTableName"
                val oaUpdateOperation = Operation()
                        .operationId(oaUpdateOperationName)
                        .requestBody(RequestBody().`$ref`("#/components/requestBodies/$oaUpdateRequestBodyName"))
                        .responses(ApiResponses()
                                .addApiResponse("200", ApiResponse().`$ref`("#/components/responses/$oaSingleResponseName"))
                                .addApiResponse("500", ApiResponse().`$ref`(oaErrorResponseName))
                        )

                val oaDeleteOperationName = "delete.$fullTableName"
                val oaDeleteOperation = Operation()
                        .operationId(oaDeleteOperationName)
                        .responses(ApiResponses()
                                .addApiResponse("204", ApiResponse())
                                .addApiResponse("500", ApiResponse().`$ref`(oaErrorResponseName))
                        )

                val keysPathPart = mutableListOf<String>()
                tableMetadata.primaryKeys.forEach { pk ->
                    val oaIdPathParameterName = "$fullTableName.${pk.name}"
                    val oaIdPathParameter = PathParameter()
                            .name(pk.name)
                            .description(pk.name)
                            .schema(Schema<Any>().type(jdbcToOpenApiType(pk.jdbcType.name)))
                            .required(true)
                            .allowEmptyValue(false)
                    val format = jdbcToOpenApiFormat(pk.jdbcType.name)
                    if (format != null) {
                        oaIdPathParameter.schema.format = format
                    }
                    components.addParameters(oaIdPathParameterName, oaIdPathParameter)
                    oaGetSingleOperation.addParametersItem(Parameter().`$ref`(oaIdPathParameterName))
                    oaUpdateOperation.addParametersItem(Parameter().`$ref`(oaIdPathParameterName))
                    oaDeleteOperation.addParametersItem(Parameter().`$ref`(oaIdPathParameterName))
                    keysPathPart.add("{${pk.name}}")
                }
                val keyParamString = keysPathPart.stream().collect(Collectors.joining(", "))
                val singlePath = "/data/$fullTablePath($keyParamString)"

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

        "bool" -> "boolean"

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
