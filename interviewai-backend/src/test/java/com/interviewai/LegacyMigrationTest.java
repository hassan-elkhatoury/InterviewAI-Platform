package com.interviewai;

import static org.assertj.core.api.Assertions.*;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class LegacyMigrationTest {
  @Test
  void baseliningAnExistingSchemaPreservesDataAndMapsOnlyFullAdministrators() throws Exception {
    String url =
        "jdbc:h2:mem:upgrade"
            + UUID.randomUUID()
            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    Flyway.configure().dataSource(url, "sa", "").target("1").load().migrate();
    try (var connection = DriverManager.getConnection(url, "sa", "");
        var sql = connection.createStatement()) {
      sql.execute(
          "INSERT INTO"
              + " users(id,username,password_hash,role,last_daily_quest_claim,last_monthly_quest_claim)"
              + " VALUES"
              + " (1,'legacy-admin','original-hash','CANDIDATE','2026-11-13','2026-11-13'),(2,'legacy-content','another-hash','CANDIDATE',NULL,NULL)");
      sql.execute("INSERT INTO roles(id,name) VALUES (1,'SUPER_ADMIN'),(2,'CONTENT_ADMIN')");
      sql.execute("INSERT INTO user_roles(user_id,role_id) VALUES(1,1),(2,2)");
      sql.execute(
          "INSERT INTO generated_courses(id,user_id,course_title) VALUES (1,1,'Existing course')");
      sql.execute(
          "INSERT INTO chapters(id,course_id,chapter_number,name) VALUES (1,1,1,'Existing"
              + " lesson')");
      sql.execute("INSERT INTO progress(user_id,course_id,xp) VALUES (1,1,500)");
      sql.execute("DROP TABLE flyway_schema_history");
    }
    Flyway.configure()
        .dataSource(url, "sa", "")
        .baselineOnMigrate(true)
        .baselineVersion("1")
        .load()
        .migrate();
    try (var connection = DriverManager.getConnection(url, "sa", "");
        var sql = connection.createStatement()) {
      try (var rs = sql.executeQuery("SELECT role,password_hash FROM users WHERE id=1")) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString(1)).isEqualTo("ROLE_ADMIN");
        assertThat(rs.getString(2)).isEqualTo("original-hash");
      }
      try (var rs = sql.executeQuery("SELECT role FROM users WHERE id=2")) {
        rs.next();
        assertThat(rs.getString(1)).isEqualTo("ROLE_USER");
      }
      try (var rs = sql.executeQuery("SELECT SUM(xp) FROM progress")) {
        rs.next();
        assertThat(rs.getInt(1)).isEqualTo(500);
      }
      try (var rs = sql.executeQuery("SELECT COUNT(*) FROM quest_claims")) {
        rs.next();
        assertThat(rs.getInt(1)).isEqualTo(2);
      }
      try (var rs = sql.executeQuery("SELECT course_title FROM generated_courses WHERE id=1")) {
        rs.next();
        assertThat(rs.getString(1)).isEqualTo("Existing course");
      }
    }
  }
}
