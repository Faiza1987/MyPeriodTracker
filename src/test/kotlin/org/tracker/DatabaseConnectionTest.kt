package org.tracker


import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import javax.sql.DataSource

@SpringBootTest
class DatabaseConnectionTest @Autowired constructor(
    private val dataSource: DataSource
) {
    @Test
    fun `can connect to database`() {
        dataSource.connection.use { conn ->
            assertFalse(conn.isClosed)
        }
    }
}

