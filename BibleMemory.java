import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class BibleMemory extends JFrame {
    private JTextField inputField;
    private JTextArea displayArea;
    private DefaultListModel listModel;
    private JList savedVersesList;
    
    private final String VPL_PATH = "bible.txt";
    private final String FOLDER_NAME = "verses";

    public BibleMemory() {
        setTitle("Bible Memory Manager");
        setSize(850, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        File dir = new File(FOLDER_NAME);
        if (!dir.exists()) dir.mkdir();

        // UI Setup
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Reference (e.g., JOH 3:16 or JOH 3:16-18):"));
        inputField = new JTextField(20);
        topPanel.add(inputField);
        
        JButton btnLoad = new JButton("Load Text");
        JButton btnSave = new JButton("Save Marker");
        topPanel.add(btnLoad);
        topPanel.add(btnSave);

        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setLineWrap(true);
        displayArea.setWrapStyleWord(true);
        displayArea.setFont(new Font("Serif", Font.PLAIN, 18));
        displayArea.setMargin(new Insets(10, 15, 10, 15));

        listModel = new DefaultListModel();
        savedVersesList = new JList(listModel);
        
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(250, 0));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Stored Markers"));
        rightPanel.add(new JScrollPane(savedVersesList), BorderLayout.CENTER);
        
        JButton btnRefresh = new JButton("Refresh List");
        rightPanel.add(btnRefresh, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(displayArea), BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        btnLoad.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleLoad(inputField.getText().trim());
            }
        });

        btnSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleSave();
            }
        });

        btnRefresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshVerseList();
            }
        });

        savedVersesList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && savedVersesList.getSelectedValue() != null) {
                    String selected = savedVersesList.getSelectedValue().toString();
                    inputField.setText(selected);
                    handleLoad(selected);
                }
            }
        });

        refreshVerseList();
    }

    private void handleSave() {
        String ref = inputField.getText().trim();
        if (ref.isEmpty()) return;

        // Save Logic: "PSA 23:1-6" -> "PSA 23-1_6.jpg"
        String fileName = ref.replace("-", "_");
        fileName = fileName.replace(":", "-") + ".jpg";

        File newFile = new File(FOLDER_NAME + "/" + fileName);
        try {
            if (newFile.createNewFile()) {
                JOptionPane.showMessageDialog(this, "Marker saved!");
                refreshVerseList();
            } else {
                JOptionPane.showMessageDialog(this, "Marker already exists.");
            }
        } catch (IOException ex) {
            displayArea.setText("Error: " + ex.getMessage());
        }
    }

    private void handleLoad(String ref) {
        if (ref.isEmpty()) return;
        // Clean display immediately
        displayArea.setText(""); 
        
        String result = fetchVerses(ref);
        displayArea.setText(result);
        displayArea.setCaretPosition(0);
    }

    private String fetchVerses(String reference) {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(VPL_PATH));
            String line;

            if (reference.contains("-")) {
                // RANGE LOGIC
                String[] parts = reference.split("-");
                String startPart = parts[0].trim();
                int endV = Integer.parseInt(parts[1].trim());
                
                String bookChap = startPart.substring(0, startPart.lastIndexOf(":") + 1);
                int startV = Integer.parseInt(startPart.substring(startPart.lastIndexOf(":") + 1));

                while ((line = br.readLine()) != null) {
                    if (line.toLowerCase().contains(bookChap.toLowerCase())) {
                        int currentV = parseVerseNumber(line);
                        if (currentV >= startV && currentV <= endV) {
                            sb.append(line).append("\n\n");
                        }
                    }
                }
            } else {
                // SINGLE VERSE LOGIC
                while ((line = br.readLine()) != null) {
                    String lowerLine = line.toLowerCase();
                    String lowerRef = reference.toLowerCase();
                    if (lowerLine.startsWith(lowerRef + " ")) {
                        return line;
                    }
                }
            }
        } catch (Exception e) {
            return "VPL Error: " + e.getMessage();
        } finally {
            try { if (br != null) br.close(); } catch (IOException ex) {}
        }
        String res = sb.toString().trim();
        return res.isEmpty() ? "No text found for " + reference : res;
    }

    private int parseVerseNumber(String line) {
        try {
            String[] parts = line.split(" ");
            for (String p : parts) {
                if (p.contains(":")) {
                    return Integer.parseInt(p.split(":")[1].replaceAll("[^0-9]", ""));
                }
            }
        } catch (Exception e) {}
        return -1;
    }

    private void refreshVerseList() {
        listModel.clear();
        File dir = new File(FOLDER_NAME);
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                String name = files[i].getName();
                if (name.endsWith(".jpg")) {
                    String readable = name.replace(".jpg", "");
                    readable = readable.replace("-", ":");
                    readable = readable.replace("_", "-");
                    listModel.addElement(readable);
                }
            }
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { new BibleMemory().setVisible(true); }
        });
    }
}