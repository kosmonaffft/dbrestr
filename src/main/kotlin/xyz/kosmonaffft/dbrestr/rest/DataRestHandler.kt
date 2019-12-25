package xyz.kosmonaffft.dbrestr.rest

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import xyz.kosmonaffft.dbrestr.service.api.DataService
import xyz.kosmonaffft.dbrestr.service.api.OpenApiMetadataService.Companion.PAGE_PARAMETER_NAME
import xyz.kosmonaffft.dbrestr.service.api.OpenApiMetadataService.Companion.PAGE_SIZE_PARAMETER_NAME
import xyz.kosmonaffft.dbrestr.service.api.OpenApiMetadataService.Companion.TOTAL_HEADER_NAME
import javax.servlet.http.HttpServletResponse

@RestController
class DataRestHandler(private val dataService: DataService) {

    @GetMapping(path = ["/data/{schema}/{table}"], produces = ["application/json"])
    fun selectMany(@PathVariable("schema") schema: String,
                   @PathVariable("table") table: String,
                   @RequestParam(PAGE_PARAMETER_NAME, defaultValue = "0") page: Int,
                   @RequestParam(PAGE_SIZE_PARAMETER_NAME, defaultValue = "25") size: Int,
                   response: HttpServletResponse): List<Map<String, Any>> {

        val (result, count) = dataService.selectMany(schema, table, page, size)
        response.setHeader(TOTAL_HEADER_NAME, count.toString())
        return result
    }
}