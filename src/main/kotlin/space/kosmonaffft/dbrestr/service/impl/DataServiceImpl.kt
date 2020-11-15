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

import org.springframework.jdbc.core.JdbcTemplate
import space.kosmonaffft.dbrestr.metadata.ColumnMetadata
import space.kosmonaffft.dbrestr.service.api.DataService
import space.kosmonaffft.dbrestr.service.api.DatabaseMetadataService
import space.kosmonaffft.dbrestr.service.api.SqlService
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.regex.Pattern
import javax.sql.DataSource

class DataServiceImpl(private val dataSource: DataSource,
                      private val sqlService: SqlService,
                      private val databaseMetadataService: DatabaseMetadataService) : DataService {

    override fun selectMany(schema: String, table: String, page: Long, size: Long): DataService.SelectManyResult {
        val selectScript = sqlService.selectMany(schema, table, page * size, (page + 1) * size)
        val countScript = sqlService.count(schema, table)
        val template = JdbcTemplate(dataSource)

        val result = dataSource.connection.use { connection ->
            val data = template.queryForList(selectScript)
            val count = template.queryForObject(countScript, Long::class.java)!!
            DataService.SelectManyResult(data, count)
        }

        return result
    }

    override fun selectOne(schema: String, table: String, id: String): Map<String, Any> {
        val (sql, args, argsTypes) = prepareCallWithId(schema, table, id) { s, t, i -> sqlService.selectOne(s, t, i) }
        val template = JdbcTemplate(dataSource)
        val result = doWithJdbc(sql, args, argsTypes) { s, a, t -> template.queryForMap(s, a, t) }
        return result
    }

    override fun delete(schema: String, table: String, id: String) {
        val (sql, args, argsTypes) = prepareCallWithId(schema, table, id) { s, t, i -> sqlService.delete(s, t, i) }
        val template = JdbcTemplate(dataSource)
        doWithJdbc(sql, args, argsTypes) { s, a, t -> template.update(s, a, t) }
    }

    private fun prepareCallWithId(schema: String,
                                  table: String,
                                  id: String,
                                  sqlCreator: (String, String, List<String>) -> String): Triple<String, Array<Any>, IntArray> {

        val metaData = databaseMetadataService.getDatabaseMetadata()
        val columnsMetadata = metaData[schema]!![table]!!
        val primaryKeysMetadata = columnsMetadata.primaryKeys

        val idColumnsNames = primaryKeysMetadata.map { it.name }
        val sql = sqlCreator(schema, table, idColumnsNames)
        val parsedId = parseId(primaryKeysMetadata, id)

        val args: Array<Any> = idColumnsNames.map {
            parsedId[it]!!
        }.toTypedArray()

        val argsTypes: IntArray = idColumnsNames.mapIndexed { i, _ ->
            columnsMetadata.primaryKeys[i].sqlType
        }.toIntArray()

        return Triple(sql, args, argsTypes)
    }
}

private fun <T> doWithJdbc(sql: String, args: Array<Any>, argsTypes: IntArray, executor: (String, Array<Any>, IntArray) -> T): T {
    return executor(sql, args, argsTypes)
}

private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")
private val DIGIT_PATTERN = Pattern.compile("[0-9]+")

private fun parseId(primaryKeyMetadata: List<ColumnMetadata>, id: String): MutableMap<String, Any> {
    if (primaryKeyMetadata.size == 1) {
        val columnMetadata = primaryKeyMetadata.first()
        return Collections.singletonMap(columnMetadata.name, fromJson(columnMetadata.type, id))
    }

    TODO("Implement composite keys!!!")
}

private fun fromJson(jdbcType: String, jsonString: String): Any {
    val str = jsonString.trim()
    return when (jdbcType) {
        "timestamp" -> {
            if (DIGIT_PATTERN.matcher(str).matches()) {
                return Timestamp(str.toLong())
            }
            val value = OffsetDateTime.parse(str)
            Timestamp.valueOf(value.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime())
        }
        "date" -> Date(DATE_FORMAT.parse(jsonString).getTime())
        "int2", "int4", "serial" -> str.toInt()
        "int8", "bigserial" -> str.toLong()
        "uuid" -> UUID.fromString(str)
        "numeric" -> BigDecimal(str)
        "bool", "boolean" -> str.toBoolean()
        else -> jsonString
    }
}
