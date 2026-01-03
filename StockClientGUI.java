import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StockClientGUI extends JFrame {

    // --- CONFIGURATION ---
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    
    // --- DARK THEME PALETTE ---
    private static final Color BG_MAIN     = new Color(18, 18, 18);
    private static final Color BG_SIDEBAR  = new Color(25, 25, 25);
    private static final Color BG_CARD     = new Color(32, 32, 32);
    private static final Color BG_INPUT    = new Color(45, 45, 45);
    
    private static final Color TEXT_WHITE  = new Color(245, 245, 245);
    private static final Color TEXT_GRAY   = new Color(170, 170, 170);
    
    private static final Color ACCENT_BLUE = new Color(64, 123, 255);
    private static final Color ACCENT_GREEN= new Color(46, 204, 113);
    private static final Color ACCENT_RED  = new Color(231, 76, 60);

    // --- FONTS ---
    private static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 26);
    private static final Font FONT_HEADER  = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_NORMAL  = new Font("Segoe UI", Font.PLAIN, 15);
    private static final Font FONT_MONO    = new Font("Consolas", Font.PLAIN, 14);

    // --- APP STATE ---
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private boolean isAdmin;
    
    // --- UI COMPONENTS ---
    private CardLayout contentCardLayout;
    private JPanel contentPanel;
    private JLabel statusLabel;
    private DefaultTableModel productTableModel;
    private JTextArea reportArea;

    public StockClientGUI() {
        setupUIDefaults();

        // 1. LOGIN
        LoginResult login = showCustomLogin();
        if (login == null) System.exit(0);
        this.username = login.user;
        this.isAdmin = login.isAdmin;

        // 2. WINDOW SETUP
        setTitle("Stock Master Pro - " + username);
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_MAIN);

        // --- SIDEBAR ---
        add(createSidebar(), BorderLayout.WEST);

        // --- CONTENT AREA ---
        contentCardLayout = new CardLayout();
        contentPanel = new JPanel(contentCardLayout);
        contentPanel.setBackground(BG_MAIN);
        
        contentPanel.add(createDashboardPage(), "DASHBOARD");
        contentPanel.add(createMarketPage(), "MARKET");
        contentPanel.add(createBuyPage(), "BUY");
        contentPanel.add(createReportPage(), "REPORT");
        contentPanel.add(createAdminPage(), "ADMIN");

        add(contentPanel, BorderLayout.CENTER);

        // --- STATUS BAR ---
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(BG_SIDEBAR);
        statusBar.setBorder(new EmptyBorder(8, 15, 8, 15));
        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(TEXT_GRAY);
        statusLabel.setFont(FONT_MONO);
        statusBar.add(statusLabel, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        // 3. START CONNECTION
        new Thread(this::connect).start();
        
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ==========================================
    //           CUSTOM LOGIN
    // ==========================================
    private static class LoginResult { String user; boolean isAdmin; }

    private LoginResult showCustomLogin() {
        JDialog dialog = new JDialog((Frame)null, "Login", true);
        dialog.setSize(400, 320);
        dialog.setLayout(new GridBagLayout());
        dialog.getContentPane().setBackground(BG_CARD);
        dialog.setLocationRelativeTo(null);
        dialog.setUndecorated(true);
        ((JPanel)dialog.getContentPane()).setBorder(new LineBorder(ACCENT_BLUE, 2));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        JLabel title = new JLabel("STOCK MASTER");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0; dialog.add(title, gbc);

        JTextField userField = createStyledField();
        userField.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(TEXT_GRAY), "Username", TitledBorder.LEFT, TitledBorder.TOP, FONT_NORMAL, TEXT_WHITE));
        gbc.gridy = 1; dialog.add(userField, gbc);

        JCheckBox adminCheck = new JCheckBox("Log in as Admin");
        adminCheck.setBackground(BG_CARD);
        adminCheck.setForeground(TEXT_WHITE);
        adminCheck.setFont(FONT_NORMAL);
        gbc.gridy = 2; dialog.add(adminCheck, gbc);

        JButton loginBtn = createButton("ENTER DASHBOARD", ACCENT_BLUE, e -> dialog.dispose());
        loginBtn.setPreferredSize(new Dimension(0, 45));
        gbc.gridy = 3; gbc.insets = new Insets(20, 20, 20, 20);
        dialog.add(loginBtn, gbc);
        
        dialog.setVisible(true);

        LoginResult res = new LoginResult();
        res.user = userField.getText().trim();
        if(res.user.isEmpty()) res.user = "User";
        res.isAdmin = adminCheck.isSelected();
        return res;
    }

    // ==========================================
    //              PAGE FACTORIES
    // ==========================================

    private JPanel createDashboardPage() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_MAIN);
        
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(new EmptyBorder(50, 80, 50, 80));
        
        JLabel icon = new JLabel("📊");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel welcome = new JLabel("Welcome, " + username);
        welcome.setFont(FONT_TITLE);
        welcome.setForeground(TEXT_WHITE);
        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel sub = new JLabel(isAdmin ? "Admin Privileges Active" : "Standard User Access");
        sub.setFont(FONT_NORMAL);
        sub.setForeground(isAdmin ? ACCENT_RED : ACCENT_GREEN);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(icon);
        card.add(Box.createVerticalStrut(20));
        card.add(welcome);
        card.add(Box.createVerticalStrut(10));
        card.add(sub);
        p.add(card);
        return p;
    }

    private JPanel createMarketPage() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_MAIN);
        p.setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_MAIN);
        JLabel title = new JLabel("Market Overview");
        title.setFont(FONT_HEADER);
        title.setForeground(TEXT_WHITE);
        JButton refreshBtn = createButton("↻ Refresh Data", ACCENT_BLUE, e -> send("LIST"));
        refreshBtn.setPreferredSize(new Dimension(150, 40));
        header.add(title, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);
        p.add(header, BorderLayout.NORTH);

        String[] columns = {"ID", "Product Name", "Quantity Available", "Price ($)"};
        productTableModel = new DefaultTableModel(columns, 0);
        JTable table = new JTable(productTableModel);
        
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_WHITE);
        table.setGridColor(new Color(60, 60, 60));
        table.setRowHeight(40);
        table.setFont(FONT_NORMAL);
        table.setShowGrid(true);
        table.setFillsViewportHeight(true);

        JTableHeader th = table.getTableHeader();
        th.setBackground(BG_SIDEBAR);
        th.setForeground(TEXT_GRAY);
        th.setFont(new Font("Segoe UI", Font.BOLD, 14));
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, center);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(BG_MAIN);
        scroll.setBorder(new LineBorder(BG_SIDEBAR));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel createBuyPage() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_MAIN);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(BG_CARD);
        form.setBorder(new EmptyBorder(40, 60, 40, 60));
        form.setPreferredSize(new Dimension(500, 400));
        
        JLabel title = new JLabel("Purchase Order");
        title.setFont(FONT_HEADER);
        title.setForeground(TEXT_WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JTextField idField = createLabeledField("Product ID");
        JTextField qtyField = createLabeledField("Quantity");
        
        JButton buyBtn = createButton("CONFIRM PURCHASE", ACCENT_GREEN, e -> {
            String id = idField.getText();
            String qty = qtyField.getText();
            if(!id.isEmpty() && !qty.isEmpty()) {
                send("BUY_STOCK " + id + " " + qty);
                idField.setText(""); qtyField.setText("");
            }
        });
        buyBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        buyBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        form.add(title);
        form.add(Box.createVerticalStrut(30));
        form.add(idField);
        form.add(Box.createVerticalStrut(15));
        form.add(qtyField);
        form.add(Box.createVerticalStrut(30));
        form.add(buyBtn);

        p.add(form);
        return p;
    }

    private JPanel createReportPage() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_MAIN);
        p.setBorder(new EmptyBorder(20, 30, 20, 30));
        
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setBackground(BG_MAIN);
        JButton loadBtn = createButton("Generate Daily Report", ACCENT_BLUE, e -> {
            reportArea.setText("Fetching report from server...\n\n");
            send("DAILY_REPORT");
        });
        header.add(loadBtn);
        
        reportArea = new JTextArea();
        reportArea.setFont(FONT_MONO);
        reportArea.setBackground(BG_CARD);
        reportArea.setForeground(TEXT_WHITE);
        reportArea.setEditable(false);
        reportArea.setMargin(new Insets(20,20,20,20));
        reportArea.setText("Click 'Generate' to load data.");
        
        p.add(header, BorderLayout.NORTH);
        p.add(new JScrollPane(reportArea), BorderLayout.CENTER);
        return p;
    }

    // --- ADMIN PAGE WITH DROPDOWN (Fixed Text Color) ---
    private JPanel createAdminPage() {
        if (!isAdmin) return createAccessDeniedPage();

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_MAIN);
        p.setBorder(new EmptyBorder(20, 40, 20, 40));

        // 1. TOP DROPDOWN BAR
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.setBackground(BG_MAIN);
        
        JLabel label = new JLabel("Select Action:  ");
        label.setFont(FONT_HEADER);
        label.setForeground(TEXT_WHITE);
        
        String[] options = {"Add New Product", "Update Product Price", "Restock Inventory"};
        JComboBox<String> actionDropdown = new JComboBox<>(options);
        actionDropdown.setFont(FONT_NORMAL);
        
        // FIX: Force White Background and Black Text for Visibility
        actionDropdown.setBackground(Color.WHITE);
        actionDropdown.setForeground(Color.BLACK);
        
        actionDropdown.setPreferredSize(new Dimension(250, 35));
        
        topBar.add(label);
        topBar.add(actionDropdown);
        p.add(topBar, BorderLayout.NORTH);

        // 2. CENTER CARD CONTAINER
        CardLayout adminCards = new CardLayout();
        JPanel formContainer = new JPanel(adminCards);
        formContainer.setBackground(BG_MAIN);
        formContainer.setBorder(new EmptyBorder(30, 0, 0, 0));

        formContainer.add(createSingleAdminForm("Add New Product", "ADD_PRODUCT", "Product Name", "Initial Qty", "Price ($)"), "Add New Product");
        formContainer.add(createSingleAdminForm("Update Product Price", "UPDATE_PRICE", "Product ID", "New Price ($)", null), "Update Product Price");
        formContainer.add(createSingleAdminForm("Restock Inventory", "ADD_STOCK", "Product ID", "Quantity to Add", null), "Restock Inventory");

        p.add(formContainer, BorderLayout.CENTER);

        // 3. DROPDOWN LOGIC
        actionDropdown.addActionListener(e -> {
            String selected = (String) actionDropdown.getSelectedItem();
            adminCards.show(formContainer, selected);
        });

        return p;
    }

    private JPanel createSingleAdminForm(String title, String cmd, String l1, String l2, String l3) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(BG_MAIN);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(new EmptyBorder(40, 60, 40, 60));
        card.setPreferredSize(new Dimension(500, 450));

        JLabel header = new JLabel(title);
        header.setForeground(ACCENT_RED);
        header.setFont(FONT_TITLE);
        header.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField f1 = createLabeledField(l1);
        JTextField f2 = createLabeledField(l2);
        JTextField f3 = (l3 != null) ? createLabeledField(l3) : null;

        JButton submitBtn = createButton("SUBMIT ACTION", ACCENT_BLUE, e -> {
            String args = f1.getText() + " " + f2.getText() + (f3 != null ? " " + f3.getText() : "");
            send(cmd + " " + args);
            f1.setText(""); f2.setText(""); if (f3 != null) f3.setText("");
        });
        submitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        submitBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        
        // FIX: Force Black text on Admin Submit buttons too, just in case
        submitBtn.setForeground(Color.BLACK); 

        card.add(header);
        card.add(Box.createVerticalStrut(30));
        card.add(f1);
        card.add(Box.createVerticalStrut(15));
        card.add(f2);
        if (f3 != null) {
            card.add(Box.createVerticalStrut(15));
            card.add(f3);
        }
        card.add(Box.createVerticalStrut(40));
        card.add(submitBtn);

        wrapper.add(card);
        return wrapper;
    }

    private JPanel createAccessDeniedPage() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_MAIN);
        JLabel l = new JLabel("⛔ Restricted Access: Admin Rights Required");
        l.setFont(FONT_HEADER);
        l.setForeground(ACCENT_RED);
        p.add(l);
        return p;
    }

    // ==========================================
    //           NAVIGATION & HELPERS
    // ==========================================

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(260, 0));
        sidebar.setBorder(new EmptyBorder(30, 15, 30, 15));

        JLabel brand = new JLabel("STOCK MASTER");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 22));
        brand.setForeground(ACCENT_BLUE);
        
        JLabel role = new JLabel(isAdmin ? "ADMIN CONSOLE" : "USER DASHBOARD");
        role.setFont(new Font("Segoe UI", Font.BOLD, 11));
        role.setForeground(TEXT_GRAY);

        sidebar.add(brand);
        sidebar.add(role);
        sidebar.add(Box.createVerticalStrut(50));

        sidebar.add(createNavButton("📊  Dashboard", "DASHBOARD"));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createNavButton("🛒  Market List", "MARKET"));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createNavButton("💳  Buy Stock", "BUY"));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createNavButton("📄  Reports", "REPORT"));
        
        if (isAdmin) {
            sidebar.add(Box.createVerticalStrut(40));
            JLabel lbl = new JLabel(" ADMINISTRATION");
            lbl.setForeground(ACCENT_RED);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            sidebar.add(lbl);
            sidebar.add(Box.createVerticalStrut(10));
            sidebar.add(createNavButton("🛠  Management", "ADMIN"));
        }

        sidebar.add(Box.createVerticalGlue());
        JButton newWin = createButton("New Window", new Color(60,60,60), e -> SwingUtilities.invokeLater(StockClientGUI::new));
        sidebar.add(newWin);
        return sidebar;
    }

    private JButton createNavButton(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_NORMAL);
        btn.setForeground(TEXT_WHITE);
        btn.setBackground(BG_SIDEBAR);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addActionListener(e -> {
            contentCardLayout.show(contentPanel, cardName);
            if(cardName.equals("MARKET")) send("LIST");
        });
        
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { btn.setBackground(new Color(45,45,45)); }
            public void mouseExited(java.awt.event.MouseEvent evt) { btn.setBackground(BG_SIDEBAR); }
        });
        return btn;
    }

    // ==========================================
    //           NETWORKING & PARSING
    // ==========================================

    private void connect() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            setStatus("Connected.", ACCENT_GREEN);
            Thread.sleep(100); out.println(username);
            Thread.sleep(100); out.println(isAdmin ? "yes" : "no");

            String line;
            while ((line = in.readLine()) != null) processMessage(line);

        } catch (Exception e) {
            setStatus("Connection Failed.", ACCENT_RED);
        }
    }

    private void send(String cmd) {
        if(out!=null) out.println(cmd);
    }

    private void processMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (msg.contains("Total Inventory Value") || msg.contains("Highest Value") || msg.contains("Daily Report")) {
                reportArea.append(msg + "\n");
            }
            
            if (msg.startsWith("SUCCESS:")) {
                showToast(msg, ACCENT_GREEN);
            } else if (msg.startsWith("ERROR:")) {
                showToast(msg, ACCENT_RED);
            } else if (msg.startsWith("=== INVENTORY")) {
                productTableModel.setRowCount(0);
            } else if (msg.startsWith("Product[")) {
                Matcher m = Pattern.compile("ID=(\\d+), Name=(.*?), Qty=(\\d+), Price=([\\d.]+)").matcher(msg);
                if(m.find()) productTableModel.addRow(new Object[]{m.group(1), m.group(2), m.group(3), "$" + m.group(4)});
            }
        });
    }

    // ==========================================
    //           UI FACTORY
    // ==========================================

    private static JTextField createStyledField() {
        JTextField f = new JTextField();
        f.setBackground(BG_INPUT);
        f.setForeground(TEXT_WHITE);
        f.setCaretColor(TEXT_WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(TEXT_GRAY, 1), new EmptyBorder(8, 10, 8, 10)));
        f.setFont(FONT_NORMAL);
        return f;
    }
    
    private static JTextField createLabeledField(String labelText) {
        JTextField f = createStyledField();
        f.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(TEXT_GRAY), labelText, TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.PLAIN, 12), TEXT_GRAY));
        return f;
    }

    private static JButton createButton(String text, Color bg, java.awt.event.ActionListener action) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(TEXT_WHITE); // Default Text Color
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(action);
        
        b.setOpaque(true);

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { b.setBackground(bg.darker()); }
            public void mouseExited(java.awt.event.MouseEvent evt) { b.setBackground(bg); }
        });
        return b;
    }

    private void showToast(String msg, Color color) {
        JWindow w = new JWindow(this);
        JPanel p = new JPanel(); p.setBackground(color); p.setBorder(new LineBorder(Color.WHITE));
        JLabel l = new JLabel(msg); l.setForeground(Color.WHITE); l.setFont(FONT_NORMAL);
        p.add(l); w.add(p); w.pack();
        Point loc = getLocation();
        w.setLocation(loc.x + getWidth()/2 - w.getWidth()/2, loc.y + getHeight() - 100);
        w.setVisible(true);
        new Timer(2500, e -> w.dispose()).start();
    }

    private void setStatus(String m, Color c) { statusLabel.setText(m); statusLabel.setForeground(c); }
    private void setupUIDefaults() { try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception e){} }
    public static void main(String[] args) { SwingUtilities.invokeLater(StockClientGUI::new); }
}