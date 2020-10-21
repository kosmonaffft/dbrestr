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

package xyz.kosmonaffft.dbrestr

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import xyz.kosmonaffft.dbrestr.configuration.ConfigurationProperties
import xyz.kosmonaffft.dbrestr.service.api.DataService
import xyz.kosmonaffft.dbrestr.service.api.DatabaseMetadataService
import xyz.kosmonaffft.dbrestr.service.api.OpenApiMetadataService
import xyz.kosmonaffft.dbrestr.service.api.SqlService
import xyz.kosmonaffft.dbrestr.service.impl.DataServiceImpl
import xyz.kosmonaffft.dbrestr.service.impl.DatabaseMetadataServiceImpl
import xyz.kosmonaffft.dbrestr.service.impl.OpenApiMetadataServiceImpl
import xyz.kosmonaffft.dbrestr.service.impl.SqlServiceImpl
import javax.sql.DataSource

/**
 * @author Anton V. Kirilchik
 * @since 17.09.2019
 */
@SpringBootApplication
@EnableConfigurationProperties(ConfigurationProperties::class)
class DBrestrApplication {

    @Bean
    fun databaseMetadataService(dataSource: DataSource, configurationProperties: ConfigurationProperties): DatabaseMetadataService {
        return DatabaseMetadataServiceImpl(dataSource, configurationProperties)
    }

    @Bean
    fun openApiMetadataService(databaseMetadataService: DatabaseMetadataService): OpenApiMetadataService {
        return OpenApiMetadataServiceImpl(databaseMetadataService)
    }

    @Bean
    fun sqlService(): SqlService {
        return SqlServiceImpl()
    }

    @Bean
    fun dataService(dataSource: DataSource, sqlService: SqlService, databaseMetadataService: DatabaseMetadataService): DataService {
        return DataServiceImpl(dataSource, sqlService, databaseMetadataService)
    }
}

fun main(args: Array<String>) {
    runApplication<DBrestrApplication>(*args)
}
