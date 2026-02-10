package com.rutar.ttool_tbs;

import java.io.*;
import java.awt.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.imageio.*;
import java.util.jar.*;
import java.awt.event.*;
import java.awt.image.*;
import java.nio.charset.*;
import javax.swing.event.*;
import javax.swing.table.*;
import com.formdev.flatlaf.*;
import javax.swing.filechooser.*;
import com.rutar.ua_translator.*;
import com.formdev.flatlaf.themes.*;
import com.fasterxml.jackson.databind.node.*;

import static java.io.File.*;
import static javax.swing.JOptionPane.*;
import static javax.swing.JFileChooser.*;
import static com.rutar.ttool_tbs.Utils.*;

// ............................................................................
/// Головний клас програми
/// @author Rutar_Andriy
/// 08.02.2026

public class TToolTBS extends JFrame {

private File inputFile;                                         // вхідний файл
private File outputFile;                                       // вихідний файл

private final JFileChooser fileOpen;           // відкривання/збереження файлів

private String appDescription;                                 // опис програми
private DefaultTableModel tableModel;              // стандартна модель таблиці

private boolean dataWasChanged;                // якщо true - дані були змінені
private String[] procFiles;

// Сукупність усіх даних із файлу одної локалізації
private final LinkedHashMap<String, String> langData =
          new LinkedHashMap<>();

// Сукупність усіх даних із файлів всіх оброблюваних локалізацій
private final ArrayList<LinkedHashMap<String, String>> allData =
          new ArrayList<>();

// ............................................................................

private SearchDialog searchDialog;         // діалогове вікно пошуку інформації

// Домашня директорія користувача
public static final File HOME_DIR = FileSystemView.getFileSystemView()
                                                  .getHomeDirectory();

public static boolean debug = true;  // якщо true - увімк. режим налагоджування

// ============================================================================
/// Конструктор за замовчуванням

public TToolTBS() {

initComponents();
initAppIcons();

fileOpen = Utils.getFileChooser("json", FILES_ONLY, "TBS файли локалізації");

}

// ============================================================================
/// Головний метод програми
/// @param args масив переданих параметрів

public static void main (String args[]) {
    
    if (args.length > 0 &&
        args[0].equals("--debug")) { debug = true; }
    
    // ........................................................................
    
    UATranslator.init();
    UIManager.put("FileChooser.readOnly", true);

    JFrame .setDefaultLookAndFeelDecorated(true);
    JDialog.setDefaultLookAndFeelDecorated(true);
    
    FlatLaf.registerCustomDefaultsSource("com.rutar.ttool_tbs.themes");

    try { FlatMacDarkLaf.setup(); }
    catch (Exception e) {}
    
    // ........................................................................
    
    EventQueue.invokeLater(() -> {
        new TToolTBS().setVisible(true);
    });
}

// ============================================================================
/// Відкривання файлів

private void showOpenDialog() {

// Дані змінилися - запитуємо чи відкривати новий файл
if (dataWasChanged) { 

String saveDataQuestion = """
    У відкритому файлі присутні зміни. При відкриванні
    нового файлу вони будуть втрачені. Бажаєте продовжити?
    """;

int answer = showConfirmDialog(this, saveDataQuestion,
                              "Повідомлення", YES_NO_OPTION);

if (answer != YES_OPTION) { return; }

}

// ............................................................................

int result = fileOpen.showOpenDialog(this);
if (result != JFileChooser.APPROVE_OPTION) { return; }

openJsonFile();
updateAppTitle();

}

// ============================================================================
/// Відкривання *.json файлів

private void openJsonFile() {

prepareNewTable();

// ............................................................................
// Перевірка наявності усіх необхідних файлів

for (String file : procFiles) {
    if (!new File(file).exists())
        { JOptionPane.showMessageDialog(this, "Деякі файли відсутні:\n" + file,
                                              "Помилка", ERROR_MESSAGE);
          return; } }

// ............................................................................

try { 

allData.clear();                                           // очищуємо всі дані

// Зчитування вмісту всіх файлів
for (String file : procFiles) {

langData.clear();                                              // очищуємо дані
Utils.parseJson("", MAPPER.readTree(new File(file)), langData); // парсимо файл

// Якщо даних ще немає - додаємо перші дані
if (allData.isEmpty())
    { allData.add((LinkedHashMap<String, String>) langData.clone()); }

// Якщо дані вже є - перевіряємо чи збігаються ключі у файлах
else if (!allData.getFirst().keySet().equals(langData.keySet()))
    { showMessageDialog(this, "Не збігаються ключі у файлах!:\n" +
                               procFiles[0] + "\n" + file,
                              "Помилка", ERROR_MESSAGE);
      return; }

else { allData.add((LinkedHashMap<String, String>) langData.clone()); } }

// ............................................................................
// Записування даних у таблицю

int index = 0;
ArrayList<String> row = new ArrayList<>();
    
for (String key : allData.getFirst().keySet())
    { row.clear();
      row.add(String.valueOf(++index));
      row.add(key);
      
      for (LinkedHashMap<String, String> map : allData)
          { row.add(map.get(key)); }
        
      tableModel.addRow(row.toArray(String[]::new)); }
      finalizeNewTable(); }

catch (IOException ex) { JOptionPane.showMessageDialog(this,
                        "Помилка читання JSON: " + ex.getMessage()); }

}

// ============================================================================
/// Збереження файлів

private void showSaveDialog() {

fileOpen.setSelectedFile(inputFile);
int result = fileOpen.showSaveDialog(this);
if (result != JFileChooser.APPROVE_OPTION) { return; }

saveJsonFile();

}

// ============================================================================
/// Збереження *.json файлів

private void saveJsonFile() {

try {

dataWasChanged = false;
outputFile = fileOpen.getSelectedFile();

ObjectNode newRoot = Utils.buildJsonFromTable(tbl_main);
String pretty = MAPPER.writer(new CompactPrettyPrinter())
                                 .writeValueAsString(newRoot);

try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile),
                                            StandardCharsets.UTF_8))
    { writer.write(pretty); }

