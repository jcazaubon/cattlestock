package com.kinetix.cattlestock.app;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pasa las categorias de una base de la version anterior, donde eran de cada establecimiento, al catalogo de
 * la empresa. Las que se llaman igual (mismo codigo) dentro de una empresa quedan en una sola: el stock y los
 * movimientos siguen valiendo porque referencian la categoria por su codigo. Es idempotente: si la tabla ya no
 * tiene la columna vieja, no hace nada. Corre despues de MigracionEmpresas, que le asigna empresa a los
 * establecimientos.
 */
@Component
@Order(1)
public class MigracionCategorias implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionCategorias.class);

    private final JdbcTemplate jdbc;

    public MigracionCategorias(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean h2 = jdbc.execute((java.sql.Connection c) -> c.getMetaData().getDatabaseProductName())
                .toLowerCase().contains("h2");
        String esquema = h2 ? "schema()" : "database()";
        List<Integer> columna = jdbc.queryForList(
                "select 1 from information_schema.columns where table_schema = " + esquema
                        + " and upper(table_name) = 'CATEGORY' and upper(column_name) = 'ESTABLISHMENT_CODE'",
                Integer.class);
        if (columna.isEmpty()) return;

        // 1) de las repetidas dentro de una empresa (mismo codigo) queda la primera
        // (solo se tocan las categorias cuyo establecimiento ya tiene empresa: nunca se borra a ciegas)
        int repetidas = jdbc.update("delete from category where company_id is null"
                + " and establishment_code in (select code from establishment where company_id is not null)"
                + " and id not in (select id from (select min(c.id) as id from category c"
                + " join establishment e on e.code = c.establishment_code"
                + " where c.company_id is null group by e.company_id, c.code) t)");
        // 2) cada categoria pasa a la empresa de su establecimiento
        int pasadas = jdbc.update("update category set company_id = (select e.company_id from establishment e"
                + " where e.code = category.establishment_code)"
                + " where company_id is null and establishment_code is not null");
        if (repetidas > 0 || pasadas > 0) {
            log.info("Categorias pasadas al catalogo de la empresa: {} (fusionadas por repetidas: {})",
                    pasadas, repetidas);
        }
    }
}
