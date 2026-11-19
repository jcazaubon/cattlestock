package com.kinetix.cattlestock.app;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Convierte a VARCHAR las columnas que una version anterior creo como ENUM. Hibernate no amplia un ENUM
 * existente cuando el enum de Java gana valores (un rol nuevo, un tipo de movimiento nuevo), asi que una
 * base vieja los rechazaria con "Data truncated". Es idempotente: si la columna ya es VARCHAR no hace nada.
 */
@Component
@Order(-1)
public class EsquemaEnums implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EsquemaEnums.class);

    /** Tabla y columna de cada enum persistido como texto. */
    private static final List<String[]> COLUMNAS = List.of(
            new String[] {"app_user", "role"},
            new String[] {"movement", "type"},
            new String[] {"category", "species"});

    private final JdbcTemplate jdbc;

    public EsquemaEnums(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean h2 = jdbc.execute((java.sql.Connection c) -> c.getMetaData().getDatabaseProductName())
                .toLowerCase().contains("h2");
        String esquema = h2 ? "schema()" : "database()";
        for (String[] tc : COLUMNAS) {
            try {
                List<String> tipos = jdbc.queryForList(
                        "select data_type from information_schema.columns where table_schema = " + esquema
                                + " and upper(table_name) = upper(?) and upper(column_name) = upper(?)",
                        String.class, tc[0], tc[1]);
                if (tipos.isEmpty() || !"enum".equalsIgnoreCase(tipos.get(0))) continue;
                jdbc.execute(h2
                        ? "alter table " + tc[0] + " alter column " + tc[1] + " varchar(30)"
                        : "alter table " + tc[0] + " modify column " + tc[1] + " varchar(30)");
                log.info("Columna {}.{} convertida de ENUM a VARCHAR", tc[0], tc[1]);
            } catch (RuntimeException e) {
                log.warn("No se pudo revisar la columna {}.{}: {}", tc[0], tc[1], e.getMessage());
            }
        }
    }
}