updateAppTitle();
showMessageDialog(this, "Файл " + outputFile.getName() + " успішно збережено",
                        "Повідомлення", INFORMATION_MESSAGE); }

catch (HeadlessException | IOException ex)
    { showMessageDialog(this, "При збереженні файлу відбулася "
                            + "критична помилка", "Помилка", ERROR_MESSAGE); }
}

// ============================================================================
/// Відображення інформації про програму

private void showInfoDialog() {

// Отримуємо текст опису програми
if (appDescription == null) {

URL descriptionUrl = getClass().getResource("others/appDescription.txt");
URL channelUrl     = getClass().getResource("others/channelURL.txt");
URL manifestUrl    = getClass().getClassLoader()
                    .getResource("META-INF/MANIFEST.MF");

try (InputStream desc = descriptionUrl.openStream();
     InputStream link = channelUrl    .openStream();
     InputStream data = manifestUrl   .openStream()) {

Attributes attributes = new Manifest(data).getMainAttributes();
    
String channelURL = new String(link.readAllBytes(), StandardCharsets.UTF_8);
String appVersion = attributes.getValue("Version");
String buildDate  = attributes.getValue("Build-Date");

appVersion = (appVersion == null) ? "0.0.1" : appVersion;
buildDate  = (buildDate  == null) ? "25.04.1995" : buildDate.split(" ")[0];

appDescription = new String(desc.readAllBytes(), StandardCharsets.UTF_8)
                    .formatted(channelURL, appVersion, buildDate); }

catch (IOException _) {} }

// ............................................................................

JEditorPane pane = new JEditorPane("text/html", appDescription);
pane.setEditable(false);
pane.setFocusable(false);

pane.addHyperlinkListener((HyperlinkEvent e) -> {
    if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
        try { Desktop.getDesktop().browse(e.getURL().toURI()); }
        catch (IOException | URISyntaxException _) { }
    }
});

showMessageDialog(this, pane, "Про програму", INFORMATION_MESSAGE);

}

// ============================================================================
/// Відображення вікна пошуку інформації

private void showSearchDialog()
    { searchDialog = new SearchDialog(this);   
      searchDialog.setVisible(true); }

// ============================================================================
/// Відображення вікна підтвердження виходу

