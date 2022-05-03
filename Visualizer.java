import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

// TODO: add specialized gates
// TODO: fix operator strings (chunk of wire)
// DONE: fix literal string length (when it juts out)
// TODO: fix wire being set to temp late
// TODO: adjust column distances to reflect amount of wire squishing
// TODO: resolve redundant
// TODO: fix wire density jank (scale horizontal distance)

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

    public static final Color BACKWARDS_COLOR = Color.BLUE;

    public Visualizer(ArrayList<ArrayList<Element>> c, HashMap<String, Element> em, ArrayList<HashMap<String, Element>>[] cm, int[] dims) {
        initUI(c, em, cm, dims);
    }

    private void initUI(ArrayList<ArrayList<Element>> c, HashMap<String, Element> em, ArrayList<HashMap<String, Element>>[] cm, int[] dims) {
        Panel panel = new Panel(c, em, cm);
        panel.setPreferredSize(new Dimension(dims[0], dims[1]));
        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane);

        setTitle("Verilog Visualizer");
        setSize(Math.min(dims[0] + ADDITIONAL_WIDTH, MAX_WIDTH), Math.min(dims[1] + ADDITIONAL_HEIGHT, MAX_HEIGHT));
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
        ArrayList<HashMap<String, Element>>[] columnMaps = getColumnMaps(columns);
        reorder(columns, elementMap, columnMaps);
        int[] dims = setCoords(columns);
        // int xDim = columns.size() * (Visualizer.HORIZ_DIST + Visualizer.FULL_WIDTH) + Visualizer.HORIZ_DIST;

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                Visualizer vis = new Visualizer(columns, elementMap, columnMaps, dims);
                vis.setVisible(true);
            }
        });
    }
    
    //places each element into its proper column
    public static ArrayList<ArrayList<Element>> placeElements(ArrayList<Element> elementList, HashMap<String, Element> elementMap) {
        ArrayList<ArrayList<Element>> columns = new ArrayList<>();
        for (Element elem : elementList) {
            int col;
            if (elem.getOperation().equals("--")) {
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
                if (elem.getOperation().equals("--")) {
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
                if (!elem.getOperation().equals("--")) {
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
                columns.get(i).add(new Element(elem, i, true));
            }
        }
        for (Element elem : tempList) {
            if (!firstColMap.containsKey(elem.getName())) {
                continue;
            }
            for (int i = firstColMap.get(elem.getName()); i <= elem.getColNum(); i++) {
                columns.get(i).add(new Element(elem, i, false));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<HashMap<String, Element>>[] getColumnMaps(ArrayList<ArrayList<Element>> columns) {
        ArrayList<HashMap<String, Element>> columnMaps = new ArrayList<>();
        ArrayList<HashMap<String, Element>> backColumnMaps = new ArrayList<>();
        for (ArrayList<Element> column : columns) {
            HashMap<String, Element> columnMap = new HashMap<>();
            HashMap<String, Element> backColumnMap = new HashMap<>();
            for (Element elem : column) {
                if (elem.getOperation().equals("<-")) {
                    backColumnMap.put(elem.getName(), elem);
                } else {
                    columnMap.put(elem.getName(), elem);
                }
            }
            columnMaps.add(columnMap);
            backColumnMaps.add(backColumnMap);
        }
        return new ArrayList[] {columnMaps, backColumnMaps};
    }

    public static void reorder(ArrayList<ArrayList<Element>> columns, HashMap<String, Element> elementMap, ArrayList<HashMap<String, Element>>[] columnMaps) {
        HashMap<String, Double> scoreMap = new HashMap<>();
        HashMap<String, Integer> indexMap = getIndexMap(columns.get(0));
        for (int i = 1; i < columns.size(); i++) {
            ArrayList<Element> column = columns.get(i);
            for (int j = 0; j < column.size(); j++) {
                Element elem = column.get(j);
                int scoreSum = i;
                int scoreNum = 1;
                if (!(elem.getOperation().equals("--") || elem.getOperation().equals("<-"))) {
                    for (String operand : elem.getOperands()) {
                        if (elementMap.get(operand).getColNum() >= elem.getColNum()) {
                            continue;
                        }
                        Element fromElem = columnMaps[0].get(elem.getColNum() - 1).get(operand);
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
            if (a.getOperation().equals("<-") && !b.getOperation().equals("<-")) {
                return 1;
            }
            if (b.getOperation().equals("<-") && !a.getOperation().equals("<-")) {
                return -1;
            }

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

    // sets xCoords and yCoords to correct values within/across each column
    // returns the required dimensions of the final image
    public static int[] setCoords(ArrayList<ArrayList<Element>> columns){
        // set yCoords
        int maxHeight = 0;
        for (ArrayList<Element> column : columns) {
            int currHeight = Visualizer.VERT_DIST;
            for (Element elem : column) {
                elem.setYCoord(currHeight);
                currHeight += elem.getHeight() + Visualizer.VERT_DIST;
            }
            maxHeight = Math.max(maxHeight, currHeight);
        }
        int yDim = maxHeight + Visualizer.VERT_DIST;

        // set xCoords
        int prevX = 0;
        int horizDist = Visualizer.HORIZ_DIST;
        setColX(columns.get(0), prevX + horizDist);
        prevX += horizDist + Visualizer.FULL_WIDTH;
        for (int i = 1; i < columns.size(); i++) {
            // scale horizontal distance according to the ratio between column heights
            int prevHeight = getColHeight(columns.get(i - 1));
            int currHeight = getColHeight(columns.get(i));
            double colRatio = ((double) currHeight) / prevHeight;
            if (colRatio < 1) {
                colRatio = 1 / colRatio;
            }

            // scale according to the height of the highest column
            double heightRatio = ((double) Math.max(prevHeight, currHeight)) / yDim;
            colRatio = 1 + (colRatio - 1) * heightRatio;
            horizDist = (int) (Visualizer.HORIZ_DIST * colRatio);

            setColX(columns.get(i), prevX + horizDist);
            prevX += horizDist + Visualizer.FULL_WIDTH;
        }
        int xDim = prevX + Visualizer.HORIZ_DIST;
        return new int[] {xDim, yDim};
    }

    private static int getColHeight(ArrayList<Element> column) {
        return column.get(column.size()-1).getYBase();
    }

    private static void setColX(ArrayList<Element> column, int xCoord) {
        for (Element elem : column) {
            elem.setXCoord(xCoord);
        }
    }

    public static String condenseName(String name, int length){
        if(name.length() <= length)
            return name;
        return name.substring(0, length / 2 - 2) + ".." + name.substring(name.length() - length / 2);
    }
}