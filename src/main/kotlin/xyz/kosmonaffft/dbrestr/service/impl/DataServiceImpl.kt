package xyz.kosmonaffft.dbrestr.service.impl

import org.springframework.stereotype.Service
import xyz.kosmonaffft.dbrestr.service.api.DataService
import xyz.kosmonaffft.dbrestr.service.api.DatabaseMetadataService
import xyz.kosmonaffft.dbrestr.service.api.SqlService
import javax.sql.DataSource

@Service
class DataServiceImpl(private val dataSource: DataSource,
                      private val sqlService: SqlService,
                      private val databaseMetadataService: DatabaseMetadataService) : DataService {

    override fun selectMany(schema: String, table: String, page: Int, size: Int): DataService.SelectManyResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun selectOne(schema: String, table: String, id: List<Any>): Map<String, Any> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}