private void showExitDialog() {

// Якщо дані не змінювалися - просто виходимо
if (!dataWasChanged) { System.exit(0); }

String saveDataQuestion = """
    Ви бажаєте вийти з програми?
    Усі незбережені дані буде втрачено
    """;

int answer = showConfirmDialog(this, saveDataQuestion,
                              "Підтвердження виходу", YES_NO_OPTION);

if (answer == YES_OPTION) { System.exit(0); }

}

// ============================================================================
/// Попередня ініціалізація нової таблиці

private void prepareNewTable() {

dataWasChanged = false;
mni_save.setEnabled(false);
inputFile = fileOpen.getSelectedFile();
sp_table.getVerticalScrollBar().setValue(0);

tableModel = new DefaultTableModel() {
    @Override
    public boolean isCellEditable (int row, int column) { return column == 2; }
};

tbl_main.setModel(tableModel);

tableModel.addColumn("№");
tableModel.addColumn("Ключ");
tableModel.addColumn("Значення");

procFiles = Utils.getProcFiles(inputFile);
String tmp;
for (int z = 1; z < procFiles.length; z++)
    { tmp = procFiles[z].substring(procFiles[z].lastIndexOf("locale") + 7);
      tmp = tmp.substring(0, tmp.indexOf(separator) - 1);
      tableModel.addColumn(tmp.toUpperCase()); }

}

// ============================================================================
/// Завершальна ініціалізація нової таблиці

private void finalizeNewTable() {

TableColumn tColumn;

CellRender cellRenderer = new CellRender();
cellRenderer.setHorizontalAlignment(SwingConstants.CENTER);

tColumn = tbl_main.getColumnModel().getColumn(0);
tColumn.setCellRenderer(cellRenderer);
tColumn.setPreferredWidth(45);
tColumn.setResizable(false);

for (int z = 1; z < tbl_main.getColumnCount(); z++) {
    tbl_main.getColumnModel().getColumn(z).setCellRenderer(new CellRender());
    tbl_main.getColumnModel().getColumn(z).setPreferredWidth(175);    
}

// ............................................................................

updateTableInfo();

mni_find.setEnabled(true);
tableModel.addTableModelListener((TableModelEvent e) -> {
    mni_save.setEnabled(true);
    dataWasChanged = true;
    updateAppTitle();
});

}

// ============================================================================
/// Оновлення інформації про таблицю

private void updateTableInfo() {

    String tmp;

    tmp = lbl_rowCount.getText();
    tmp = tmp.substring(0, tmp.indexOf(":") + 1) + " "
                      + tableModel.getRowCount();
    lbl_rowCount.setText(tmp);

    tmp = lbl_colCount.getText();
    tmp = tmp.substring(0, tmp.indexOf(":") + 1) + " "
                      + tableModel.getColumnCount();
    lbl_colCount.setText(tmp);
    
}

// ============================================================================
/// Оновлення заголовку головного вікна

private void updateAppTitle() {
    
    String newTitle = !dataWasChanged ? inputFile.getName() :
                                 "* " + inputFile.getName() + " *";
    
    if (!getTitle().equals(newTitle)) { setTitle(newTitle); }
}

// ============================================================================
/// Встановлення іконок для головного вікна

private void initAppIcons() {

    BufferedImage icon;
    ArrayList<Image> appIcons = new ArrayList<>();

    try {
        
    for (String resource : new String[] { "icon_16.png",
                                          "icon_32.png" }) {
        resource = "icons/" + resource;
        icon = ImageIO.read(getClass().getResourceAsStream(resource));
        appIcons.add(icon); }
    
    setIconImages(appIcons); }
    
    catch (IOException _) { }
    
}

