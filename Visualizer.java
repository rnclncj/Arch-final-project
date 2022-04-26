// https://zetcode.com/gfx/java2d/

import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
// import javax.swing.JFrame;
// import javax.swing.JPanel;
import javax.swing.*;

class Surface extends JPanel {

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
        // add(new Surface());
        JScrollPane scrollPane = new JScrollPane(new Surface(), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(scrollPane);

        setTitle("Verilog Visualizer");
        setSize(3300, 3200);
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