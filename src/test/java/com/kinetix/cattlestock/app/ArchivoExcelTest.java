package com.kinetix.cattlestock.app;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/** Solo se abre como Excel lo que lo es de verdad: no alcanza con que el nombre termine en .xlsx. */
class ArchivoExcelTest {

    @Test
    void rechazaLoQueNoEsUnExcelAunqueSeLlameAsi() {
        assertThrows(IOException.class, () -> ArchivoExcel.exigir("hola, esto es texto".getBytes()));
        assertThrows(IOException.class, () -> ArchivoExcel.exigir(new byte[0]));
        assertThrows(IOException.class, () -> new ImportadorCategorias().leer("falso.xlsx", "texto".getBytes()));
    }

    @Test
    void aceptaLasFirmasDeXlsxYXls() {
        assertDoesNotThrow(() -> ArchivoExcel.exigir(new byte[] {'P', 'K', 3, 4, 0, 0}));
        assertDoesNotThrow(() -> ArchivoExcel.exigir(new byte[] {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, 0, 0, 0, 0, 0}));
    }
}
