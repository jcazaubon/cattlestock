package com.kinetix.cattlestock.app;

import java.io.IOException;

/** Comprueba que lo subido sea de verdad una planilla de Excel (xlsx/xlsm son ZIP, xls es OLE) antes de abrirlo. */
final class ArchivoExcel {

    private ArchivoExcel() {
    }

    static void exigir(byte[] datos) throws IOException {
        boolean zip = datos != null && datos.length > 4 && datos[0] == 'P' && datos[1] == 'K'
                && datos[2] == 3 && datos[3] == 4;
        boolean ole = datos != null && datos.length > 8 && (datos[0] & 0xFF) == 0xD0 && (datos[1] & 0xFF) == 0xCF
                && (datos[2] & 0xFF) == 0x11 && (datos[3] & 0xFF) == 0xE0;
        if (!zip && !ole) throw new IOException("No pude leer el archivo como planilla de Excel");
    }
}
