package com.construtora.financeiro.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que todas as migrations Flyway (V1–V16) são aplicadas corretamente
 * em um banco PostgreSQL real e que as tabelas essenciais existem com os campos
 * esperados.
 */
@Transactional
class MigrationIT extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void allMigrationsApply_tablesExist() {
        // Tabelas core
        assertTableExists("users");
        assertTableExists("roles");
        assertTableExists("permissions");
        assertTableExists("role_permissions");
        assertTableExists("clients");
        assertTableExists("developments");
        assertTableExists("blocks");
        assertTableExists("lots");
        assertTableExists("property_sales");
        assertTableExists("installments");
        assertTableExists("accounts_payable");
        assertTableExists("accounts_receivable");
        assertTableExists("bank_accounts");
        assertTableExists("bank_transactions");
        assertTableExists("reconciliations");
        assertTableExists("categories");
        assertTableExists("cost_centers");
        assertTableExists("system_settings");
        assertTableExists("email_notifications");
        assertTableExists("audit_logs");
    }

    @Test
    void v13_modulesAndLicense() {
        assertTableExists("modules");
        assertTableExists("license");
        int modules = jdbc.queryForObject("select count(*) from modules", Integer.class);
        assertThat(modules).isGreaterThanOrEqualTo(11);
    }

    @Test
    void v14_permissionsUpdated() {
        assertTableExists("refresh_tokens");  // V15 also tested here via V14 + V15
        int viewPerms = jdbc.queryForObject(
                "select count(*) from permissions where code like '%_VIEW'", Integer.class);
        assertThat(viewPerms).isGreaterThanOrEqualTo(9);
        int oldRead = jdbc.queryForObject(
                "select count(*) from permissions where code = 'READ'", Integer.class);
        assertThat(oldRead).isZero();
    }

    @Test
    void v15_refreshTokensAndPaymentMethods() {
        assertTableExists("refresh_tokens");
        assertColumnExists("lots", "reservation_expires_at");
        assertTableExists("payment_methods");
        assertTableExists("correction_indexes");
        int pm = jdbc.queryForObject("select count(*) from payment_methods", Integer.class);
        assertThat(pm).isGreaterThanOrEqualTo(6);
    }

    @Test
    void v16_receivableCategoryAndLogo() {
        assertColumnExists("accounts_receivable", "category_id");
        assertColumnExists("system_settings", "logo_data");
        assertColumnExists("system_settings", "logo_mime");
    }

    // ---- helpers ----

    private void assertTableExists(String table) {
        Integer count = jdbc.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema='public' and table_name=?",
                Integer.class, table);
        assertThat(count).as("tabela '%s' deve existir", table).isEqualTo(1);
    }

    private void assertColumnExists(String table, String column) {
        Integer count = jdbc.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_schema='public' and table_name=? and column_name=?",
                Integer.class, table, column);
        assertThat(count).as("coluna '%s.%s' deve existir", table, column).isEqualTo(1);
    }
}
