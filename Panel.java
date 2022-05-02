import java.awt.*;
import javax.swing.*;
import java.util.*;

public class Panel extends JPanel {
    private ArrayList<ArrayList<Element>> columns;
    private HashMap<String, Element> elementMap;
    private ArrayList<HashMap<String, Element>>[] columnMaps;

    public Panel(ArrayList<ArrayList<Element>> c, HashMap<String, Element> em, ArrayList<HashMap<String, Element>>[] cm) {
        columns = c;
        elementMap = em;
        columnMaps = cm;
    }

    private void doDrawing(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        for (ArrayList<Element> column : columns) {
            int numBackOps = 0;
            int numBackOuts = 0;
            for (Element elem : column) {
                elem.draw(g2d, elementMap, columnMaps);
                
                if (elem.getOperation().equals("--") || elem.getOperation().equals("<-")) {
                    continue;
                }
                for (String operand : elem.getOperands()) {
                    if (elementMap.get(operand).getColNum() >= elem.getColNum()) {
                        numBackOps++;
                    }
                }
                if (columnMaps[1].get(elem.getColNum()).containsKey(elem.getName())) {
                    numBackOuts++;
                }
            }

            // backwards wires
            g2d.setColor(Visualizer.BACKWARDS_COLOR);
            int backOpCount = 0;
            int backOutCount = 0;
            for (Element elem : column) {
                if (elem.getOperation().equals("--") || elem.getOperation().equals("<-")) {
                    continue;
                }
                // operands
                for (int i = 0; i < elem.getOperands().size(); i++) {
                    String operand = elem.getOperands().get(i);
                    if (elementMap.get(operand).getColNum() >= elem.getColNum()) {
                        backOpCount++;
                        int rightX = elem.getXCoord();
                        int leftX = rightX - (int) ((Visualizer.HORIZ_DIST / 2.0) * (1 - ((double) backOpCount) / (numBackOps + 1)));
                        int toY = elem.getOperandYCoord(i);
                        int fromY = columnMaps[1].get(elem.getColNum()).get(operand).getOutY();
                        
                        g2d.drawLine(rightX, fromY, leftX, fromY);
                        g2d.drawLine(leftX, fromY, leftX, toY);
                        g2d.drawLine(leftX, toY, rightX, toY);
                    }
                }
                
                // outputs
                if (columnMaps[1].get(elem.getColNum()).containsKey(elem.getName())) {
                    backOutCount++;
                    int leftX = elem.getOutX();
                    int rightX = leftX + (int) ((Visualizer.HORIZ_DIST / 2.0) * (1 - ((double) backOutCount) / (numBackOuts + 1)));
                    int fromY = elem.getOutY();
                    int toY = columnMaps[1].get(elem.getColNum()).get(elem.getName()).getOutY();
                    
                    g2d.drawLine(leftX, fromY, rightX, fromY);
                    g2d.drawLine(rightX, fromY, rightX, toY);
                    g2d.drawLine(rightX, toY, leftX, toY);
                }
            }
            g2d.setColor(Color.BLACK);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }
}