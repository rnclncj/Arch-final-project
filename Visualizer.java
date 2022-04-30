// https://zetcode.com/gfx/java2d/

import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

// TODO: fix strings
// TODO: fix wires

class Element {
    private String name;
    private String type;
    private String operation;
    private ArrayList<String> operands;
    private int colNum;
    private int yCoord;
    private int lastCol;

    public Element(Element elem, int cn){
        this.name = elem.name;
        this.type = elem.type;
        operation = "->";
        operands = new ArrayList<>();
        operands.add(name);
        this.colNum = cn;
        lastCol = 0;
    }

    public Element(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line);
        type = tokenizer.nextToken();
        name = tokenizer.nextToken();
        operation = tokenizer.nextToken();
        operands = new ArrayList<>();
        colNum = -1;
        yCoord = 0;
        while (tokenizer.hasMoreTokens()) {
            operands.add(tokenizer.nextToken());
        }
        lastCol = 0;
    }

    public String toString() {
        String str = name + " " + type + " " + operation + " " + operands;
        return str;
    }

    public int getMaxColOfInputs(HashMap<String, Element> elementMap) {
        int max = -1;
        for (String input : operands) {
            if (!elementMap.containsKey(input))
                continue;
            int currCol = elementMap.get(input).getColNum();
            max = Math.max(currCol, max);
        }
        return max;
    }

    public int getXCoord() {
        return Visualizer.HORIZ_DIST + getColNum() * (Visualizer.WIDTH + Visualizer.HORIZ_DIST);
    }

    // returns the height of the element
    public int getHeight() {
        if(operation.equals("->"))
            return Visualizer.BASE_HEIGHT / 4;
        return Visualizer.BASE_HEIGHT * getOperands().size();
    }

    // returns the height of the element
    public int getRealBaseHeight() {
        return getHeight() / operands.size();
    }

    public int getOutX() {
        return getXCoord() + Visualizer.WIDTH;
    }

    public int getOutY() {
        return yCoord + getHeight() / 2;
    }

    public void setYCoord(int yCoord) {
        this.yCoord = yCoord;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public ArrayList<String> getOperands() {
        return operands;
    }

    public void setOutputPoint(int colNum, int yCoord) {
        this.colNum = colNum;
        this.yCoord = yCoord;
    }

    public int getColNum() {
        return colNum;
    }

    public void setColNum(int colNum) {
        this.colNum = colNum;
    }

    public int getYCoord() {
        return yCoord;
    }

    public int getYBase() {
        return yCoord + getHeight();
    }

    public int getLastCol() {
        return lastCol;
    }

    public void setLastCol(int lc) {
        lastCol = lc;
    }

    public void draw(Graphics g, HashMap<String, Element> elementMap, ArrayList<HashMap<String, Element>> columnMaps) {
        Graphics2D g2d = (Graphics2D) g;

        if ((getOperation().equals("=") && !getType().equals("reg")) || getOperation().equals("->")) {
            g2d.drawLine(getXCoord(), getYCoord() + getHeight() / 2, getXCoord() + Visualizer.WIDTH,
                    getYCoord() + getHeight() / 2);
        } else if (!getOperation().equals("<-")) {
            g2d.drawRect(getXCoord(), getYCoord(), Visualizer.WIDTH, getHeight());
            if (type.equals("reg")) {
                int base = getYBase();
                int peakY = base - Visualizer.WIDTH / 6;
                int peakX = getXCoord() + Visualizer.WIDTH / 2;
                g2d.drawLine(peakX, peakY, getXCoord() + Visualizer.WIDTH / 3, base);
                g2d.drawLine(peakX, peakY, getXCoord() + 2 * Visualizer.WIDTH / 3, base);
            }
        }

        if (getOperation().equals("<-")) {
            g2d.drawString(getOperands().get(0), getXCoord() + Visualizer.WIDTH / 2 - 5,
                    getYCoord() + getHeight() / 2 - 5);
        } else if (!getOperation().equals("->")) {
            g2d.drawString(getOperation(), getXCoord() + Visualizer.WIDTH / 2 - 5, getYCoord() + getHeight() / 2 - 5);
        }
        
        if (!(getName().charAt(0) == '.' || getOperation().equals("->"))) {
            g2d.drawString(getName(), getXCoord() + Visualizer.WIDTH / 2 - 5, getYCoord() + getHeight() / 2 + 10);
        }

        // input wires
        if (!getOperation().equals("<-")) {
            ArrayList<String> operands = getOperands();
            for (int i = 0; i < operands.size(); i++) {
                int operandX = getXCoord();
                int operandY = getYCoord() + (int) (getRealBaseHeight() * (i + 0.5));

                Element fromElem;
                if (elementMap.get(operands.get(i)).getColNum() >= getColNum()) {
                    fromElem = elementMap.get(operands.get(i));
                } else {
                    fromElem = columnMaps.get(getColNum() - 1).get(operands.get(i));
                }
                int inputX = fromElem.getOutX();
                int inputY = fromElem.getOutY();
                g2d.drawLine(inputX, inputY, operandX, operandY);
            }
        }
    }
}

class Panel extends JPanel {
    private ArrayList<ArrayList<Element>> columns;
    private HashMap<String, Element> elementMap;
    private ArrayList<HashMap<String, Element>> columnMaps;

    public Panel(ArrayList<ArrayList<Element>> c, HashMap<String, Element> em, ArrayList<HashMap<String, Element>> cm) {
        columns = c;
        elementMap = em;
        columnMaps = cm;
    }

