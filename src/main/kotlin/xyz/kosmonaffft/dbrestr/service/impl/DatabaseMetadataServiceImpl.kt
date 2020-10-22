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

import com.google.common.base.Supplier
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import xyz.kosmonaffft.dbrestr.configuration.ConfigurationProperties
import xyz.kosmonaffft.dbrestr.metadata.ColumnMetadata
import xyz.kosmonaffft.dbrestr.metadata.DatabaseMetadata
import xyz.kosmonaffft.dbrestr.metadata.SchemaMetadata
import xyz.kosmonaffft.dbrestr.metadata.TableMetadata
import xyz.kosmonaffft.dbrestr.service.api.DatabaseMetadataService
import java.sql.Connection
import java.sql.JDBCType
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import javax.sql.DataSource
import kotlin.collections.HashMap

class DatabaseMetadataServiceImpl(private val dataSource: DataSource,
                                  private val configurationProperties: ConfigurationProperties) : DatabaseMetadataService {

    private val metadataCache: LoadingCache<String, DatabaseMetadata> = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.of(1, ChronoUnit.HOURS))
            .build(CacheLoader.from(Supplier { this.generateDatabaseMetadata() }))

    override fun getDatabaseMetadata(): DatabaseMetadata {
        return metadataCache["metadata"];
    }

    private fun generateDatabaseMetadata(): DatabaseMetadata {
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
                val comment = tablesResultSet.getString("REMARKS")
                val tableMetadata = generateTableMetadata(connection, schemaName, tableName, comment)
                result[tableName] = tableMetadata
            }
        }

        return result
    }

    private fun generateTableMetadata(connection: Connection, schemaName: String, tableName: String, tableComment: String?): TableMetadata {
        val allColumnsMap = HashMap<String, ColumnMetadata>()
        val allColumns = ArrayList<ColumnMetadata>()
        connection.metaData.getColumns(null, schemaName, tableName, null).use { columnsResultSet ->
            while (columnsResultSet.next()) {
                val columnName = columnsResultSet.getString("COLUMN_NAME")
                val columnType = columnsResultSet.getString("TYPE_NAME")
                val columnNullable = columnsResultSet.getShort("NULLABLE") != 0.toShort()
                val comment = columnsResultSet.getString("REMARKS")
                val autoIncremented = columnsResultSet.getString("IS_AUTOINCREMENT") == "YES"
                val columnMetadata = ColumnMetadata(columnName, columnType, columnNullable, autoIncremented, comment)
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

        return TableMetadata(allColumns, primaryKeys, tableComment)
    }
}
