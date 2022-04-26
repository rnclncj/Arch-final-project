// https://zetcode.com/gfx/java2d/

import java.awt.*;
import javax.swing.*;

class Panel extends JPanel {

    private void doDrawing(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.drawString("Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D Java 2D", 50, 50);
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

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                Visualizer vis = new Visualizer();
                vis.setVisible(true);
            }
        });
    }
}