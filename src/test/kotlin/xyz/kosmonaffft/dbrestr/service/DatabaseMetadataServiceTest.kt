package xyz.kosmonaffft.dbrestr.service

import com.opentable.db.postgres.embedded.FlywayPreparer
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import xyz.kosmonaffft.dbrestr.configuration.ConfigurationProperties

class DatabaseMetadataServiceTest {

    @Rule
    @JvmField
    final var db = EmbeddedPostgresRules.preparedDatabase(
            FlywayPreparer.forClasspathLocation("migrations"))

    @Test
    fun getDatabaseMetadata() {
        val props = ConfigurationProperties().apply {
            schemas = arrayOf("public")
        }

        val metadataService = DatabaseMetadataService(db.testDatabase, props)
        val metadata = metadataService.getDatabaseMetadata()

        Assert.assertTrue(metadata.size == 1)
        Assert.assertTrue(metadata.containsKey("public"))
    }
}