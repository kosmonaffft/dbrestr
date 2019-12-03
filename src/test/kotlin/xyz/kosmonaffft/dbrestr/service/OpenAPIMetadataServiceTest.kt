package xyz.kosmonaffft.dbrestr.service

import com.opentable.db.postgres.embedded.FlywayPreparer
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import org.junit.Rule
import org.junit.Test
import xyz.kosmonaffft.dbrestr.configuration.ConfigurationProperties

class OpenAPIMetadataServiceTest {

    @Rule
    @JvmField
    var db = EmbeddedPostgresRules.preparedDatabase(
            FlywayPreparer.forClasspathLocation("migrations"))

    @Test
    fun generateOpenApiV3Metadata() {
        val props = ConfigurationProperties().apply {
            schemas = arrayOf("public")
        }

        val metadataService = DatabaseMetadataService(db.testDatabase, props)
        val openApiService = OpenAPIMetadataService(metadataService)

        val metadata = openApiService.generateOpenApiV3Metadata()
    }
}