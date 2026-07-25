package org.tales.infrastructure.datasource

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import org.tales.infrastructure.configuration.TalesProperties
import javax.sql.DataSource

@Configuration
class DataSourcesConfiguration(
  private val talesProperties: TalesProperties,
) {
  @Bean("sqliteDataSourceRW")
  @Primary
  fun sqliteDataSourceRW(): DataSource =
    buildDataSource("SqliteMainPoolRW", SqliteUdfDataSource::class.java, talesProperties.database)
      .apply {
        // force pool size to 1 if the pool is only used for writes
        if (talesProperties.database.shouldSeparateReadFromWrites()) this.maximumPoolSize = 1
      }

  @Bean("sqliteDataSourceRO")
  fun sqliteDataSourceRO(): DataSource =
    if (talesProperties.database.shouldSeparateReadFromWrites())
      buildDataSource("SqliteMainPoolRO", SqliteUdfDataSource::class.java, talesProperties.database)
    else
      sqliteDataSourceRW()

  @Bean("tasksDataSourceRW")
  fun tasksDataSourceRW(): DataSource =
    buildDataSource("SqliteTasksPoolRW", SQLiteDataSource::class.java, talesProperties.tasksDb)
      .apply {
        // pool size is always 1:
        // - if there's only 1 pool for read and writes, size should be 1
        // - if there's a separate read pool, the write pool size should be 1
        this.maximumPoolSize = 1
      }

  @Bean("tasksDataSourceRO")
  fun tasksDataSourceRO(): DataSource =
    if (talesProperties.tasksDb.shouldSeparateReadFromWrites())
      buildDataSource("SqliteTasksPoolRO", SQLiteDataSource::class.java, talesProperties.tasksDb)
    else
      tasksDataSourceRW()

  private fun buildDataSource(
    poolName: String,
    dataSourceClass: Class<out SQLiteDataSource>,
    databaseProps: TalesProperties.Database,
  ): HikariDataSource {
    val extraPragmas =
      databaseProps.pragmas.let {
        if (it.isEmpty())
          ""
        else
          "?" + it.map { (key, value) -> "$key=$value" }.joinToString(separator = "&")
      }

    val dataSource =
      DataSourceBuilder
        .create()
        .driverClassName("org.sqlite.JDBC")
        .url("jdbc:sqlite:${databaseProps.file}$extraPragmas")
        .type(dataSourceClass)
        .build()

    with(dataSource) {
      setEnforceForeignKeys(true)
      setGetGeneratedKeys(false)
    }
    with(databaseProps) {
      journalMode?.let { dataSource.setJournalMode(it.name) }
      busyTimeout?.let { dataSource.config.busyTimeout = it.toMillis().toInt() }
    }

    val poolSize =
      if (databaseProps.isMemory())
        1
      else if (databaseProps.poolSize != null)
        databaseProps.poolSize!!
      else
        Runtime.getRuntime().availableProcessors().coerceAtMost(databaseProps.maxPoolSize)

    return HikariDataSource(
      HikariConfig().apply {
        this.dataSource = dataSource
        this.poolName = poolName
        this.maximumPoolSize = poolSize
      },
    )
  }

  fun TalesProperties.Database.isMemory() = file.contains(":memory:") || file.contains("mode=memory")

  fun TalesProperties.Database.shouldSeparateReadFromWrites(): Boolean = !isMemory() && journalMode == SQLiteConfig.JournalMode.WAL
}
