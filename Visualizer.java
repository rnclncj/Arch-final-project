// https://zetcode.com/gfx/java2d/

import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

// TODO: add backwards wires
// TODO: add specialized gates
// TODO: fix operator strings (chunk of wire)
// TODO: fix literal string length (when it juts out)
// TODO: fix MUX jank
// TODO: represent wire lengths of unnamed wires?

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
    private static final int MAX_WIDTH = 1500;
    private static final int MAX_HEIGHT = 800;
    private static final int ADDITIONAL_WIDTH = 4;
    private static final int ADDITIONAL_HEIGHT = 32;

    public static final int BASE_HEIGHT = 20;
    public static final int FULL_WIDTH = 80;
    public static final int BOX_WIDTH = 40;
    public static final int STUD_WIDTH = FULL_WIDTH - BOX_WIDTH;
    public static final int VERT_DIST = 4;
    public static final int HORIZ_DIST = 60;

    public Visualizer(ArrayList<ArrayList<Element>> c, HashMap<String, Element> em, ArrayList<HashMap<String, Element>> cm, int xDim, int yDim) {
        initUI(c, em, cm, xDim, yDim);
    }

    private void initUI(ArrayList<ArrayList<Element>> c, HashMap<String, Element> em, ArrayList<HashMap<String, Element>> cm, int xDim, int yDim) {
        Panel panel = new Panel(c, em, cm);
        panel.setPreferredSize(new Dimension(xDim, yDim));
        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane);

        setTitle("Verilog Visualizer");
        setSize(Math.min(xDim + ADDITIONAL_WIDTH, MAX_WIDTH), Math.min(yDim + ADDITIONAL_HEIGHT,MAX_HEIGHT));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("p9example.vf"));
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
        addPaths(columns);
        ArrayList<HashMap<String, Element>> columnMaps = getColumnMaps(columns);
        reorder(columns, elementMap, columnMaps);
        int yDim = setCoords(columns);
        int xDim = columns.size() * (Visualizer.HORIZ_DIST + Visualizer.FULL_WIDTH) + Visualizer.HORIZ_DIST;

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

    public static void addPaths(ArrayList<ArrayList<Element>> columns) {
        HashMap<String, Integer> lastColMap = new HashMap<>();
        HashMap<String, Integer> firstColMap = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            for (Element elem : columns.get(i)) {
                if (!elem.getOperation().equals("<-")) {
                    for (String operand : elem.getOperands()) {
                        lastColMap.put(operand, i);
                        if (!firstColMap.containsKey(operand)) {
                            firstColMap.put(operand, i);
                        }
                    }
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
            if (!lastColMap.containsKey(elem.getName())) {
                continue;
            }
            for (int i = elem.getColNum() + 1; i < lastColMap.get(elem.getName()); i++) {
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

    public static void reorder(ArrayList<ArrayList<Element>> columns, HashMap<String, Element> elementMap, ArrayList<HashMap<String, Element>> columnMaps) {
        HashMap<String, Double> scoreMap = new HashMap<>();
        HashMap<String, Integer> indexMap = getIndexMap(columns.get(0));
        for (int i = 1; i < columns.size(); i++) {
            ArrayList<Element> column = columns.get(i);
            for (int j = 0; j < column.size(); j++) {
                Element elem = column.get(j);
                int scoreSum = i;
                int scoreNum = 1;
                if (!elem.getOperation().equals("<-")) {
                    for (String operand : elem.getOperands()) {
                        if (elementMap.get(operand).getColNum() >= elem.getColNum()) {
                            continue;
                        }
                        Element fromElem = columnMaps.get(elem.getColNum() - 1).get(operand);
                        scoreSum += indexMap.get(fromElem.getName());
                        scoreNum++;
                    }
                }
                scoreMap.put(elem.getName(), ((double) scoreSum) / scoreNum);
            }
            column.sort(new ScoreComp(scoreMap));
            indexMap = getIndexMap(column);
        }
    }

    private static HashMap<String, Integer> getIndexMap(ArrayList<Element> column) {
        HashMap<String, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < column.size(); i++) {
            indexMap.put(column.get(i).getName(), i);
        }
        return indexMap;
    }

    private static class ScoreComp implements Comparator<Element> {
        private HashMap<String, Double> scoreMap;

        public ScoreComp(HashMap<String, Double> sm) {
            scoreMap = sm;
        }

        public int compare(Element a, Element b) {
            double diff = scoreMap.get(a.getName()) - scoreMap.get(b.getName());
            if (diff > 0) {
                return 1;
            }
            if (diff < 0) {
                return -1;
            }
            return 0;
        }
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

    public static String condenseName(String name, int length){
        if(name.length() <= length)
            return name;
        return name.substring(0, length / 2 - 2) + ".." + name.substring(name.length() - length / 2);
    }
}