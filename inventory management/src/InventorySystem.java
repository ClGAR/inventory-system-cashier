import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;

public class InventorySystem extends JFrame {
    private JTable inventoryTable, cartTable;
    private DefaultTableModel inventoryModel, cartModel;
    private JTextField txtName, txtQty, txtPrice, txtSearch;
    private JLabel statusBar, totalLabel;
    private double totalAmount = 0;
    private TableRowSorter<DefaultTableModel> sorter;
    private HashMap<String, Integer> inventoryStock = new HashMap<>();
    private final String INVENTORY_FILE = "inventory_data.csv";

    public InventorySystem() {
        setTitle("\uD83D\uDE97 Car Repair Inventory & Cashier");
        setSize(1080, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLookAndFeel();

        JPanel contentPane = setupMainPanel();
        setContentPane(contentPane);

        setupTables();
        contentPane.add(setupStatusBar(), BorderLayout.NORTH);
        contentPane.add(setupTablesPanel(), BorderLayout.CENTER);
        contentPane.add(setupFormPanel(), BorderLayout.SOUTH);

        loadInventory();
    }

    private void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    private JPanel setupMainPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(Color.BLACK);
        return panel;
    }

    private void setupTables() {
        inventoryModel = new DefaultTableModel(new String[]{"Part/Service", "Qty", "Price (PHP)"}, 0);
        cartModel = new DefaultTableModel(new String[]{"Item", "Qty", "Price", "Subtotal"}, 0);

        inventoryTable = new JTable(inventoryModel);
        sorter = new TableRowSorter<>(inventoryModel);
        inventoryTable.setRowSorter(sorter);

        cartTable = new JTable(cartModel);
        cartTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(isSelected ? new Color(100, 149, 237) : new Color(173, 216, 230));
                c.setForeground(isSelected ? Color.WHITE : Color.BLACK);
                return c;
            }
        });
        cartTable.setRowHeight(26);
        cartTable.setBackground(new Color(173, 216, 230));
        cartTable.setForeground(Color.BLACK);
        cartTable.setOpaque(true);
    }

    private JPanel setupTablesPanel() {
        JScrollPane inventoryPane = new JScrollPane(inventoryTable);
        inventoryPane.setBorder(BorderFactory.createTitledBorder("\uD83D\uDCE6 Inventory"));

        JScrollPane cartPane = new JScrollPane(cartTable);
        cartPane.setBorder(BorderFactory.createTitledBorder("\uD83E\uDDFE Receipt"));
        cartPane.getViewport().setBackground(new Color(173, 216, 230));

        JTableHeader cartHeader = cartTable.getTableHeader();
        cartHeader.setBackground(new Color(70, 130, 180));
        cartHeader.setForeground(Color.BLACK);
        cartHeader.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        panel.add(inventoryPane);
        panel.add(cartPane);
        return panel;
    }

    private JPanel setupFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("\uD83D\uDEE0 Manage & Checkout"));
        panel.setBackground(new Color(245, 245, 245));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtName = new JTextField(15);
        txtQty = new JTextField(5);
        txtPrice = new JTextField(5);
        txtSearch = new JTextField(15);

        JButton btnAdd = new JButton("➕ Add");
        JButton btnUpdate = new JButton("✏️ Update");
        JButton btnDelete = new JButton("❌ Delete");
        JButton btnToCart = new JButton("\uD83D\uDED2 To Cart");
        JButton btnCheckout = new JButton("\uD83D\uDCB5 Checkout");
        JButton btnSave = new JButton("\uD83D\uDCBE Save");
        JButton btnLoad = new JButton("\uD83D\uDCC2 Load");

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("🔧 Name:"), gbc); gbc.gridx++;
        panel.add(txtName, gbc); gbc.gridx++;
        panel.add(new JLabel("📦 Qty:"), gbc); gbc.gridx++;
        panel.add(txtQty, gbc); gbc.gridx++;
        panel.add(new JLabel("💰 Price:"), gbc); gbc.gridx++;
        panel.add(txtPrice, gbc);

        gbc.gridy = 1; gbc.gridx = 0; gbc.gridwidth = 6;
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionPanel.add(btnAdd); actionPanel.add(btnUpdate); actionPanel.add(btnDelete);
        actionPanel.add(btnToCart); actionPanel.add(btnCheckout);
        actionPanel.add(btnSave); actionPanel.add(btnLoad);
        actionPanel.add(new JLabel("🔍 Search:")); actionPanel.add(txtSearch);
        panel.add(actionPanel, gbc);

        btnAdd.addActionListener(e -> crudAdd());
        btnUpdate.addActionListener(e -> crudUpdate());
        btnDelete.addActionListener(e -> crudDelete());
        btnToCart.addActionListener(e -> addToCart());
        btnCheckout.addActionListener(e -> checkout());
        btnSave.addActionListener(e -> saveInventory());
        btnLoad.addActionListener(e -> loadInventory());

        inventoryTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = inventoryTable.getSelectedRow();
                txtName.setText(inventoryModel.getValueAt(row, 0).toString());
                txtQty.setText(inventoryModel.getValueAt(row, 1).toString());
                txtPrice.setText(inventoryModel.getValueAt(row, 2).toString());
            }
        });

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchFilter(); }
            public void removeUpdate(DocumentEvent e) { searchFilter(); }
            public void changedUpdate(DocumentEvent e) { searchFilter(); }
        });

        return panel;
    }

    private JPanel setupStatusBar() {
        statusBar = new JLabel(" Ready to work");
        statusBar.setForeground(Color.WHITE);
        statusBar.setBackground(Color.BLACK);
        statusBar.setOpaque(true);
        statusBar.setBorder(BorderFactory.createEtchedBorder());

        totalLabel = new JLabel(" Total: PHP 0.00");
        totalLabel.setForeground(Color.BLACK);
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(statusBar, BorderLayout.CENTER);
        panel.add(totalLabel, BorderLayout.EAST);
        return panel;
    }

    private void crudAdd() {
        String name = txtName.getText().trim();
        if (inventoryStock.containsKey(name)) {
            showError("Item already exists. Use Update instead.");
            return;
        }
        try {
            int qty = Integer.parseInt(txtQty.getText());
            double price = Double.parseDouble(txtPrice.getText());
            inventoryModel.addRow(new Object[]{name, qty, price});
            inventoryStock.put(name, qty);
            statusBar.setText("✅ Added: " + name);
        } catch (NumberFormatException e) {
            showError("Invalid quantity or price.");
        }
    }

    private void crudUpdate() {
        int row = inventoryTable.getSelectedRow();
        if (row >= 0) {
            String name = txtName.getText();
            try {
                int qty = Integer.parseInt(txtQty.getText());
                double price = Double.parseDouble(txtPrice.getText());
                inventoryModel.setValueAt(name, row, 0);
                inventoryModel.setValueAt(qty, row, 1);
                inventoryModel.setValueAt(price, row, 2);
                inventoryStock.put(name, qty);
                statusBar.setText("✏️ Updated: " + name);
            } catch (NumberFormatException e) {
                showError("Invalid quantity or price.");
            }
        } else showError("Select a row to update.");
    }

    private void crudDelete() {
        int row = inventoryTable.getSelectedRow();
        if (row >= 0) {
            String name = (String) inventoryModel.getValueAt(row, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "Delete item: " + name + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                inventoryModel.removeRow(row);
                inventoryStock.remove(name);
                statusBar.setText("❌ Deleted: " + name);
            }
        } else showError("Select a row to delete.");
    }

    private void addToCart() {
        String name = txtName.getText();
        try {
            int qty = Integer.parseInt(txtQty.getText());
            double price = Double.parseDouble(txtPrice.getText());
            int available = inventoryStock.getOrDefault(name, -1);
            if (available == -1) {
                showError("Item not found.");
                return;
            }
            if (qty > available) {
                showError("Insufficient stock.");
                return;
            }
            inventoryStock.put(name, available - qty);
            updateInventoryQty(name, available - qty);
            double subtotal = qty * price;
            cartModel.addRow(new Object[]{name, qty, price, subtotal});
            totalAmount += subtotal;
            totalLabel.setText(String.format("🧾 Total: PHP %.2f", totalAmount));
            statusBar.setText("🛒 Added to cart: " + name);
        } catch (NumberFormatException e) {
            showError("Qty and Price must be valid numbers.");
        }
    }

    private void checkout() {
        JOptionPane.showMessageDialog(this, String.format("Transaction complete.\nTotal: PHP %.2f", totalAmount));
        cartModel.setRowCount(0);
        totalAmount = 0;
        totalLabel.setText("🧾 Total: PHP 0.00");
        statusBar.setText("✅ Checkout done.");
    }

    private void updateInventoryQty(String name, int newQty) {
        for (int i = 0; i < inventoryModel.getRowCount(); i++) {
            if (inventoryModel.getValueAt(i, 0).equals(name)) {
                inventoryModel.setValueAt(newQty, i, 1);
                break;
            }
        }
    }

    private void searchFilter() {
        String keyword = txtSearch.getText().trim();
        sorter.setRowFilter(keyword.isEmpty() ? null : RowFilter.regexFilter("(?i)" + keyword, 0));
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    private void saveInventory() {
        File file = new File(INVENTORY_FILE);
        try {
            if (!file.exists()) file.createNewFile();
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                for (int i = 0; i < inventoryModel.getRowCount(); i++) {
                    writer.println(inventoryModel.getValueAt(i, 0) + "," +
                            inventoryModel.getValueAt(i, 1) + "," +
                            inventoryModel.getValueAt(i, 2));
                }
                statusBar.setText("💾 Inventory saved successfully.");
            }
        } catch (IOException e) {
            showError("Save error: " + e.getMessage());
        }
    }

    private void loadInventory() {
        File file = new File(INVENTORY_FILE);
        if (!file.exists()) {
            showError("No saved inventory found.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            inventoryModel.setRowCount(0);
            inventoryStock.clear();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    String name = parts[0];
                    int qty = Integer.parseInt(parts[1]);
                    double price = Double.parseDouble(parts[2]);
                    inventoryModel.addRow(new Object[]{name, qty, price});
                    inventoryStock.put(name, qty);
                }
            }
            statusBar.setText("📂 Inventory loaded successfully.");
        } catch (IOException | NumberFormatException e) {
            showError("Load error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}

class LoginFrame extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;

    public LoginFrame() {
        setTitle("\uD83D\uDD10 Login");
        setSize(350, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtUsername = new JTextField(15);
        txtPassword = new JPasswordField(15);
        JButton btnLogin = new JButton("Login");
        btnLogin.addActionListener(e -> authenticate());

        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Username:"), gbc); gbc.gridx = 1;
        add(txtUsername, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Password:"), gbc); gbc.gridx = 1;
        add(txtPassword, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(btnLogin, gbc);
    }

    private void authenticate() {
        String user = txtUsername.getText();
        String pass = new String(txtPassword.getPassword());

        if (user.equals("admin") && pass.equals("1234")) {
            new InventorySystem().setVisible(true);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid credentials. Try admin/1234.");
        }
    }
}
