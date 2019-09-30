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

import org.springframework.stereotype.Service
import xyz.kosmonaffft.dbrestr.configuration.ConfigurationProperties
import xyz.kosmonaffft.dbrestr.metadata.ColumnMetadata
import xyz.kosmonaffft.dbrestr.metadata.DatabaseMetadata
import xyz.kosmonaffft.dbrestr.metadata.SchemaMetadata
import xyz.kosmonaffft.dbrestr.metadata.TableMetadata
import java.sql.Connection
import java.sql.JDBCType
import java.util.*
import javax.sql.DataSource
import kotlin.collections.HashMap

/**
 * @author Anton V. Kirilchik
 * @since 17.09.2019
 */
@Service
class DatabaseMetadataService(private val dataSource: DataSource,
                              private val configurationProperties: ConfigurationProperties) {

    fun generateDatabaseMetadata(): DatabaseMetadata {
        val result = DatabaseMetadata()
        val schemas = if (configurationProperties.schemas.isEmpty()) {
            arrayOf<String?>(null)
        } else {
            configurationProperties.schemas
        }

        dataSource.connection.use { connection ->
            schemas.forEach { configurationSchemaName ->
                connection.metaData.getSchemas(null, configurationSchemaName).use { schemasResultSet ->
                    while (schemasResultSet.next()) {
                        val schemaName = schemasResultSet.getString("TABLE_SCHEM")
                        val schemaMetadata = generateSchemaMetadata(connection, schemaName)
                        result[schemaName] = schemaMetadata
                    }
                }
            }
        }

        return result
    }

    private fun generateSchemaMetadata(connection: Connection, schemaName: String): SchemaMetadata {
        val result = SchemaMetadata()
        connection.metaData.getTables(null, schemaName, null, arrayOf("TABLE")).use { tablesResultSet ->
            while (tablesResultSet.next()) {
                val tableName = tablesResultSet.getString("TABLE_NAME")
                val tableMetadata = generateTableMetadata(connection, schemaName, tableName)
                result[tableName] = tableMetadata
            }
        }

        return result
    }

    private fun generateTableMetadata(connection: Connection, schemaName: String, tableName: String): TableMetadata {
        val allColumnsMap = HashMap<String, ColumnMetadata>()
        val allColumns = ArrayList<ColumnMetadata>()
        connection.metaData.getColumns(null, schemaName, tableName, null).use { columnsResultSet ->
            while (columnsResultSet.next()) {
                val columnName = columnsResultSet.getString("COLUMN_NAME")
                val columnType = columnsResultSet.getInt("DATA_TYPE")
                val columnNullable = columnsResultSet.getShort("NULLABLE") != 0.toShort()
                val columnMetadata = ColumnMetadata(columnName, JDBCType.valueOf(columnType), columnNullable)
                allColumns.add(columnMetadata)
                allColumnsMap[columnName] = columnMetadata
            }
        }

        val primaryKeysMap = HashMap<Short, ColumnMetadata>()
        connection.metaData.getPrimaryKeys(null, schemaName, tableName).use { primaryKeysResultSet ->
            while (primaryKeysResultSet.next()) {
                val columnName = primaryKeysResultSet.getString("COLUMN_NAME")
                val columnsPosition = primaryKeysResultSet.getShort("KEY_SEQ")
                val columnMetadata = allColumnsMap[columnName]!!
                primaryKeysMap[columnsPosition] = columnMetadata
            }
        }

        val primaryKeys = primaryKeysMap.keys
                .toList()
                .sorted()
                .map { primaryKeysMap[it]!! }

        return TableMetadata(allColumns, primaryKeys)
    }
}