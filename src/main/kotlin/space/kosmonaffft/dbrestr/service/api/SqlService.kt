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

package space.kosmonaffft.dbrestr.service.api

import space.kosmonaffft.dbrestr.metadata.DatabaseMetadata

interface SqlService {

    fun selectMany(schema: String, table: String, offset: Long, limit: Long): String

    fun count(schema: String, table: String): String

    fun selectOne(schema: String, table: String, idColumns: List<String>): String

    fun insert(schema: String, table: String, actualColumns: Set<String>): String

    fun update(schema: String, table: String, idColumns: List<String>, databaseMetadata: DatabaseMetadata, actualColumns: Set<String>): String

    fun delete(schema: String, table: String, idColumns: List<String>): String
}
