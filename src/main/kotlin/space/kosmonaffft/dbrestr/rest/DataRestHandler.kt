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

package space.kosmonaffft.dbrestr.rest

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import space.kosmonaffft.dbrestr.service.api.DataService
import space.kosmonaffft.dbrestr.service.api.OpenApiMetadataService.Companion.PAGE_PARAMETER_NAME
import space.kosmonaffft.dbrestr.service.api.OpenApiMetadataService.Companion.PAGE_SIZE_PARAMETER_NAME
import space.kosmonaffft.dbrestr.service.api.OpenApiMetadataService.Companion.TOTAL_HEADER_NAME
import javax.servlet.http.HttpServletResponse

@RestController
class DataRestHandler(private val dataService: DataService) {

    @GetMapping(path = ["/data/{schema}/{table}"], produces = ["application/json"])
    fun selectMany(@PathVariable("schema") schema: String,
                   @PathVariable("table") table: String,
                   @RequestParam(PAGE_PARAMETER_NAME, defaultValue = "0") page: Long,
                   @RequestParam(PAGE_SIZE_PARAMETER_NAME, defaultValue = "25") size: Long,
                   response: HttpServletResponse): List<Map<String, Any>> {

        val (result, count) = dataService.selectMany(schema, table, page, size)
        response.addHeader(TOTAL_HEADER_NAME, count.toString())
        return result
    }

    @GetMapping(path = ["/data/{schema}/{table}/{ids}"], produces = ["application/json"])
    fun selectOne(@PathVariable("schema") schema: String,
                  @PathVariable("table") table: String,
                  @PathVariable("ids") ids: String): Map<String, Any> {

        val result = dataService.selectOne(schema, table, ids)
        return result
    }

    @PostMapping(path = ["/data/{schema}/{table}"], produces = ["application/json"])
    fun insert(@PathVariable("schema") schema: String,
               @PathVariable("table") table: String,
               @RequestBody record: Map<String, Any>): Map<String, Any> {

        val result = dataService.insert(schema, table, record)
        return result
    }

    @PutMapping(path = ["/data/{schema}/{table}/{ids}"], produces = ["application/json"])
    fun update(@PathVariable("schema") schema: String,
               @PathVariable("table") table: String,
               @PathVariable("ids") ids: String,
               @RequestBody record: Map<String, Any>): Map<String, Any> {

        val result = dataService.update(schema, table, ids, record)
        return result
    }

    @DeleteMapping(path = ["/data/{schema}/{table}/{ids}"])
    fun delete(@PathVariable("schema") schema: String,
               @PathVariable("table") table: String,
               @PathVariable("ids") ids: String,
               response: HttpServletResponse) {

        dataService.delete(schema, table, ids)
        response.status = HttpStatus.NO_CONTENT.value();
    }
}
