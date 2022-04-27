// https://zetcode.com/gfx/java2d/

import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

class Element {
    private String name;
    private String type;
    private String operation;
    private ArrayList<String> operands;

    public Element(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line);
        type = tokenizer.nextToken();
        name = tokenizer.nextToken();
        operation = tokenizer.nextToken();
        operands = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            operands.add(tokenizer.nextToken());
        }
    }

    public String toString() {
        String str = type + " " + name + " " + operation;
        for (String operand : operands) {
            str += " " + operand;
        }
        return str;
    }
}

class Panel extends JPanel {

    private void doDrawing(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }
}

public class Visualizer extends JFrame {

    public Visualizer() {
        initUI();
    }

    private void initUI() {
        Panel panel = new Panel();
        panel.setPreferredSize(new Dimension(1000, 1000));
        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane);

        setTitle("Verilog Visualizer");
        setSize(300, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("example.txt"));
        ArrayList<Element> elements = new ArrayList<>();
        String line = "";
        while ((line = reader.readLine()) != null) {
            elements.add(new Element(line));
        }
        reader.close();
        System.out.println(elements);

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                Visualizer vis = new Visualizer();
                vis.setVisible(true);
            }
        });
    }
}