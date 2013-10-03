package tk.ivybits.unicode;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.util.ArrayList;

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
            new Font("Arial MS Unicode", Font.PLAIN, 40), // For Windows
            font("Aegean"),
            font("Aegyptus_R"),
            font("Akkadian"),
            font("Analecta"),
            font("Anatolian"),
            font("Maya"),
            font("Musica"),
            font("Symbola")
    };
    private final ArrayList<String> CODE_BLOCKS = new ArrayList<String>(250);
    private final DefaultTableModel TABLE_MODEL = new DefaultTableModel(null, new String[]{"Glyph", "Description"}) {
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
        query(""); // Populate with initial values
        add(searchBox, BorderLayout.NORTH);
        searchBox.addKeyListener(new UnicodeSearchAdapter());
        add(new JScrollPane(unicodeTable), BorderLayout.CENTER);
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(createIcon());
    }

    private static Font font(String name) {
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
        char[] surrogate = utf.toCharArray();
        return Integer.toHexString(surrogate.length == 1 ? surrogate[0] : ((surrogate[0] - 0xD800) * 0x400) + (surrogate[1] - 0xDC00) + 0x10000);
    }

    private BufferedImage createIcon() {
        BufferedImage bf = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bf.createGraphics();
        g.setColor(Color.BLACK);
        g.setFont(UNICODE_FONTS[8].deriveFont(Font.BOLD, 65)); // Symbola
        g.drawString(new String(Character.toChars(Integer.decode("0x1f680"))), 0, 50); // Draw rocket
        return bf;
    }

    private void query(String text) {
        TABLE_MODEL.getDataVector().clear();
        unicodeTable.clearSelection();
        try {
            String[] split = text.split(" ");
            String query = "SELECT char, description, block FROM unicode WHERE description";
            for (int i = 0; i != split.length; i++) {
                query += " LIKE ?";
            }
            query += " LIMIT 200";
            PreparedStatement ps = connection.prepareStatement(query);
            for (int i = 0; i != split.length; i++) {
                ps.setString(i + 1, "%" + split[i].replaceAll("[^A-Za-z0-9]", "") + "%");
            }
            ResultSet chars = ps.executeQuery();
            while (chars.next()) {
                CODE_BLOCKS.add(chars.getString(3));
                TABLE_MODEL.addRow(new Object[]{new String(Character.toChars(chars.getInt(1))), chars.getString(2)});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        TABLE_MODEL.fireTableDataChanged(); // Repaints table
    }

    public class UnicodeSearchAdapter extends KeyAdapter {
        @Override
        public void keyReleased(KeyEvent e) {
            query(searchBox.getText());
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
                menu.add(new JMenuItem("Copy as HTML entity")).addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        toClipboard("&#x" + hexString((String) unicodeTable.getValueAt(row, 0)) + ';');
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
