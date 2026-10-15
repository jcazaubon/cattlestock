package com.kinetix.cattlestock.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import com.kinetix.cattlestock.domain.Ledger;
import com.kinetix.cattlestock.domain.Movement;
import com.kinetix.cattlestock.domain.Species;

/** Lee el .xlsx de vuelta con POI y verifica las celdas clave. */
class PlanillaExcelExporterTest {

    static final YearMonth SEP = YearMonth.of(2026, 9);

    @Test
    void elExcelTieneLasCeldasClaveDeLaPlanilla() throws Exception {
        Ledger l = new Ledger("Empresa de prueba", SEP);
        l.addEstablishment("P", "Principal");
        l.addCategory("P", "toros", "Toros", Species.VACUNOS);
        l.addCategory("P", "yeguarizos", "Yeguarizos", Species.YEGUARIZOS);
        l.setOpening("P", "toros", 51);
        l.setOpening("P", "yeguarizos", 58);
        l.add(Movement.compra("c1", LocalDate.of(2026, 9, 3), "P", "toros", 10));

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        new PlanillaExcelExporter().write(l.planilla("P", SEP), salida);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(salida.toByteArray()))) {
            Sheet hoja = wb.getSheetAt(0);

            Row toros = filaCon(hoja, "Toros");
            assertEquals(51, toros.getCell(1).getNumericCellValue(), "anterior");
            assertEquals(10, toros.getCell(2).getNumericCellValue(), "compra");
            assertEquals(10, toros.getCell(11).getNumericCellValue(), "entradas");
            assertEquals(61, toros.getCell(13).getNumericCellValue(), "final");

            Row totalVacunos = filaCon(hoja, "Total Vacunos");
            assertEquals(61, totalVacunos.getCell(13).getNumericCellValue(), "final vacunos");

            Row totalGeneral = filaCon(hoja, "Total general");
            assertEquals(109, totalGeneral.getCell(1).getNumericCellValue(), "anterior general");
            assertEquals(119, totalGeneral.getCell(13).getNumericCellValue(), "final general");
        }
    }

    private Row filaCon(Sheet hoja, String texto) {
        for (Row r : hoja) {
            if (r.getCell(0) != null && texto.equals(r.getCell(0).getStringCellValue())) {
                return r;
            }
        }
        throw new AssertionError("No se encontro la fila " + texto);
    }
}
