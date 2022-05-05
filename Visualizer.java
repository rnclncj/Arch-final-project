import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

// TODO: add NOT gate

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

    public static final int NUM_REORDERS = 100;

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
        if (args.length != 1) {
            System.out.println("usage: java Visualizer filename.vf");
            System.exit(1);
        }
        BufferedReader reader = new BufferedReader(new FileReader(args[0]));
        ArrayList<Element> elementList = new ArrayList<>();
        HashMap<String, Element> elementMap = new HashMap<>();

        String line = "";
        Element element;
        while ((line = reader.readLine()) != null) {
            //Module Check
            StringTokenizer tokenizer = new StringTokenizer(line);
            String type = tokenizer.nextToken();
            if(type.equals("module")){
                Color color = new Color((int)(Math.random()*255),(int)(Math.random()*255),(int)(Math.random()*255));
                String moduleName = tokenizer.nextToken();
                
                //module outputs
                while(tokenizer.hasMoreTokens()){
                    String output = tokenizer.nextToken();
                    if(output.equals("#"))
                        break;
                    int outWidth = Element.getWidthFromName(output);
                    String outName = Element.getNameFromName(output);
                    element = new Element(moduleName, outName, outWidth, color);
                    placeElementInMap(element, elementList, elementMap);
                }

                //module inputs
                while(tokenizer.hasMoreTokens()){
                    String output = tokenizer.nextToken();
                    String outName = Element.getNameFromName(output);
                    element = new Element(moduleName, outName, color);
                    placeElementInMap(element, elementList, elementMap);
                }

            }
            else{ //If not a module
                element = new Element(line);
                placeElementInMap(element, elementList, elementMap);
            }
            
            
            
        }
        reader.close();

        ArrayList<ArrayList<Element>> columns = placeElements(elementList, elementMap);
        addPaths(columns);
        ArrayList<HashMap<String, Element>>[] columnMaps = getColumnMaps(columns);
        for (int i = 0; i < columns.size(); i++) {
            reorderBackwards(columns, elementMap);
            reorderForwards(columns, elementMap, columnMaps);
        }
        int[] dims = setCoords(columns);

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                Visualizer vis = new Visualizer(columns, elementMap, columnMaps, dims);
                vis.setVisible(true);
            }
        });
    }

    public static void placeElementInMap(Element element, ArrayList<Element> elementList, HashMap<String, Element> elementMap){
        if (element.getName().charAt(0) != '.' && element.getOperation().equals("=")
                    && element.getOperands().get(0).charAt(0) == '.'
                    && element.getOperands().get(0).equals(elementList.get(elementList.size()-1).getName())
                    && !elementList.get(elementList.size()-1).getOperation().equals("--")) {
                Element prevElem = elementMap.remove(element.getOperands().get(0));
                prevElem.setName(element.getName());
                prevElem.setType(element.getType());
                elementMap.put(prevElem.getName(), prevElem);
                return;
            }
        elementList.add(element);
        elementMap.put(element.getName(), element);
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

    public static void reorderBackwards(ArrayList<ArrayList<Element>> columns, HashMap<String, Element> elementMap) {
        for (int i = 0; i < columns.size() - 1; i++) {
            ScoreComp scoreComp = new ScoreComp();
            ArrayList<Element> column = columns.get(i);
            for (int j = 0; j < column.size(); j++) {
                scoreComp.addScore(column.get(j).getName(), j, 0.01);
            }

            ArrayList<Element> nextColumn = columns.get(i + 1);
            for (int j = 0; j < nextColumn.size(); j++) {
                Element elem = nextColumn.get(j);
                if (!(elem.getOperation().equals("--") || elem.getOperation().equals("<-"))) {
                    for (String operand : elem.getOperands()) {
                        if (elementMap.get(operand).getColNum() >= elem.getColNum()) {
                            continue;
                        }
                        scoreComp.addScore(operand, j, 1);
                    }
                }
            }
            column.sort(scoreComp);
        }
    }

    public static void reorderForwards(ArrayList<ArrayList<Element>> columns, HashMap<String, Element> elementMap, ArrayList<HashMap<String, Element>>[] columnMaps) {
        HashMap<String, Integer> indexMap = getIndexMap(columns.get(0));
        for (int i = 1; i < columns.size(); i++) {
            ScoreComp scoreComp = new ScoreComp();
            ArrayList<Element> column = columns.get(i);
            for (int j = 0; j < column.size(); j++) {
                Element elem = column.get(j);
                scoreComp.addScore(elem.getName(), j, 0.01);
                if (!(elem.getOperation().equals("--") || elem.getOperation().equals("<-"))) {
                    for (String operand : elem.getOperands()) {
                        if (elementMap.get(operand).getColNum() >= elem.getColNum()) {
                            continue;
                        }
                        Element fromElem = columnMaps[0].get(elem.getColNum() - 1).get(operand);
                        scoreComp.addScore(elem.getName(), indexMap.get(fromElem.getName()), 1);
                    }
                }
            }
            column.sort(scoreComp);
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
        private HashMap<String, Double> sumMap;
        private HashMap<String, Double> numMap;

        public ScoreComp() {
            sumMap = new HashMap<>();
            numMap = new HashMap<>();
        }

        public void addScore(String name, int score, double weight) {
            if (sumMap.containsKey(name)) {
                sumMap.put(name, sumMap.get(name) + score * weight);
                numMap.put(name, numMap.get(name) + weight);
            } else {
                sumMap.put(name, score * weight);
                numMap.put(name, weight);
            }
        }

        public int compare(Element a, Element b) {
            if (a.getOperation().equals("<-") && !b.getOperation().equals("<-")) {
                return 1;
            }
            if (b.getOperation().equals("<-") && !a.getOperation().equals("<-")) {
                return -1;
            }

            double aScore = sumMap.get(a.getName()) / numMap.get(a.getName());
            double bScore = sumMap.get(b.getName()) / numMap.get(b.getName());
            double diff = aScore - bScore;
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
            heightRatio = Math.pow(2, 6 * (heightRatio - 0.5));
            
            // average the two ratios
            double ratio = (colRatio + heightRatio) / 2;
            ratio = Math.max(ratio, 1);
            horizDist = (int) (Visualizer.HORIZ_DIST * ratio);
            horizDist = Math.min(horizDist, Visualizer.MAX_WIDTH / 2);

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