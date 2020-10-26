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

package xyz.kosmonaffft.dbrestr.service.api

interface DataService {

    data class SelectManyResult(val data: List<Map<String, Any>>, val count: Long)

    fun selectMany(schema: String, table: String, page: Long, size: Long): SelectManyResult

    fun selectOne(schema: String, table: String, id: String): Map<String, Any>

    fun delete(schema: String, table: String, id: String)
}
