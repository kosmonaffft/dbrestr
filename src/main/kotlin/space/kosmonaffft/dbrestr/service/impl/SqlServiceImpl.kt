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

package space.kosmonaffft.dbrestr.service.impl

import space.kosmonaffft.dbrestr.metadata.DatabaseMetadata
import space.kosmonaffft.dbrestr.service.api.DatabaseMetadataService
import space.kosmonaffft.dbrestr.service.api.SqlService
import java.lang.String.join
import java.util.Collections.nCopies
import java.util.stream.Collectors

class SqlServiceImpl(private val databaseMetadataService: DatabaseMetadataService) : SqlService {

    override fun selectMany(schema: String, table: String, offset: Long, limit: Long): String {
        val sql = StringBuilder()
                .append("SELECT * FROM ")
                .append(schema)
                .append(".")
                .append(table)

        // TODO: Replace joining to parameters!!!
        sql.append(" LIMIT ")
                .append(limit)
                .append(" OFFSET ")
                .append(offset)
                .append(";")
        return sql.toString()
    }

    override fun count(schema: String, table: String): String {
        val sql = StringBuilder()
                .append("SELECT count(*) FROM ")
                .append(schema)
                .append(".")
                .append(table)

        sql.append(";")
        return sql.toString()
    }

    override fun selectOne(schema: String, table: String, idColumns: List<String>): String {
        val where = idColumns.stream()
                .map { "$it = ?" }
                .collect(joiningCollector())

        return "SELECT * FROM $schema.$table WHERE $where;"
    }

    override fun insert(schema: String, table: String, actualColumns: Set<String>): String {
        val metaData = databaseMetadataService.getDatabaseMetadata()
        val columnsList = metaData[schema]!![table]!!.allColumns.stream()
                .map { it.name }
                .filter { actualColumns.contains(it) }
                .collect(joiningCollector())

        val valuesList = join(", ", nCopies(actualColumns.size, "?"))
        return "INSERT INTO $schema.$table ($columnsList) VALUES ($valuesList);"
    }

    override fun update(schema: String, table: String, idColumns: List<String>, databaseMetadata: DatabaseMetadata, actualColumns: Set<String>): String {
        val set = databaseMetadata[schema]!![table]!!.allColumns.stream()
                .map { it.name }
                .filter { actualColumns.contains(it) }
                .map { "$it = ?" }
                .collect(joiningCollector())

        val where = idColumns.stream()
                .map { "$it = ?" }
                .collect(joiningCollector())

        return "UPDATE $schema.$table SET $set WHERE $where;"
    }

    override fun delete(schema: String, table: String, idColumns: List<String>): String {
        val where = idColumns.stream()
                .map { "$it = ?" }
                .collect(joiningCollector())

        return "DELETE FROM $schema.$table WHERE $where;"
    }
}

private fun joiningCollector() = Collectors.joining(", ")
