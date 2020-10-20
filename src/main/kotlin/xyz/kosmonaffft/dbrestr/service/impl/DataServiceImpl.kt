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

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import xyz.kosmonaffft.dbrestr.service.api.DataService
import xyz.kosmonaffft.dbrestr.service.api.DatabaseMetadataService
import xyz.kosmonaffft.dbrestr.service.api.SqlService
import javax.sql.DataSource

/**
 * @author Anton V. Kirilchik
 * @since 25.12.2019
 */
@Service
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

    override fun selectOne(schema: String, table: String, id: List<Any>): Map<String, Any> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}