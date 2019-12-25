package xyz.kosmonaffft.dbrestr.service.impl

import org.springframework.stereotype.Service
import xyz.kosmonaffft.dbrestr.service.api.DataService
import javax.sql.DataSource

@Service
class DataServiceImpl(private val dataSource: DataSource) : DataService {

    override fun selectMany(schema: String, table: String, page: Int, size: Int): DataService.SelectManyResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}