    private void doDrawing(Graphics g) {
        for (ArrayList<Element> column : columns) {
            for (Element elem : column) {
                elem.draw(g, elementMap, columnMaps);
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }
}

public class Visualizer extends JFrame {
    private static final int ADDITIONAL_WIDTH = 4;
    private static final int ADDITIONAL_HEIGHT = 32;

    public static final int BASE_HEIGHT = 20;
    public static final int WIDTH = 40;
    public static final int VERT_DIST = 10;
    public static final int HORIZ_DIST = 50;

    public Visualizer(ArrayList<ArrayList<Element>> c, HashMap<String, Element> em, ArrayList<HashMap<String, Element>> cm, int xDim, int yDim) {
        initUI(c, em, cm, xDim, yDim);
    }

    private void initUI(ArrayList<ArrayList<Element>> c, HashMap<String, Element> em, ArrayList<HashMap<String, Element>> cm, int xDim, int yDim) {
        Panel panel = new Panel(c, em, cm);
        panel.setPreferredSize(new Dimension(xDim, yDim));
        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane);

        setTitle("Verilog Visualizer");
        setSize(xDim + ADDITIONAL_WIDTH, yDim + ADDITIONAL_HEIGHT);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(args[0]));
        ArrayList<Element> elementList = new ArrayList<>();
        HashMap<String, Element> elementMap = new HashMap<>();

        String line = "";
        while ((line = reader.readLine()) != null) {
            Element element = new Element(line);
            elementList.add(element);
            elementMap.put(element.getName(), element);
        }
        reader.close();

        ArrayList<ArrayList<Element>> columns = placeElements(elementList, elementMap);
        addPaths(columns, elementMap);
        ArrayList<HashMap<String, Element>> columnMaps = getColumnMaps(columns);
        int yDim = setCoords(columns);
        int xDim = columns.size() * (Visualizer.HORIZ_DIST + Visualizer.WIDTH) + Visualizer.HORIZ_DIST;

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                Visualizer vis = new Visualizer(columns, elementMap, columnMaps, xDim, yDim);
                vis.setVisible(true);
            }
        });
    }
    
    //places each element into its proper column
    public static ArrayList<ArrayList<Element>> placeElements(ArrayList<Element> elementList, HashMap<String, Element> elementMap) {
        ArrayList<ArrayList<Element>> columns = new ArrayList<>();
        for (Element elem : elementList) {
            int col;
            if (elem.getOperation().equals("<-")) {
                col = Math.max(columns.size()-2, 0);
            } else {
                col = elem.getMaxColOfInputs(elementMap) + 1;
            }
            if (col >= columns.size())
                columns.add(new ArrayList<Element>());
            columns.get(col).add(elem);
            elem.setColNum(col);
        }
        
        //Handles the literals so that they are created in appropriate columns 
        ArrayList<Element> literals = new ArrayList<>();
        for (ArrayList<Element> column : columns) {
            for (Element elem : column) {
                if (elem.getOperation().equals("<-")) {
                    literals.add(elem);
                }
            }
        }
        for (Element literal : literals) {
            if (!columnUses(literal, columns.get(literal.getColNum() + 1))) {
                columns.get(literal.getColNum()).remove(literal);
                columns.get(literal.getColNum() + 1).add(literal);
                literal.setColNum(literal.getColNum() + 1);
            }
        }

        return columns;
    }

    //returns whether a column uses an element as an input anywhere
    private static boolean columnUses(Element elem, ArrayList<Element> column) {
        for (Element colElem : column) {
            if (colElem.getOperands().contains(elem.getName())) {
                return true;
            }
        }
        return false;
    }

    public static void addPaths(ArrayList<ArrayList<Element>> columns, HashMap<String, Element> elementMap) {
        for (int i = 0; i < columns.size(); i++) {
            for (Element elem : columns.get(i)) {
                for (String operand : elem.getOperands()) {
                    if (elementMap.containsKey(operand))
                        elementMap.get(operand).setLastCol(i);
                }
            }
        }

        ArrayList<Element> tempList = new ArrayList<>();
        for (ArrayList<Element> column : columns) {
            for (Element elem : column) {
                tempList.add(elem);
            }
        }
        for (Element elem : tempList) {
            for (int i = elem.getColNum() + 1; i < elem.getLastCol(); i++) {
                columns.get(i).add(new Element(elem, i));
            }
        }
    }

    public static ArrayList<HashMap<String, Element>> getColumnMaps(ArrayList<ArrayList<Element>> columns) {
        ArrayList<HashMap<String, Element>> columnMaps = new ArrayList<>();
        for (ArrayList<Element> column : columns) {
            HashMap<String, Element> columnMap = new HashMap<>();
            for (Element elem : column) {
                columnMap.put(elem.getName(), elem);
            }
            columnMaps.add(columnMap);
        }
        return columnMaps;
    }

    //sets yCoords to correct values within each column, returns the max Height of any column
    public static int setCoords(ArrayList<ArrayList<Element>> columns){
        int maxHeight = 0;
        for (ArrayList<Element> column : columns) {
            int currHeight = Visualizer.VERT_DIST;
            for (Element elem : column) {
                elem.setYCoord(currHeight);
                currHeight += elem.getHeight() + Visualizer.VERT_DIST;
            }
            maxHeight = Math.max(maxHeight, currHeight);
        }
        return maxHeight + Visualizer.VERT_DIST;
    }

}
