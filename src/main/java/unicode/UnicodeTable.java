package unicode;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

public class UnicodeTable extends JFrame {
    private static Connection connection;

    static {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite::resource:unicode.db");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Font UNICODE_FONTS[] = {
            new Font("Arial MS Unicode", Font.PLAIN, 40),
            create("Aegean"),
            create("Aegyptus_R"),
            create("Akkadian"),
            create("Analecta"),
            create("Anatolian"),
            create("Maya"),
            create("Musica"),
            create("Symbola")
    };
    private final ArrayList<String> CODE_BLOCKS = new ArrayList<String>(250);
    private final DefaultTableModel TABLE_MODEL = new DefaultTableModel(null, new String[]{"Character", "Info"}) {
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    };
    private JTable unicodeTable = new JTable(TABLE_MODEL);
    private JTextField searchBox = new JTextField();

    public UnicodeTable() {
        super("Unicode Character Search Engine");
        setLayout(new BorderLayout());

        unicodeTable.setFillsViewportHeight(true);
        unicodeTable.getColumnModel().getColumn(0).setCellRenderer(new UnicodeFontRenderer());
        unicodeTable.setRowHeight(60);
        unicodeTable.getColumnModel().getColumn(0).setMaxWidth(60);
        unicodeTable.getColumnModel().getColumn(0).setMinWidth(60);
        unicodeTable.addMouseListener(new CopyEntityAdapter());

        add(searchBox, BorderLayout.NORTH);
        searchBox.addKeyListener(new UnicodeSearchAdapter());
        add(new JScrollPane(unicodeTable), BorderLayout.CENTER);
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(createIcon());
    }

    private static Font create(String name) {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, ClassLoader.getSystemResourceAsStream("fonts/" + name + ".ttf")).deriveFont(Font.PLAIN, 40);
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        UnicodeTable table = new UnicodeTable();
        table.setLocationRelativeTo(null);
        table.setVisible(true);
    }

    private static String hexString(String utf) {
        return Integer.toHexString(utf.length() == 1 ? utf.toCharArray()[0] :
                ((utf.toCharArray()[0] - 0xD800) * 0x400) + (utf.toCharArray()[1] - 0xDC00) + 0x10000);
    }

    private BufferedImage createIcon() {
        // Rocket, fuel, maple leaf, train cart
        String[] ICONS = {"0x26fd", "0x1f680", "0x1f341", "0x1f683"};
        BufferedImage bf = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bf.createGraphics();
        g.setColor(Color.BLACK);
        g.setFont(UNICODE_FONTS[8].deriveFont(Font.BOLD, 65)); // Symbola
        g.drawString(new String(Character.toChars(Integer.decode(ICONS[new Random().nextInt(ICONS.length)]))), 0, 50);
        return bf;
    }

    public class UnicodeSearchAdapter extends KeyAdapter {
        @Override
        public void keyReleased(KeyEvent e) {
            TABLE_MODEL.getDataVector().clear();
            unicodeTable.clearSelection();
            try {
                // Be happy, Bobby tables.
                ResultSet chars = connection.createStatement().executeQuery("SELECT * FROM unicode WHERE description LIKE '%" + searchBox.getText() + "%'");
                for (int i = 0; i != 500; i++) { // There is a limit to what is reasonable. I believe this is to be it.
                    if (chars.next()) {
                        CODE_BLOCKS.add(chars.getString(1));
                        TABLE_MODEL.addRow(new Object[]{new String(Character.toChars(Integer.decode("0x" + chars.getString(2)))), chars.getString(3)});
                    } else {
                        break;
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            TABLE_MODEL.fireTableDataChanged(); // Repaints table
        }
    }

    public class CopyEntityAdapter extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                final int row = unicodeTable.rowAtPoint(e.getPoint());
                JPopupMenu menu = new JPopupMenu();
                menu.add(new JMenuItem("Copy as text")).addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        toClipboard((String) unicodeTable.getValueAt(row, 0));
                    }
                });
                menu.add(new JMenuItem("Copy as Unicode")).addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        toClipboard("U+" + hexString((String) unicodeTable.getValueAt(row, 0)));
                    }
                });
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        private void toClipboard(String text) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        }
    }

    public class UnicodeFontRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel part = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String str = (String) value;
            for (Font f : UNICODE_FONTS)
                if (f != null && f.canDisplayUpTo(str) == -1)
                    part.setFont(f); // -1 means entire String can be displayed

            part.setHorizontalAlignment(JLabel.CENTER);
            part.setToolTipText(String.format("<html><b>%s</b><br/>Block: <b>%s</b><br/>Character: <b>U+%s</b></html>",
                    TABLE_MODEL.getValueAt(row, 1), CODE_BLOCKS.get(row), hexString(str)
            ));
            return part;
        }
    }
}
