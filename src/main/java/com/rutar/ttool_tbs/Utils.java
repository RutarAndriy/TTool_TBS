package com.rutar.ttool_tbs;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

import static java.io.File.*;
import static com.rutar.ttool_tbs.TToolTBS.*;

// ............................................................................
/// Корисні допоміжні методи
/// @author Rutar_Andriy
/// 08.02.2026

public class Utils {

// Розділювач вкладених структур
public static final String ARROW = "➤";
// Сигнатура для пустих об'єктів
public static final String EMPTY_OBJECT = "- ### -";
// Об'єкт для маніпулювання json-даними
public static final ObjectMapper MAPPER = new ObjectMapper();

// ============================================================================
/// Парсинг даних у форматі Json
/// @param parent батьківський елемент
/// @param node вузол для парсингу
/// @param data LinkedHashMap для збереження даних

public static void parseJson (String parent, JsonNode node,
                              LinkedHashMap<String, String> data) {

// ............................................................................
// Обробка об'єктів

if (node.isObject()) {
    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    int cycles = 0;
    while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        String key = parent.isEmpty() ? entry.getKey() :
                                        parent + ARROW + entry.getKey();
        parseJson(key, entry.getValue(), data);
        cycles++;
    }
    if (cycles == 0) { 
        TextNode textNode = JsonNodeFactory.instance.textNode(EMPTY_OBJECT);
        parseJson(parent, textNode, data); }
}

// ............................................................................
// Обробка масивів

else if (node.isArray()) {
    int index = 0;
    for (JsonNode element : node) {
        parseJson(parent + "[" + index + "]", element, data);
        index++;
    }
}
// ............................................................................
// Обробка елементів

else { data.putLast(parent, node.asText()); }

}

// ============================================================================
/// Перетворення табличних даних в json-структуру
/// @param table таблиця з вихідними даними
/// @return готова json-структура

public static ObjectNode buildJsonFromTable (JTable table) {
    
    ObjectNode root = MAPPER.createObjectNode();

    for (int i = 0; i < table.getRowCount(); i++)
        { String key = table.getValueAt(i, 1).toString();
          String value = table.getValueAt(i, 2).toString();
          value = replaceUnusedChars(value);
          addValueToNode(root, key, value); }

    return root;
}

// ============================================================================
// Додавання даних до json-вузла

private static void addValueToNode (ObjectNode node,
                                    String path, String value) {
    
int dot = path.indexOf(ARROW);

// Обробка вкладених структур
if (dot >= 0)
    { String head = path.substring(0, dot);
      String rest = path.substring(dot + 1);
      JsonNode child = node.get(head);
      if (child == null || !child.isObject())
          { child = MAPPER.createObjectNode();
            node.set(head, child); }
      addValueToNode((ObjectNode) child, rest, value); }

// Обробка масивів
else if (path.matches(".+\\[\\d+\\]$"))
    { String base = path.replaceAll("\\[\\d+\\]$", "");
      ArrayNode array = node.has(base) && node.get(base).isArray() ?
                       (ArrayNode) node.get(base) : MAPPER.createArrayNode();
      node.set(base, array);
      array.add(value); }

// Обробка звичайних об'єктів
else { if (value.equals(EMPTY_OBJECT)) { node.putObject(path);  }
       else                            { node.put(path, value); } }

}

// ============================================================================
/// Отримання масиву файлів допоміжних локалізацій
/// @param currentFile шлях до перекладуваного файлу
/// @return масив файлів допоміжних локалізацій

public static String[] getProcFiles (File currentFile) {
 
    String path = currentFile.getAbsolutePath();
    String localePath = path.substring(0, path.lastIndexOf("locale") + 7);
    int localeIndex = path.lastIndexOf("locale");
    String lang = path.substring(localeIndex + 7);
    lang = lang.substring(0, lang.indexOf(File.separator));

    ArrayList<String> result = new ArrayList<>();
    result.add(path);

    if (localeIndex == -1) { return result.toArray(String[]::new); }

    for (File file : new File(localePath).listFiles()) {
        if (file.isDirectory() && file.getName().contains("^"))
            { result.add(path.replace(separator + lang + separator,
                                      separator + file.getName() +
                                      separator)); } }

    return result.toArray(String[]::new);

}

// ============================================================================
/// Отримання налаштованого JFileChooser'а
/// @param ext розширення файлів
/// @param selectionMode тип виділення (папки, файли, папки+файли)
/// @param desc опис розширення файлів
/// @return налаштований екземпляр JFileChooser'а

public static JFileChooser getFileChooser (String ext, int selectionMode,
                                           String desc) {
    
    JFileChooser chooser = new JFileChooser();
    FileNameExtensionFilter filter = new FileNameExtensionFilter(desc, ext);
    
    chooser.setFileSelectionMode(selectionMode);
    chooser.removeChoosableFileFilter(chooser
           .getChoosableFileFilters()[0]);
    chooser.addChoosableFileFilter(filter);
    chooser.setCurrentDirectory(HOME_DIR);
    
    return chooser;

}

// ============================================================================
/// Отримання папки, у якій міститься останній виділений файл/папка
/// @param chooser jFileChooser, який використовувався для вибору файлу
/// @return папка, у якій міститься останній виділений файл/папка

public static File getLastDir (JFileChooser chooser) {
    
    File file = chooser.getSelectedFile();
    
    // Якщо останього файлу немає - повертаємо null
    if (file == null)
        { return null; }
    // Якщо останній файл є папкою - повертаємо батьківську папку
    else if (file.isDirectory())
        { return new File(file.getParent()); }
    // Якщо останній файл є файлом - повертаємо шлях до його папки
    else
        { return new File(file.getPath().replace(file.getName(), "")); }

}

// ============================================================================
/// Заміна невикористовуваних символів у тексті
/// @param value текст із невикористовуваними символами
/// @return текст із заміненими символами

public static String replaceUnusedChars (String value) {
    
    return value.replace('’', '\'')
                .replace('Ґ', 'Г')
                .replace('ґ', 'г');
}

// Кінець класу Utils =========================================================

}