// ============================================================================
/// Цей метод викликається з конструктора для ініціалізації форми.
/// УВАГА: НЕ змінюйте цей код. Вміст цього методу завжди 
/// перезапишеться редактором форм

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sp_table = new JScrollPane();
        tbl_main = new JTable();
        pnl_footer = new JPanel();
        lbl_colCount = new JLabel();
        lbl_rowCount = new JLabel();
        mnb_main = new JMenuBar();
        mn_file = new JMenu();
        mni_open = new JMenuItem();
        mni_save = new JMenuItem();
        sep_one = new JPopupMenu.Separator();
        mni_find = new JMenuItem();
        sep_two = new JPopupMenu.Separator();
        mni_exit = new JMenuItem();
        mn_edit = new JMenu();
        mni_fntDecompile = new JMenuItem();
        mni_fntCompile = new JMenuItem();
        mn_info = new JMenu();
        mni_about = new JMenuItem();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("TTool_TBS");
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                onWindowClose(evt);
            }
        });

        tbl_main.setModel(new DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        tbl_main.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tbl_main.setAutoscrolls(false);
        tbl_main.setIntercellSpacing(new Dimension(2, 2));
        tbl_main.setRowSelectionAllowed(false);
        tbl_main.setShowGrid(true);
        tbl_main.getTableHeader().setReorderingAllowed(false);
        sp_table.setViewportView(tbl_main);

        pnl_footer.setLayout(new FlowLayout(FlowLayout.CENTER, 50, 5));

        lbl_colCount.setText("Кількість стовбців: 0");
        pnl_footer.add(lbl_colCount);

        lbl_rowCount.setText("Кількість рядків: 0");
        pnl_footer.add(lbl_rowCount);

        mn_file.setText("Файл");

        mni_open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        mni_open.setText("Відкрити");
        mni_open.setActionCommand("open");
        mni_open.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_file.add(mni_open);

        mni_save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        mni_save.setText("Зберегти");
        mni_save.setActionCommand("save");
        mni_save.setEnabled(false);
        mni_save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_file.add(mni_save);
        mn_file.add(sep_one);

        mni_find.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        mni_find.setText("Пошук");
        mni_find.setActionCommand("find");
        mni_find.setEnabled(false);
        mni_find.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_file.add(mni_find);
        mn_file.add(sep_two);

        mni_exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        mni_exit.setText("Вихід");
        mni_exit.setActionCommand("exit");
        mni_exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_file.add(mni_exit);

        mnb_main.add(mn_file);

        mn_edit.setText("Правка");

        mni_fntDecompile.setText("Розпакувати шрифт");
        mni_fntDecompile.setActionCommand("decompileFont");
        mni_fntDecompile.setEnabled(false);
        mni_fntDecompile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_edit.add(mni_fntDecompile);

        mni_fntCompile.setText("Запакувати шрифт");
        mni_fntCompile.setActionCommand("compileFont");
        mni_fntCompile.setEnabled(false);
        mni_fntCompile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_edit.add(mni_fntCompile);

        mnb_main.add(mn_edit);

        mn_info.setText("Інфо");

        mni_about.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK));
        mni_about.setText("Про програму");
        mni_about.setActionCommand("info");
        mni_about.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                onMenuClick(evt);
            }
        });
        mn_info.add(mni_about);

        mnb_main.add(mn_info);

        setJMenuBar(mnb_main);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(sp_table, GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE)
                    .addComponent(pnl_footer, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sp_table, GroupLayout.DEFAULT_SIZE, 333, Short.MAX_VALUE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnl_footer, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

// ============================================================================
/// Прослуховування пунктів меню програми

    private void onMenuClick(ActionEvent evt) {//GEN-FIRST:event_onMenuClick

    switch (evt.getActionCommand()) {

        case "open" -> showOpenDialog();
        case "save" -> showSaveDialog();
        case "find" -> showSearchDialog();
        case "exit" -> showExitDialog();
        case "info" -> showInfoDialog();

    }   
    }//GEN-LAST:event_onMenuClick

// ============================================================================
/// Прослуховування закривання вікна

    private void onWindowClose(WindowEvent evt) {//GEN-FIRST:event_onWindowClose
        showExitDialog();
    }//GEN-LAST:event_onWindowClose

// ============================================================================
/// Список усіх об'явлених змінних

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JLabel lbl_colCount;
    private JLabel lbl_rowCount;
    private JMenu mn_edit;
    private JMenu mn_file;
    private JMenu mn_info;
    private JMenuBar mnb_main;
    private JMenuItem mni_about;
    private JMenuItem mni_exit;
    private JMenuItem mni_find;
    private JMenuItem mni_fntCompile;
    private JMenuItem mni_fntDecompile;
    private JMenuItem mni_open;
    private JMenuItem mni_save;
    private JPanel pnl_footer;
    private JPopupMenu.Separator sep_one;
    private JPopupMenu.Separator sep_two;
    private JScrollPane sp_table;
    public JTable tbl_main;
    // End of variables declaration//GEN-END:variables

// Кінець класу TToolTBS ======================================================

}
