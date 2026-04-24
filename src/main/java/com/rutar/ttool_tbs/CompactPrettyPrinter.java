package com.rutar.ttool_tbs;

import java.io.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.*;

// ............................................................................
/// Реалізація форматувальника для запису готового *.json файлу
/// @author Rutar_Andriy
/// 08.02.2026

public class CompactPrettyPrinter extends DefaultPrettyPrinter {
    
// Задання стандартного відступу — 2 пробіли
private static final DefaultIndenter INDENTER =
                 new DefaultIndenter("  ", System.lineSeparator());

// ============================================================================

public CompactPrettyPrinter() {

// Використання відступів для об'єктів та масивів
this.indentObjectsWith(INDENTER);
this.indentArraysWith(INDENTER);

}

// ============================================================================
// залишаємо ": " (без пробілу перед ':')

@Override
public void writeObjectFieldValueSeparator (JsonGenerator g) throws IOException
  { g.writeRaw(": "); }

// ============================================================================
// Перевизначення writeEndObject, щоб у випадку порожнього об'єкта
// зробити перенесення рядка + коректний відступ перед закриваючою дужкою.

@Override
public void writeEndObject (JsonGenerator g,
                            int nrOfEntries) throws IOException {
    
    if (!_objectIndenter.isInline()) {
      // Зменшення рівня вкладення (так, як робить DefaultPrettyPrinter)
      --_nesting;

      _objectIndenter.writeIndentation(g, _nesting);
    }
    g.writeRaw("}");
}

// ============================================================================

@Override
public DefaultPrettyPrinter createInstance()
  { return new CompactPrettyPrinter(); }

// Кінець класу CompactPrettyPrinter ==========================================

}